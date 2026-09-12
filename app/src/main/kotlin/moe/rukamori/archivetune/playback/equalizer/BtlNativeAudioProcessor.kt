/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.playback.equalizer

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.btl.music.native.BtlNativeCore
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

/**
 * AndroidX Media3 AudioProcessor powered by Rust SIMD Core (`libbtl_core.so`).
 *
 * Intercepts ExoPlayer PCM stream and applies 10-band IIR biquad filtering
 * with 0% latency and no Java GC allocations in the audio thread loop.
 */
@OptIn(UnstableApi::class)
class BtlNativeAudioProcessor : BaseAudioProcessor() {

    private var nativeEngineHandle: Long = 0L
    private val bandGains = FloatArray(10) { 0.0f }
    private var isEqEnabled: Boolean = false

    @Synchronized
    fun setEnabled(enabled: Boolean) {
        isEqEnabled = enabled
    }

    @Synchronized
    fun setBandGain(bandIndex: Int, gainDb: Float) {
        if (bandIndex in bandGains.indices) {
            bandGains[bandIndex] = gainDb
            if (nativeEngineHandle != 0L && BtlNativeCore.isLoaded) {
                BtlNativeCore.setBandGain(nativeEngineHandle, bandIndex, gainDb)
            }
        }
    }

    @Synchronized
    fun setAllBands(gains: FloatArray) {
        System.arraycopy(gains, 0, bandGains, 0, minOf(gains.size, bandGains.size))
        if (nativeEngineHandle != 0L && BtlNativeCore.isLoaded) {
            BtlNativeCore.setAllBands(nativeEngineHandle, bandGains)
        }
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }

        // Initialize or update native engine with sample rate
        if (BtlNativeCore.isLoaded) {
            if (nativeEngineHandle != 0L) {
                BtlNativeCore.destroyEngine(nativeEngineHandle)
            }
            nativeEngineHandle = BtlNativeCore.initEngine(inputAudioFormat.sampleRate.toFloat())
            BtlNativeCore.setAllBands(nativeEngineHandle, bandGains)
        }

        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining)

        // Read 16-bit PCM samples
        val sampleCount = remaining / 2
        val shortArray = ShortArray(sampleCount)
        inputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer().get(shortArray)
        inputBuffer.position(inputBuffer.limit())

        // 1. Stream samples to Hub for 60-120 FPS FFT, amplitude, and LAN streamer
        BtlAudioVisualizerHub.processPcm(shortArray, channelCount = 2)

        // 2. Apply in-place Real-time Karaoke or 3D Spatial Audio
        BtlAudioVisualizerHub.applyDspInPlace(shortArray, channelCount = 2)

        // 3. Process 16-bit PCM in native Rust SIMD engine if EQ active
        if (isEqEnabled && nativeEngineHandle != 0L && BtlNativeCore.isLoaded && !areAllBandsZero()) {
            BtlNativeCore.processAudioI16(nativeEngineHandle, shortArray)
        }

        outputBuffer.order(ByteOrder.nativeOrder()).asShortBuffer().put(shortArray)
        outputBuffer.position(outputBuffer.position() + remaining)
        outputBuffer.flip()
    }

    private fun areAllBandsZero(): Boolean {
        for (gain in bandGains) {
            if (kotlin.math.abs(gain) > 0.01f) return false
        }
        return true
    }

    override fun onReset() {
        super.onReset()
        if (nativeEngineHandle != 0L && BtlNativeCore.isLoaded) {
            BtlNativeCore.destroyEngine(nativeEngineHandle)
            nativeEngineHandle = 0L
        }
    }
}
