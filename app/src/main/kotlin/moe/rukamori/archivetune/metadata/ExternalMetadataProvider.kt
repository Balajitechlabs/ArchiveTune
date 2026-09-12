/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.metadata

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExternalMetadataProvider @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    data class EnrichedMetadata(
        val highResArtworkUrl: String?,
        val bpm: Int?,
        val releaseDate: String?,
        val recordLabel: String?,
    )

    suspend fun enrichFromDeezer(title: String, artist: String): EnrichedMetadata? = withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode("$title $artist", "UTF-8")
            val url = "https://api.deezer.com/search?q=$query&limit=1"
            val request = Request.Builder().url(url).build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val json = JSONObject(response.body.string())
            val data = json.optJSONArray("data") ?: return@withContext null
            if (data.length() == 0) return@withContext null

            val item = data.getJSONObject(0)
            val album = item.optJSONObject("album")
            val artwork = album?.optString("cover_xl") ?: album?.optString("cover_big")

            EnrichedMetadata(
                highResArtworkUrl = artwork,
                bpm = item.optInt("bpm").takeIf { it > 0 },
                releaseDate = item.optString("release_date").takeIf { it.isNotBlank() },
                recordLabel = null,
            )
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to enrich metadata from Deezer")
            null
        }
    }

    companion object {
        private const val TAG = "ExternalMetadata"
    }
}
