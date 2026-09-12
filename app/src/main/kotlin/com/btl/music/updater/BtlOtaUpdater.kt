/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package com.btl.music.updater

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import moe.rukamori.archivetune.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BtlOtaUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    sealed interface OtaState {
        data object Idle : OtaState
        data object Checking : OtaState
        data class Available(
            val tagName: String,
            val releaseName: String,
            val releaseNotes: String,
            val downloadUrl: String,
            val fileSizeMb: Double,
        ) : OtaState
        data object UpToDate : OtaState
        data class Downloading(val progressPercent: Int) : OtaState
        data class ReadyToInstall(val apkFile: File) : OtaState
        data class Error(val message: String) : OtaState
    }

    private val _otaState = MutableStateFlow<OtaState>(OtaState.Idle)
    val otaState: StateFlow<OtaState> = _otaState.asStateFlow()

    suspend fun checkForUpdates(force: Boolean = false): OtaState = withContext(Dispatchers.IO) {
        _otaState.value = OtaState.Checking
        try {
            val request = Request.Builder()
                .url(GITHUB_RELEASES_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "BTL-Music-Android/${BuildConfig.VERSION_NAME}")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val error = "GitHub release check returned HTTP ${response.code}"
                _otaState.value = OtaState.Error(error)
                return@withContext _otaState.value
            }

            val body = response.body.string()
            val json = JSONObject(body)
            val latestTag = json.optString("tag_name", "").trim()
            val releaseName = json.optString("name", latestTag)
            val bodyText = json.optString("body", "")

            val currentVersion = BuildConfig.VERSION_NAME.removePrefix("v").trim()
            val remoteVersion = latestTag.removePrefix("v").trim()

            val isNewer = isRemoteVersionNewer(currentVersion, remoteVersion)
            if (isNewer || force) {
                // Look for arm64-v8a or universal apk asset
                val assets = json.optJSONArray("assets")
                var downloadUrl = ""
                var fileSizeBytes = 0L

                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "").lowercase()
                        if (name.endsWith(".apk") && (name.contains("arm64") || name.contains("universal") || name.contains("release"))) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            fileSizeBytes = asset.optLong("size", 0L)
                            break
                        }
                    }
                    if (downloadUrl.isBlank() && assets.length() > 0) {
                        val first = assets.getJSONObject(0)
                        downloadUrl = first.optString("browser_download_url", "")
                        fileSizeBytes = first.optLong("size", 0L)
                    }
                }

                if (downloadUrl.isNotBlank()) {
                    val state = OtaState.Available(
                        tagName = latestTag,
                        releaseName = releaseName,
                        releaseNotes = bodyText,
                        downloadUrl = downloadUrl,
                        fileSizeMb = fileSizeBytes / (1024.0 * 1024.0),
                    )
                    _otaState.value = state
                    return@withContext state
                }
            }

            _otaState.value = OtaState.UpToDate
            OtaState.UpToDate
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "OTA update check failed")
            val state = OtaState.Error(e.message ?: "Failed to connect to update server")
            _otaState.value = state
            state
        }
    }

    fun startDownload(downloadUrl: String, fileName: String = "btl_music_update.apk") {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("Downloading BTL Music update")
                .setDescription("Preparing new release installation")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)

            downloadManager.enqueue(request)
            _otaState.value = OtaState.Downloading(0)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to enqueue OTA download")
            _otaState.value = OtaState.Error("Failed to initiate download: ${e.message}")
        }
    }

    fun triggerApkInstall(apkFile: File) {
        if (!apkFile.exists()) {
            _otaState.value = OtaState.Error("APK file not found on disk")
            return
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile,
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        context.startActivity(intent)
    }

    private fun isRemoteVersionNewer(current: String, remote: String): Boolean {
        val currentParts = current.split(".").mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }
        val remoteParts = remote.split(".").mapNotNull { it.takeWhile(Char::isDigit).toIntOrNull() }

        val maxLength = maxOf(currentParts.size, remoteParts.size)
        for (i in 0 until maxLength) {
            val curr = currentParts.getOrElse(i) { 0 }
            val rem = remoteParts.getOrElse(i) { 0 }
            if (rem > curr) return true
            if (rem < curr) return false
        }
        return false
    }

    companion object {
        private const val TAG = "BtlOtaUpdater"
        const val GITHUB_REPO = "balajitechlabs/ArchiveTune"
        const val GITHUB_RELEASES_URL = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    }
}
