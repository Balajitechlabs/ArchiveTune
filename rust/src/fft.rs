//! High-speed FFT frequency analyzer for 60 FPS Compose Audio Visualizer.

use std::f32::consts::PI;

/// Computes the magnitude spectrum of an audio sample buffer using Cooley-Tukey FFT.
/// Output is downsampled to `num_bands` (e.g., 32, 64) for visual rendering.
pub fn compute_spectrum(samples: &[f32], num_output_bands: usize) -> Vec<f32> {
    let n = samples.len().next_power_of_two();
    if n < 8 {
        return vec![0.0; num_output_bands];
    }

    let mut real = vec![0.0f32; n];
    let mut imag = vec![0.0f32; n];

    // Apply Hann window and copy into real buffer
    for (i, &s) in samples.iter().take(n).enumerate() {
        let window = 0.5 * (1.0 - (2.0 * PI * i as f32 / n as f32).cos());
        real[i] = s * window;
    }

    // Bit-reversal permutation
    let mut j = 0;
    for i in 0..(n - 1) {
        if i < j {
            real.swap(i, j);
            imag.swap(i, j);
        }
        let mut k = n / 2;
        while k <= j {
            j -= k;
            k /= 2;
        }
        j += k;
    }

    // Cooley-Tukey Radix-2 decimation-in-time
    let mut len = 2;
    while len <= n {
        let half = len / 2;
        let angle = -2.0 * PI / len as f32;
        let w_real = angle.cos();
        let w_imag = angle.sin();

        let mut i = 0;
        while i < n {
            let mut u_real = 1.0f32;
            let mut u_imag = 0.0f32;

            for j in 0..half {
                let u = i + j;
                let v = i + j + half;

                let t_real = u_real * real[v] - u_imag * imag[v];
                let t_imag = u_real * imag[v] + u_imag * real[v];

                real[v] = real[u] - t_real;
                imag[v] = imag[u] - t_imag;
                real[u] += t_real;
                imag[u] += t_imag;

                let next_u_real = u_real * w_real - u_imag * w_imag;
                u_imag = u_real * w_imag + u_imag * w_real;
                u_real = next_u_real;
            }
            i += len;
        }
        len *= 2;
    }

    // Compute magnitudes for positive half of spectrum
    let half_n = n / 2;
    let mut magnitudes = Vec::with_capacity(half_n);
    for i in 0..half_n {
        let mag = (real[i] * real[i] + imag[i] * imag[i]).sqrt() / (n as f32);
        magnitudes.push(mag);
    }

    // Downsample/aggregate into output frequency bands
    let mut output = vec![0.0f32; num_output_bands];
    if num_output_bands == 0 || magnitudes.is_empty() {
        return output;
    }

    let chunk_size = (magnitudes.len() as f32 / num_output_bands as f32).max(1.0);
    for band in 0..num_output_bands {
        let start = (band as f32 * chunk_size) as usize;
        let end = (((band + 1) as f32 * chunk_size) as usize).min(magnitudes.len());
        if start < end {
            let sum: f32 = magnitudes[start..end].iter().sum();
            let avg = sum / (end - start) as f32;
            // Apply gentle logarithmic scaling for pleasant visualization
            output[band] = (avg * 10.0).clamp(0.0, 1.0);
        }
    }

    output
}
