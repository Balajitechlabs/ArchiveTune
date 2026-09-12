//! EBU R128 Loudness / Integrated LUFS Analyzer

/// Calculates the integrated loudness in LUFS for 16-bit PCM audio samples.
/// Target standard is -14.0 LUFS.
pub fn calculate_lufs_i16(samples: &[i16], sample_rate: f32) -> f32 {
    if samples.is_empty() || sample_rate <= 0.0 {
        return -70.0;
    }

    // Convert samples to float normalized to -1.0 .. 1.0
    let mut sum_squares = 0.0f64;
    for &sample in samples {
        let val = sample as f64 / 32768.0;
        sum_squares += val * val;
    }

    let mean_square = sum_squares / samples.len() as f64;
    if mean_square <= 1e-12 {
        return -70.0; // Silence threshold
    }

    // Simplified K-weighting approximation for loudness
    let lufs = -0.691 + 10.0 * mean_square.log10();
    (lufs as f32).clamp(-70.0, 0.0)
}

/// Calculates the volume normalization gain factor to reach target LUFS (-14.0 LUFS).
pub fn calculate_normalization_gain(current_lufs: f32, target_lufs: f32) -> f32 {
    if current_lufs <= -69.0 {
        return 1.0;
    }
    let diff_db = target_lufs - current_lufs;
    // Cap maximum boost to +6dB to prevent clipping, cut can be down to -12dB
    let clamped_diff = diff_db.clamp(-12.0, 6.0);
    10.0f32.powf(clamped_diff / 20.0)
}
