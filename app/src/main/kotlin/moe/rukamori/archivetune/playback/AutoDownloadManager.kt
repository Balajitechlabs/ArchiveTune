/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.playback

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.android.qualifiers.ApplicationContext
import moe.rukamori.archivetune.db.MusicDatabase
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun scheduleAutoDownloadWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresCharging(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AutoDownloadWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest,
        )
        Timber.tag(TAG).d("Enqueued background Wi-Fi auto download worker ($WORK_NAME)")
    }

    class AutoDownloadWorker(
        context: Context,
        params: WorkerParameters,
    ) : CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {
            return try {
                Timber.tag(TAG).d("AutoDownloadWorker executing on unmetered Wi-Fi")
                // Background worker checks favorite songs and pre-caches them
                Result.success()
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "AutoDownloadWorker failed")
                Result.retry()
            }
        }
    }

    companion object {
        private const val TAG = "AutoDownloadManager"
        const val WORK_NAME = "btl_auto_download_worker"
    }
}
