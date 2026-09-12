//! High-performance Audio DSP: 10-band Parametric Equalizer
//! Based on standard Robert Bristow-Johnson Biquad IIR Filter Formulas.

use std::f32::consts::PI;

pub const NUM_BANDS: usize = 10;
pub const BAND_FREQUENCIES: [f32; NUM_BANDS] = [
    31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0,
];

#[derive(Debug, Clone, Copy)]
pub struct BiquadCoeffs {
    pub b0: f32,
    pub b1: f32,
    pub b2: f32,
    pub a1: f32,
    pub a2: f32,
}

impl Default for BiquadCoeffs {
    fn default() -> Self {
        Self {
            b0: 1.0,
            b1: 0.0,
            b2: 0.0,
            a1: 0.0,
            a2: 0.0,
        }
    }
}

impl BiquadCoeffs {
    /// Computes peaking EQ coefficients for a given frequency, gain in dB, Q factor, and sample rate.
    pub fn peaking_eq(center_freq: f32, gain_db: f32, q: f32, sample_rate: f32) -> Self {
        if gain_db.abs() < 0.01 {
            return Self::default();
        }

        let a = 10.0f32.powf(gain_db / 40.0);
        let omega = 2.0 * PI * center_freq / sample_rate;
        let sin_omega = omega.sin();
        let cos_omega = omega.cos();
        let alpha = sin_omega / (2.0 * q);

        let a0 = 1.0 + alpha / a;
        let b0 = (1.0 + alpha * a) / a0;
        let b1 = (-2.0 * cos_omega) / a0;
        let b2 = (1.0 - alpha * a) / a0;
        let a1 = (-2.0 * cos_omega) / a0;
        let a2 = (1.0 - alpha / a) / a0;

        Self { b0, b1, b2, a1, a2 }
    }
}

#[derive(Debug, Clone, Default)]
pub struct ChannelFilterState {
    pub x1: f32,
    pub x2: f32,
    pub y1: f32,
    pub y2: f32,
}

impl ChannelFilterState {
    #[inline(always)]
    pub fn process_sample(&mut self, input: f32, coeffs: &BiquadCoeffs) -> f32 {
        let output = coeffs.b0 * input + coeffs.b1 * self.x1 + coeffs.b2 * self.x2
            - coeffs.a1 * self.y1
            - coeffs.a2 * self.y2;

        self.x2 = self.x1;
        self.x1 = input;
        self.y2 = self.y1;
        self.y1 = output;

        output
    }
}

/// A 10-band stereo parametric equalizer engine.
pub struct EqualizerEngine {
    pub sample_rate: f32,
    pub bands_gain_db: [f32; NUM_BANDS],
    coeffs: [BiquadCoeffs; NUM_BANDS],
    left_states: [ChannelFilterState; NUM_BANDS],
    right_states: [ChannelFilterState; NUM_BANDS],
}

impl EqualizerEngine {
    pub fn new(sample_rate: f32) -> Self {
        let mut engine = Self {
            sample_rate,
            bands_gain_db: [0.0; NUM_BANDS],
            coeffs: [BiquadCoeffs::default(); NUM_BANDS],
            left_states: Default::default(),
            right_states: Default::default(),
        };
        engine.recompute_coeffs();
        engine
    }

    pub fn set_band_gain(&mut self, band_idx: usize, gain_db: f32) {
        if band_idx < NUM_BANDS {
            self.bands_gain_db[band_idx] = gain_db.clamp(-24.0, 24.0);
            self.recompute_coeffs();
        }
    }

    pub fn set_all_bands(&mut self, gains: &[f32]) {
        for (i, &gain) in gains.iter().take(NUM_BANDS).enumerate() {
            self.bands_gain_db[i] = gain.clamp(-24.0, 24.0);
        }
        self.recompute_coeffs();
    }

    fn recompute_coeffs(&mut self) {
        for i in 0..NUM_BANDS {
            let freq = BAND_FREQUENCIES[i];
            let gain = self.bands_gain_db[i];
            self.coeffs[i] = BiquadCoeffs::peaking_eq(freq, gain, 1.414, self.sample_rate);
        }
    }

    /// Process interleaved stereo 16-bit PCM samples in place.
    pub fn process_interleaved_i16(&mut self, samples: &mut [i16]) {
        for chunk in samples.chunks_exact_mut(2) {
            let mut left = chunk[0] as f32 / 32768.0;
            let mut right = chunk[1] as f32 / 32768.0;

            for i in 0..NUM_BANDS {
                let coeff = &self.coeffs[i];
                left = self.left_states[i].process_sample(left, coeff);
                right = self.right_states[i].process_sample(right, coeff);
            }

            chunk[0] = (left.clamp(-1.0, 1.0) * 32767.0) as i16;
            chunk[1] = (right.clamp(-1.0, 1.0) * 32767.0) as i16;
        }
    }

    /// Process interleaved stereo 32-bit float samples in place.
    pub fn process_interleaved_f32(&mut self, samples: &mut [f32]) {
        for chunk in samples.chunks_exact_mut(2) {
            let mut left = chunk[0];
            let mut right = chunk[1];

            for i in 0..NUM_BANDS {
                let coeff = &self.coeffs[i];
                left = self.left_states[i].process_sample(left, coeff);
                right = self.right_states[i].process_sample(right, coeff);
            }

            chunk[0] = left.clamp(-1.0, 1.0);
            chunk[1] = right.clamp(-1.0, 1.0);
        }
    }
}
