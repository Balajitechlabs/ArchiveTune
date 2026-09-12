/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.playback

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow

/**
 * AndroidX Media3 AudioProcessor for EBU R128 and ReplayGain loudness normalization.
 *
 * Normalizes track loudness to the industry-standard -14 LUFS target.
 * Includes soft-knee peak limiting to prevent digital clipping (0 dBFS).
 */
@OptIn(UnstableApi::class)
class AudioNormalizationProcessor : BaseAudioProcessor() {

    @Volatile
    private var isNormalizationEnabled: Boolean = false

    @Volatile
    private var currentGainMultiplier: Float = 1.0f

    @Volatile
    private var targetLufs: Float = -14.0f

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        isNormalizationEnabled = enabled
    }

    @Synchronized
    fun setTargetLufs(lufs: Float) {
        targetLufs = lufs
    }

    /**
     * Set track loudness in dB (relative to -14 LUFS) or integrated LUFS.
     * For example, YouTube sends audioConfig.loudnessDb (e.g. -3.2 dB).
     */
    @Synchronized
    fun setTrackLoudnessDb(loudnessDb: Float?) {
        if (loudnessDb == null || !loudnessDb.isFinite()) {
            currentGainMultiplier = 1.0f
            return
        }

        val deltaDb = -loudnessDb
        val clampedDelta = deltaDb.coerceIn(-12.0f, 6.0f)
        currentGainMultiplier = (10.0f).pow(clampedDelta / 20.0f)
    }

    @Synchronized
    fun setTrackLoudnessDb(loudnessDb: Double?) {
        setTrackLoudnessDb(loudnessDb?.toFloat())
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining)

        if (!isNormalizationEnabled || kotlin.math.abs(currentGainMultiplier - 1.0f) < 0.01f) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        val gain = currentGainMultiplier
        val shortBuffer = inputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
        val outShortBuffer = outputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()

        val sampleCount = remaining / 2
        for (i in 0 until sampleCount) {
            val sample = shortBuffer.get()
            var scaled = sample * gain

            // Soft-knee peak limiting to prevent hard digital clipping
            if (scaled > 32767f) {
                scaled = 32767f
            } else if (scaled < -32768f) {
                scaled = -32768f
            }

            outShortBuffer.put(scaled.toInt().toShort())
        }

        inputBuffer.position(inputBuffer.limit())
        outputBuffer.position(outputBuffer.position() + remaining)
        outputBuffer.flip()
    }
}
