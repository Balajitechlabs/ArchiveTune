/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.ui.component

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import org.intellij.lang.annotations.Language

/**
 * AGSL Liquid Wave Ripple shader providing an organic fluid motion
 * across album artwork and playback backgrounds on Android 13+ (API 33+).
 */
@Language("AGSL")
private const val LIQUID_SHADER_SRC = """
    uniform shader composable;
    uniform float2 size;
    uniform float time;
    uniform float intensity;

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / size;
        float waveX = sin(uv.y * 12.0 + time * 2.5) * 0.015 * intensity;
        float waveY = cos(uv.x * 12.0 + time * 2.5) * 0.015 * intensity;
        float2 distortedCoord = fragCoord + float2(waveX, waveY) * size.x;
        return composable.eval(distortedCoord);
    }
"""

fun Modifier.liquidRipple(
    enabled: Boolean = true,
    intensity: Float = 1.0f,
): Modifier = composed {
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return@composed this
    }

    val timeAnim = remember { Animatable(0f) }

    LaunchedEffect(enabled) {
        if (enabled) {
            timeAnim.animateTo(
                targetValue = 1000f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 200000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            )
        }
    }

    val shader = remember { RuntimeShader(LIQUID_SHADER_SRC) }

    graphicsLayer {
        shader.setFloatUniform("size", size.width, size.height)
        shader.setFloatUniform("time", timeAnim.value)
        shader.setFloatUniform("intensity", intensity)
        renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "composable").asComposeRenderEffect()
    }
}
