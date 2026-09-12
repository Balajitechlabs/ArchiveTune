pub mod cipher;
pub mod dsp;
pub mod fft;
pub mod loudness;

use cipher::{decipher_signature, parse_op, transform_n_param};
use dsp::EqualizerEngine;
use fft::compute_spectrum;
use jni::objects::{JClass, JFloatArray, JObjectArray, JShortArray, JString};
use jni::sys::{jfloat, jfloatArray, jint, jlong, jstring};
use jni::JNIEnv;
use loudness::{calculate_lufs_i16, calculate_normalization_gain};

// =============================================================================
// Equalizer Engine Lifecycle & DSP JNI
// =============================================================================

#[no_mangle]
pub extern "system" fn Java_com_btl_music_native_BtlNativeCore_initEngine(
    _env: JNIEnv,
    _class: JClass,
    sample_rate: jfloat,
) -> jlong {
    let engine = Box::new(EqualizerEngine::new(sample_rate));
    Box::into_raw(engine) as jlong
}

#[no_mangle]
pub extern "system" fn Java_com_btl_music_native_BtlNativeCore_destroyEngine(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        unsafe {
            let _ = Box::from_raw(handle as *mut EqualizerEngine);
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_btl_music_native_BtlNativeCore_setBandGain(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
    band_idx: jint,
    gain_db: jfloat,
) {
    if handle != 0 {
        let engine = unsafe { &mut *(handle as *mut EqualizerEngine) };
        engine.set_band_gain(band_idx as usize, gain_db);
    }
}

#[no_mangle]
pub extern "system" fn Java_com_btl_music_native_BtlNativeCore_setAllBands(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    gains: JFloatArray,
) {
    if handle != 0 {
        let engine = unsafe { &mut *(handle as *mut EqualizerEngine) };
        let len = match env.get_array_length(&gains) {
            Ok(l) => l as usize,
            Err(_) => return,
        };
        let mut buf = vec![0.0f32; len];
        if env.get_float_array_region(&gains, 0, &mut buf).is_ok() {
            engine.set_all_bands(&buf);
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_btl_music_native_BtlNativeCore_processAudioI16(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    samples: JShortArray,
) {
    if handle != 0 {
        let engine = unsafe { &mut *(handle as *mut EqualizerEngine) };
        let len = match env.get_array_length(&samples) {
            Ok(l) => l as usize,
            Err(_) => return,
        };
        let mut buf = vec![0i16; len];
        if env.get_short_array_region(&samples, 0, &mut buf).is_ok() {
            engine.process_interleaved_i16(&mut buf);
            let _ = env.set_short_array_region(&samples, 0, &buf);
        }
    }
}

#[no_mangle]
pub extern "system" fn Java_com_btl_music_native_BtlNativeCore_processAudioF32(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    samples: JFloatArray,
) {
    if handle != 0 {
        let engine = unsafe { &mut *(handle as *mut EqualizerEngine) };
        let len = match env.get_array_length(&samples) {
            Ok(l) => l as usize,
            Err(_) => return,
        };
        let mut buf = vec![0.0f32; len];
        if env.get_float_array_region(&samples, 0, &mut buf).is_ok() {
            engine.process_interleaved_f32(&mut buf);
            let _ = env.set_float_array_region(&samples, 0, &buf);
        }
    }
}

// =============================================================================
// Spectrum FFT & Loudness JNI
// =============================================================================

#[no_mangle]
pub extern "system" fn Java_com_btl_music_native_BtlNativeCore_computeSpectrum(
    env: JNIEnv,
    _class: JClass,
    samples: JFloatArray,
    num_bands: jint,
) -> jfloatArray {
    let len = match env.get_array_length(&samples) {
        Ok(l) => l as usize,
        Err(_) => return std::ptr::null_mut(),
    };
    let mut buf = vec![0.0f32; len];
    if env.get_float_array_region(&samples, 0, &mut buf).is_err() {
        return std::ptr::null_mut();
    }

    let spectrum = compute_spectrum(&buf, num_bands as usize);
    let output_array = match env.new_float_array(spectrum.len() as i32) {
        Ok(arr) => arr,
        Err(_) => return std::ptr::null_mut(),
    };
    let _ = env.set_float_array_region(&output_array, 0, &spectrum);
    output_array.as_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_btl_music_native_BtlNativeCore_calculateLufs(
    env: JNIEnv,
    _class: JClass,
    samples: JShortArray,
    sample_rate: jfloat,
) -> jfloat {
    let len = match env.get_array_length(&samples) {
        Ok(l) => l as usize,
        Err(_) => return -70.0,
    };
    let mut buf = vec![0i16; len];
    if env.get_short_array_region(&samples, 0, &mut buf).is_err() {
        return -70.0;
    }
    calculate_lufs_i16(&buf, sample_rate)
}

#[no_mangle]
pub extern "system" fn Java_com_btl_music_native_BtlNativeCore_calculateNormalizationGain(
    _env: JNIEnv,
    _class: JClass,
    current_lufs: jfloat,
    target_lufs: jfloat,
) -> jfloat {
    calculate_normalization_gain(current_lufs, target_lufs)
}

// =============================================================================
// YouTube Cipher & Deobfuscator JNI
// =============================================================================

#[no_mangle]
pub extern "system" fn Java_com_btl_music_native_BtlNativeCore_decipherSignature(
    mut env: JNIEnv,
    _class: JClass,
    signature: JString,
    operations: JObjectArray,
) -> jstring {
    let raw_sig = signature.as_raw();
    let sig_str: String = match env.get_string(&signature) {
        Ok(s) => match s.to_str() {
            Ok(valid) => valid.to_owned(),
            Err(_) => return raw_sig,
        },
        Err(_) => return raw_sig,
    };

    let op_count = match env.get_array_length(&operations) {
        Ok(len) => len as usize,
        Err(_) => return raw_sig,
    };

    let mut ops = Vec::with_capacity(op_count);
    for i in 0..op_count {
        let obj = match env.get_object_array_element(&operations, i as i32) {
            Ok(o) => o,
            Err(_) => continue,
        };
        let jstr = JString::from(obj);
        let parsed_op = {
            let java_str = match env.get_string(&jstr) {
                Ok(s) => s,
                Err(_) => continue,
            };
            let rust_str = match java_str.to_str() {
                Ok(s) => s,
                Err(_) => continue,
            };
            parse_op(rust_str)
        };
        if let Some(op) = parsed_op {
            ops.push(op);
        }
    }

    let deciphered = decipher_signature(&sig_str, &ops);
    match env.new_string(deciphered) {
        Ok(jstr) => jstr.as_raw(),
        Err(_) => raw_sig,
    }
}

#[no_mangle]
pub extern "system" fn Java_com_btl_music_native_BtlNativeCore_transformNParam(
    mut env: JNIEnv,
    _class: JClass,
    n_token: JString,
) -> jstring {
    let raw_n = n_token.as_raw();
    let n_str: String = match env.get_string(&n_token) {
        Ok(s) => match s.to_str() {
            Ok(valid) => valid.to_owned(),
            Err(_) => return raw_n,
        },
        Err(_) => return raw_n,
    };

    let transformed = transform_n_param(&n_str);
    match env.new_string(transformed) {
        Ok(jstr) => jstr.as_raw(),
        Err(_) => raw_n,
    }
}
