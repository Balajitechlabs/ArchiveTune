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
import kotlin.math.cos

/**
 * AndroidX Media3 AudioProcessor providing smooth logarithmic crossfade, fade-in, and fade-out ramps.
 */
@OptIn(UnstableApi::class)
class CrossfadeAudioProcessor : BaseAudioProcessor() {

    enum class FadeState {
        IDLE,
        FADING_IN,
        FADING_OUT,
    }

    @Volatile
    private var fadeState = FadeState.IDLE

    @Volatile
    private var fadeDurationSamples: Long = 0L

    @Volatile
    private var currentFadeSample: Long = 0L

    @Volatile
    private var sampleRate: Int = 44100

    @Synchronized
    fun startFadeIn(durationMs: Long) {
        if (durationMs <= 0) {
            fadeState = FadeState.IDLE
            return
        }
        fadeDurationSamples = (durationMs * sampleRate) / 1000L
        currentFadeSample = 0L
        fadeState = FadeState.FADING_IN
    }

    @Synchronized
    fun startFadeOut(durationMs: Long) {
        if (durationMs <= 0) {
            fadeState = FadeState.IDLE
            return
        }
        fadeDurationSamples = (durationMs * sampleRate) / 1000L
        currentFadeSample = 0L
        fadeState = FadeState.FADING_OUT
    }

    @Synchronized
    fun resetFade() {
        fadeState = FadeState.IDLE
        currentFadeSample = 0L
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        sampleRate = inputAudioFormat.sampleRate
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining)

        if (fadeState == FadeState.IDLE) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        val shortBuffer = inputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
        val outShortBuffer = outputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
        val sampleCount = remaining / 2

        for (i in 0 until sampleCount) {
            val sample = shortBuffer.get()
            val gain = calculateGain()
            val scaled = (sample * gain).toInt().coerceIn(-32768, 32767).toShort()
            outShortBuffer.put(scaled)
            stepSample()
        }

        inputBuffer.position(inputBuffer.limit())
        outputBuffer.position(outputBuffer.position() + remaining)
        outputBuffer.flip()
    }

    private fun calculateGain(): Float {
        if (fadeDurationSamples <= 0) return 1.0f
        val progress = (currentFadeSample.toFloat() / fadeDurationSamples.toFloat()).coerceIn(0.0f, 1.0f)
        return when (fadeState) {
            FadeState.FADING_IN -> {
                // Equal-power fade in: sin(progress * PI / 2)
                kotlin.math.sin(progress * Math.PI.toFloat() * 0.5f)
            }
            FadeState.FADING_OUT -> {
                // Equal-power fade out: cos(progress * PI / 2)
                cos(progress * Math.PI.toFloat() * 0.5f)
            }
            FadeState.IDLE -> 1.0f
        }
    }

    private fun stepSample() {
        if (currentFadeSample < fadeDurationSamples) {
            currentFadeSample++
        } else {
            if (fadeState == FadeState.FADING_IN) {
                fadeState = FadeState.IDLE
            }
        }
    }
}
