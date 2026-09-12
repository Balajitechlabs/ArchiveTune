/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import moe.rukamori.archivetune.playback.equalizer.BtlAudioVisualizerHub
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BTL LAN Hi-Fi Web Remote and Live Audio Streamer.
 *
 * Exposes:
 * 1. GET / - Responsive web controller with live track info and web audio player.
 * 2. GET /stream.wav - Zero-latency 16-bit 44.1kHz stereo PCM WAV stream.
 * 3. GET /api/status - JSON with current playback state, title, and artist.
 * 4. GET /api/play, /pause, /playpause, /next, /prev - Control endpoints.
 */
@Singleton
class WebRemoteServer @Inject constructor() {

    private var serverSocket: ServerSocket? = null
    @Volatile
    private var isRunning = false
    private var scope = CoroutineScope(Dispatchers.IO)

    private val streamingClients = Collections.synchronizedSet(mutableSetOf<OutputStream>())

    fun start(
        port: Int = 8080,
        lanModeEnabled: Boolean = true,
        getCurrentSong: () -> Pair<String?, String?>? = { null },
        isPlaying: () -> Boolean = { false },
        onCommand: (String) -> Unit
    ) {
        if (isRunning) return
        isRunning = true
        scope = CoroutineScope(Dispatchers.IO)

        // Register visualizer hub listener to broadcast PCM frames to active stream sockets
        BtlAudioVisualizerHub.pcmStreamListener = { buffer, offset, length ->
            if (streamingClients.isNotEmpty()) {
                synchronized(streamingClients) {
                    val iterator = streamingClients.iterator()
                    while (iterator.hasNext()) {
                        val out = iterator.next()
                        try {
                            out.write(buffer, offset, length)
                            out.flush()
                        } catch (_: Exception) {
                            iterator.remove()
                        }
                    }
                }
            }
        }

        scope.launch {
            try {
                val bindAddress = if (lanModeEnabled) null else InetAddress.getByName("127.0.0.1")
                serverSocket = ServerSocket(port, 50, bindAddress)
                val boundTo = if (lanModeEnabled) "0.0.0.0:$port (LAN)" else "127.0.0.1:$port (localhost)"
                Timber.tag(TAG).i("WebRemoteServer started on $boundTo")

                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client, getCurrentSong, isPlaying, onCommand)
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Timber.tag(TAG).e(e, "WebRemoteServer error")
                }
            }
        }
    }

    private fun handleClient(
        socket: Socket,
        getCurrentSong: () -> Pair<String?, String?>?,
        isPlaying: () -> Boolean,
        onCommand: (String) -> Unit
    ) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val line = reader.readLine() ?: return@launch
                val path = line.split(" ").getOrNull(1) ?: "/"

                if (path.startsWith("/stream")) {
                    handleAudioStream(socket)
                    return@launch
                }

                val outStream = socket.getOutputStream()
                val writer = PrintWriter(outStream, true)

                if (path.startsWith("/api/status")) {
                    val (title, artist) = getCurrentSong() ?: (null to null)
                    val json = """{"isPlaying":${isPlaying()},"title":"${escapeJson(title ?: "No Song Playing")}","artist":"${escapeJson(artist ?: "BTL Music")}"}"""
                    writer.println("HTTP/1.1 200 OK")
                    writer.println("Content-Type: application/json; charset=UTF-8")
                    writer.println("Access-Control-Allow-Origin: *")
                    writer.println("Content-Length: ${json.toByteArray(Charsets.UTF_8).size}")
                    writer.println("Connection: close")
                    writer.println()
                    writer.println(json)
                    writer.flush()
                    socket.close()
                    return@launch
                }

                if (path.startsWith("/api/")) {
                    val command = path.removePrefix("/api/").substringBefore("?").lowercase()
                    if (command.isNotBlank()) {
                        onCommand(command)
                    }
                    val json = """{"status":"ok","command":"$command"}"""
                    writer.println("HTTP/1.1 200 OK")
                    writer.println("Content-Type: application/json; charset=UTF-8")
                    writer.println("Access-Control-Allow-Origin: *")
                    writer.println("Content-Length: ${json.toByteArray(Charsets.UTF_8).size}")
                    writer.println("Connection: close")
                    writer.println()
                    writer.println(json)
                    writer.flush()
                    socket.close()
                    return@launch
                }

                // Serve responsive modern web player
                val html = buildWebPage()
                writer.println("HTTP/1.1 200 OK")
                writer.println("Content-Type: text/html; charset=UTF-8")
                writer.println("Content-Length: ${html.toByteArray(Charsets.UTF_8).size}")
                writer.println("Connection: close")
                writer.println()
                writer.println(html)
                writer.flush()
                socket.close()
            } catch (_: Exception) {
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    private fun handleAudioStream(socket: Socket) {
        try {
            val out = socket.getOutputStream()
            val header = buildWavHeader(sampleRate = 44100, channels = 2, bitsPerSample = 16)

            val httpHeaders = (
                "HTTP/1.1 200 OK\r\n" +
                "Content-Type: audio/wav\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Cache-Control: no-cache, no-store\r\n" +
                "Connection: close\r\n\r\n"
            ).toByteArray(Charsets.US_ASCII)

            out.write(httpHeaders)
            out.write(header)
            out.flush()

            streamingClients.add(out)
        } catch (_: Exception) {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun buildWavHeader(sampleRate: Int, channels: Int, bitsPerSample: Int): ByteArray {
        val buffer = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()

        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(0x7FFFFFF0)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign)
        buffer.putShort(bitsPerSample.toShort())
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(0x7FFFFFF0)

        return buffer.array()
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ")
            .replace("\r", "")
    }

    private fun buildWebPage(): String {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>BTL Music — Hi-Fi Web Player & Controller</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    :root {
                        --bg: #0B0E14;
                        --card: #151921;
                        --card-border: rgba(255, 255, 255, 0.08);
                        --primary: #4ADE80;
                        --primary-glow: rgba(74, 222, 128, 0.25);
                        --text: #F1F5F9;
                        --text-muted: #94A3B8;
                    }
                    * { box-sizing: border-box; margin: 0; padding: 0; }
                    body {
                        background: var(--bg);
                        color: var(--text);
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        justify-content: center;
                        min-height: 100vh;
                        padding: 24px 16px;
                    }
                    .player-card {
                        background: var(--card);
                        border: 1px solid var(--card-border);
                        border-radius: 28px;
                        padding: 32px 24px;
                        width: 100%;
                        max-width: 440px;
                        box-shadow: 0 20px 40px rgba(0,0,0,0.6);
                        text-align: center;
                    }
                    .badge {
                        display: inline-flex;
                        align-items: center;
                        gap: 6px;
                        padding: 6px 14px;
                        background: rgba(74, 222, 128, 0.12);
                        border: 1px solid rgba(74, 222, 128, 0.3);
                        border-radius: 999px;
                        font-size: 12px;
                        font-weight: 600;
                        color: var(--primary);
                        margin-bottom: 24px;
                    }
                    .dot { width: 8px; height: 8px; border-radius: 50%; background: var(--primary); animation: pulse 2s infinite; }
                    @keyframes pulse { 0%, 100% { opacity: 1; transform: scale(1); } 50% { opacity: 0.4; transform: scale(0.8); } }
                    .song-title { font-size: 22px; font-weight: 700; margin-bottom: 8px; line-height: 1.3; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
                    .artist-name { font-size: 15px; color: var(--text-muted); margin-bottom: 28px; }
                    .controls { display: flex; align-items: center; justify-content: center; gap: 16px; margin-bottom: 28px; }
                    .btn {
                        background: #1E2430;
                        border: 1px solid var(--card-border);
                        color: #FFF;
                        border-radius: 50%;
                        width: 52px;
                        height: 52px;
                        font-size: 20px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        cursor: pointer;
                        transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
                    }
                    .btn:hover { background: #2A3242; transform: translateY(-2px); }
                    .btn:active { transform: scale(0.95); }
                    .btn-play {
                        background: var(--primary);
                        color: #0B0E14;
                        width: 64px;
                        height: 64px;
                        font-size: 26px;
                        font-weight: bold;
                        box-shadow: 0 4px 20px var(--primary-glow);
                    }
                    .btn-play:hover { background: #5eead4; }
                    .stream-box {
                        background: rgba(0,0,0,0.3);
                        border: 1px solid var(--card-border);
                        border-radius: 18px;
                        padding: 16px;
                    }
                    .stream-box h3 { font-size: 14px; font-weight: 600; margin-bottom: 6px; color: var(--primary); }
                    .stream-box p { font-size: 12px; color: var(--text-muted); margin-bottom: 12px; }
                    audio { width: 100%; height: 38px; border-radius: 8px; outline: none; }
                    footer { margin-top: 24px; font-size: 12px; color: #64748B; }
                </style>
            </head>
            <body>
                <div class="player-card">
                    <div class="badge"><div class="dot"></div> LAN Hi-Fi Streamer Active</div>
                    <div class="song-title" id="title">Connecting...</div>
                    <div class="artist-name" id="artist">BTL Music</div>
                    <div class="controls">
                        <button class="btn" onclick="sendCommand('prev')">⏮</button>
                        <button class="btn btn-play" id="playBtn" onclick="sendCommand('playpause')">⏯</button>
                        <button class="btn" onclick="sendCommand('next')">⏭</button>
                    </div>
                    <div class="stream-box">
                        <h3>Direct Audio Stream</h3>
                        <p>Listen directly on this device with zero compression</p>
                        <audio id="audio" controls preload="none">
                            <source src="/stream.wav" type="audio/wav">
                        </audio>
                    </div>
                </div>
                <footer>||BTL||™ (balajitechlabs) — BTL Music Core</footer>
                <script>
                    async function fetchStatus() {
                        try {
                            const res = await fetch('/api/status');
                            if (res.ok) {
                                const data = await res.json();
                                document.getElementById('title').textContent = data.title || 'No Song Playing';
                                document.getElementById('artist').textContent = data.artist || 'BTL Music';
                                document.getElementById('playBtn').textContent = data.isPlaying ? '⏸' : '▶';
                            }
                        } catch (_) {}
                    }
                    async function sendCommand(cmd) {
                        try {
                            await fetch('/api/' + cmd);
                            setTimeout(fetchStatus, 200);
                        } catch (_) {}
                    }
                    setInterval(fetchStatus, 2500);
                    fetchStatus();
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    fun stop() {
        isRunning = false
        BtlAudioVisualizerHub.pcmStreamListener = null
        synchronized(streamingClients) {
            streamingClients.forEach { out ->
                try { out.close() } catch (_: Exception) {}
            }
            streamingClients.clear()
        }
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        scope.cancel()
    }

    companion object {
        private const val TAG = "WebRemoteServer"
    }
}
