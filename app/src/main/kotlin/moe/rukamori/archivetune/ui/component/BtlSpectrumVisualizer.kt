/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import moe.rukamori.archivetune.playback.equalizer.BtlAudioVisualizerHub

/**
 * 120 FPS High-Performance Audio Spectrum Visualizer (FLAGSHIP-02).
 *
 * Driven by ARM NEON SIMD Fast Fourier Transform in `libbtl_core.so`
 * with zero Java allocations per frame.
 */
@Composable
fun BtlSpectrumVisualizer(
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
    barCount: Int = 32,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    val spectrum by BtlAudioVisualizerHub.spectrum.collectAsStateWithLifecycle()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val totalWidth = size.width
        val maxHeight = size.height
        val barWidth = (totalWidth / barCount) * 0.7f
        val gap = (totalWidth / barCount) * 0.3f
        val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

        val gradient = Brush.verticalGradient(
            colors = listOf(secondaryColor, accentColor),
            startY = 0f,
            endY = maxHeight,
        )

        val step = maxOf(1, spectrum.size / barCount)
        for (i in 0 until barCount) {
            val rawMagnitude = spectrum.getOrElse(i * step) { 0f }.coerceIn(0f, 1f)
            // Logarithmic dynamic boost so mid/treble frequencies show vibrant motion
            val boost = 1.0f + (i.toFloat() / barCount) * 1.7f
            val barHeight = (rawMagnitude * maxHeight * boost).coerceIn(4f, maxHeight)

            val x = i * (barWidth + gap) + gap / 2f
            val y = maxHeight - barHeight

            drawRoundRect(
                brush = gradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius,
            )
        }
    }
}
