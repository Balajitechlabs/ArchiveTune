/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRemoteServer @Inject constructor() {

    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start(port: Int = 8080, onCommand: (String) -> Unit) {
        if (isRunning) return
        isRunning = true
        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                Timber.tag(TAG).i("WebRemoteServer listening on port $port")

                while (isRunning) {
                    val client = serverSocket?.accept() ?: break
                    handleClient(client, onCommand)
                }
            } catch (e: Exception) {
                if (isRunning) {
                    Timber.tag(TAG).e(e, "WebRemoteServer error")
                }
            }
        }
    }

    private fun handleClient(socket: Socket, onCommand: (String) -> Unit) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                val line = reader.readLine() ?: return@launch
                val path = line.split(" ").getOrNull(1) ?: "/"

                val command = path.removePrefix("/api/").lowercase()
                if (command.isNotBlank() && command != "/") {
                    onCommand(command)
                }

                val html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <title>BTL Music Remote</title>
                        <meta name="viewport" content="width=device-width, initial-scale=1">
                        <style>
                            body { background: #121212; color: #eee; font-family: sans-serif; text-align: center; padding: 40px 20px; }
                            h1 { color: #fff; }
                            .btn { background: #222; border: 1px solid #444; color: #fff; padding: 16px 32px; margin: 10px; border-radius: 24px; font-size: 18px; cursor: pointer; }
                            .btn:active { background: #444; }
                        </style>
                    </head>
                    <body>
                        <h1>||BTL||™ Music Remote</h1>
                        <p>Local Wi-Fi Playback Controller</p>
                        <button class="btn" onclick="fetch('/api/prev')">⏮ Prev</button>
                        <button class="btn" onclick="fetch('/api/playpause')">⏯ Play / Pause</button>
                        <button class="btn" onclick="fetch('/api/next')">⏭ Next</button>
                    </body>
                    </html>
                """.trimIndent()

                writer.println("HTTP/1.1 200 OK")
                writer.println("Content-Type: text/html; charset=UTF-8")
                writer.println("Content-Length: ${html.toByteArray(Charsets.UTF_8).size}")
                writer.println("Connection: close")
                writer.println()
                writer.println(html)
                writer.flush()
                socket.close()
            } catch (e: Exception) {
                try { socket.close() } catch (_: Exception) {}
            }
        }
    }

    fun stop() {
        isRunning = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }

    companion object {
        private const val TAG = "WebRemoteServer"
    }
}
