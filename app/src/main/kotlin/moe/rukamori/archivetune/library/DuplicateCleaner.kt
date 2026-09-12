/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.library

import moe.rukamori.archivetune.db.entities.Song
import java.util.Locale

object DuplicateCleaner {

    data class DuplicateGroup(
        val normalizedKey: String,
        val primarySong: Song,
        val duplicateSongs: List<Song>,
    )

    fun findDuplicates(songs: List<Song>): List<DuplicateGroup> {
        val groups = songs.groupBy { normalizeSongKey(it) }
        val duplicateGroups = mutableListOf<DuplicateGroup>()

        for ((key, groupSongs) in groups) {
            if (groupSongs.size > 1) {
                // Keep the one with highest playCount or favorite status as primary
                val sorted = groupSongs.sortedWith(
                    compareByDescending<Song> { it.song.liked }
                        .thenByDescending { it.song.totalPlayTime }
                )
                duplicateGroups.add(
                    DuplicateGroup(
                        normalizedKey = key,
                        primarySong = sorted.first(),
                        duplicateSongs = sorted.drop(1),
                    )
                )
            }
        }
        return duplicateGroups
    }

    private fun normalizeSongKey(song: Song): String {
        val title = song.title.lowercase(Locale.ROOT)
            .replace(Regex("\\(.*?\\)|\\[.*?\\]"), "")
            .replace(Regex("[^a-z0-9]"), "")
            .trim()
        val artist = song.artists.firstOrNull()?.name?.lowercase(Locale.ROOT)
            ?.replace(Regex("[^a-z0-9]"), "")
            ?.trim()
            .orEmpty()
        return "$artist::$title"
    }
}
