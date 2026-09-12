/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.utils

import moe.rukamori.archivetune.db.entities.Song
import java.io.InputStream
import java.io.OutputStream

object PlaylistMigrationUtil {

    fun exportToM3u8(playlistName: String, songs: List<Song>, outputStream: OutputStream) {
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write("#EXTM3U\n")
            writer.write("#PLAYLIST:$playlistName\n\n")
            for (song in songs) {
                val artistStr = song.artists.joinToString(", ") { it.name }
                writer.write("#EXTINF:${song.song.duration},${artistStr} - ${song.title}\n")
                writer.write("https://music.youtube.com/watch?v=${song.id}\n\n")
            }
            writer.flush()
        }
    }

    data class ImportedPlaylistEntry(
        val videoId: String?,
        val title: String,
        val artist: String,
        val durationSeconds: Int,
    )

    fun importFromM3u8(inputStream: InputStream): List<ImportedPlaylistEntry> {
        val results = mutableListOf<ImportedPlaylistEntry>()
        val lines = inputStream.bufferedReader(Charsets.UTF_8).readLines()

        var currentTitle = ""
        var currentArtist = ""
        var currentDuration = 0

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                val info = trimmed.removePrefix("#EXTINF:")
                val commaIndex = info.indexOf(',')
                if (commaIndex != -1) {
                    currentDuration = info.substring(0, commaIndex).trim().toIntOrNull() ?: 0
                    val titleArtist = info.substring(commaIndex + 1).trim()
                    if (titleArtist.contains(" - ")) {
                        currentArtist = titleArtist.substringBefore(" - ").trim()
                        currentTitle = titleArtist.substringAfter(" - ").trim()
                    } else {
                        currentTitle = titleArtist
                    }
                }
            } else if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                val videoId = if (trimmed.contains("watch?v=")) {
                    trimmed.substringAfter("watch?v=").substringBefore('&')
                } else if (trimmed.contains("youtu.be/")) {
                    trimmed.substringAfter("youtu.be/").substringBefore('?')
                } else {
                    null
                }
                results.add(
                    ImportedPlaylistEntry(
                        videoId = videoId,
                        title = currentTitle,
                        artist = currentArtist,
                        durationSeconds = currentDuration,
                    )
                )
                currentTitle = ""
                currentArtist = ""
                currentDuration = 0
            }
        }
        return results
    }
}
