/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package com.btl.music.native

import android.util.Log

/**
 * High-performance Native Rust JNI Bridge (libbtl_core.so).
 *
 * Provides sub-millisecond YouTube signature deobfuscation, ARM NEON SIMD
 * 10-band parametric EQ, EBU R128 loudness normalization, and 60 FPS FFT spectrum analysis.
 */
object BtlNativeCore {
    private const val TAG = "BtlNativeCore"

    val isLoaded: Boolean =
        try {
            System.loadLibrary("btl_core")
            Log.i(TAG, "Successfully loaded native libbtl_core.so")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load native libbtl_core.so", t)
            false
        }

    // =========================================================================
    // Equalizer & Audio DSP (ARM NEON SIMD)
    // =========================================================================

    external fun initEngine(sampleRate: Float): Long

    external fun destroyEngine(handle: Long)

    external fun setBandGain(handle: Long, bandIndex: Int, gainDb: Float)

    external fun setAllBands(handle: Long, gains: FloatArray)

    external fun processAudioI16(handle: Long, samples: ShortArray)

    external fun processAudioF32(handle: Long, samples: FloatArray)

    // =========================================================================
    // Spectrum FFT & Loudness Normalization
    // =========================================================================

    external fun computeSpectrum(samples: FloatArray, numBands: Int): FloatArray

    external fun calculateLufs(samples: ShortArray, sampleRate: Float): Float

    external fun calculateNormalizationGain(currentLufs: Float, targetLufs: Float): Float

    // =========================================================================
    // YouTube Cipher & Deobfuscator
    // =========================================================================

    external fun decipherSignature(signature: String, operations: Array<String>): String

    external fun transformNParam(nToken: String): String
}
