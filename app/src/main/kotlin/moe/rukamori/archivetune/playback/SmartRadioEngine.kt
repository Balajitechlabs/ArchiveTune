/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.playback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.innertube.YouTube
import moe.rukamori.archivetune.innertube.models.WatchEndpoint
import moe.rukamori.archivetune.models.MediaMetadata
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartRadioEngine @Inject constructor() {

    suspend fun fetchContinuationTracks(
        currentVideoId: String,
        currentPlaylistId: String? = null,
    ): List<MediaMetadata> = withContext(Dispatchers.IO) {
        try {
            val response = YouTube.next(
                endpoint = WatchEndpoint(
                    videoId = currentVideoId,
                    playlistId = currentPlaylistId,
                ),
            ).getOrNull()

            val continuation = response?.items?.mapNotNull { track ->
                track.id.takeIf { it.isNotBlank() }?.let { id ->
                    MediaMetadata(
                        id = id,
                        title = track.title,
                        artists = track.artists.map { MediaMetadata.Artist(id = it.id, name = it.name) },
                        duration = track.duration ?: 0,
                        thumbnailUrl = track.thumbnail,
                    )
                }
            } ?: emptyList()

            continuation
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to fetch radio continuation for %s", currentVideoId)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "SmartRadioEngine"
    }
}
