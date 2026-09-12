/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.ui.component

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import moe.rukamori.archivetune.playback.equalizer.BtlAudioVisualizerHub
import org.intellij.lang.annotations.Language

@Language("AGSL")
private const val FLUID_SHADER_SRC = """
    uniform float2 uResolution;
    uniform float uTime;
    uniform float uAudioAmplitude;
    uniform float4 uColorPrimary;
    uniform float4 uColorSecondary;

    float hash(float2 p) {
        return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        f = f * f * (3.0 - 2.0 * f);
        float a = hash(i);
        float b = hash(i + float2(1.0, 0.0));
        float c = hash(i + float2(0.0, 1.0));
        float d = hash(i + float2(1.0, 1.0));
        return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
    }

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / uResolution;
        float pulse = 1.0 + (uAudioAmplitude * 0.35);
        float n = noise((uv * 2.5 * pulse) + float2(uTime * 0.1, uTime * 0.08));
        float n2 = noise((uv * 4.0) - float2(uTime * 0.05, uTime * 0.07));
        float blend = smoothstep(0.2, 0.8, (n + n2) * 0.5);

        half4 col = mix(half4(uColorPrimary), half4(uColorSecondary), blend);
        col.a = half(0.35 + (uAudioAmplitude * 0.25));
        return col;
    }
"""

/**
 * Audio-reactive AGSL Procedural Fluid Shader (FLAGSHIP-04).
 *
 * Runs natively on the mobile GPU via Android RuntimeShader (API 33+),
 * pulsing organically with the real-time RMS bass amplitude of the song.
 */
@Composable
fun BtlAgslFluidShader(
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    val amplitude by BtlAudioVisualizerHub.amplitude.collectAsStateWithLifecycle()

    val infiniteTransition = rememberInfiniteTransition(label = "agsl_fluid_time")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(120000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "agsl_time",
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(FLUID_SHADER_SRC) }

        Canvas(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    shader.setFloatUniform("uResolution", size.width, size.height)
                    shader.setFloatUniform("uTime", time)
                    shader.setFloatUniform("uAudioAmplitude", amplitude)
                    shader.setColorUniform(
                        "uColorPrimary",
                        android.graphics.Color.valueOf(
                            primaryColor.red,
                            primaryColor.green,
                            primaryColor.blue,
                            primaryColor.alpha,
                        ),
                    )
                    shader.setColorUniform(
                        "uColorSecondary",
                        android.graphics.Color.valueOf(
                            secondaryColor.red,
                            secondaryColor.green,
                            secondaryColor.blue,
                            secondaryColor.alpha,
                        ),
                    )
                    renderEffect = RenderEffect
                        .createRuntimeShaderEffect(shader, "uContent")
                        .asComposeRenderEffect()
                }
        ) {
            drawRect(color = Color.Transparent)
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.25f + amplitude * 0.2f),
                            secondaryColor.copy(alpha = 0.15f),
                            Color.Transparent,
                        ),
                    )
                )
        )
    }
}
