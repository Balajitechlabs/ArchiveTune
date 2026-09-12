/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package moe.rukamori.archivetune.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

class SleepTimer(
    private val scope: CoroutineScope,
    val player: Player,
    private val service: MusicService,
) : Player.Listener {
    private var sleepTimerJob: Job? = null
    private var originalVolume: Float = 1.0f

    var triggerTime by mutableStateOf(-1L)
        private set
    var pauseWhenSongEnd by mutableStateOf(false)
        private set
    val isActive: Boolean
        get() = triggerTime != -1L || pauseWhenSongEnd

    fun start(minute: Int) {
        clear()
        if (minute == -1) {
            pauseWhenSongEnd = true
        } else {
            val totalDurationMs = minute.minutes.inWholeMilliseconds
            triggerTime = System.currentTimeMillis() + totalDurationMs
            originalVolume = player.volume

            sleepTimerJob =
                scope.launch {
                    // Sunset Decelerator: Apply smooth fade-out curve over the last 60 seconds (or 1/3 of timer if short)
                    val fadeDurationMs = 60_000L.coerceAtMost(totalDurationMs / 3).coerceAtLeast(0L)
                    val normalDurationMs = totalDurationMs - fadeDurationMs

                    if (normalDurationMs > 0) {
                        delay(normalDurationMs)
                    }

                    if (fadeDurationMs > 0) {
                        val steps = 30
                        val stepDelay = fadeDurationMs / steps
                        for (i in 1..steps) {
                            val progress = i.toFloat() / steps.toFloat()
                            val factor = kotlin.math.cos(progress * Math.PI.toFloat() * 0.5f)
                            player.volume = (originalVolume * factor).coerceAtLeast(0f)
                            delay(stepDelay)
                        }
                    }

                    service.pauseFromSleepTimer()
                    player.volume = originalVolume
                }
        }
    }

    fun clear() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        pauseWhenSongEnd = false
        triggerTime = -1L
        if (player.volume != originalVolume && originalVolume > 0f) {
            player.volume = originalVolume
        }
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        if (pauseWhenSongEnd) {
            service.pauseFromSleepTimer()
        }
    }

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        if (playbackState == Player.STATE_ENDED && pauseWhenSongEnd) {
            service.pauseFromSleepTimer()
        }
    }
}
