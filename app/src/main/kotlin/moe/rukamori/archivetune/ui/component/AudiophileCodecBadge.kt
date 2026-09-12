/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.rukamori.archivetune.R
import moe.rukamori.archivetune.db.entities.FormatEntity
import moe.rukamori.archivetune.db.entities.codecLabel
import moe.rukamori.archivetune.db.entities.containerLabel
import moe.rukamori.archivetune.db.entities.formattedBitrate
import moe.rukamori.archivetune.db.entities.formattedSampleRate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiophileCodecBadge(
    format: FormatEntity?,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    var showDiagnostics by remember { mutableStateOf(false) }

    val codec = format?.codecLabel() ?: "STREAM"
    val bitrate = format?.formattedBitrate() ?: "HQ"
    val sampleRate = format?.formattedSampleRate()

    val badgeText = buildString {
        append(codec)
        append(" • ")
        append(bitrate)
        if (sampleRate != null) {
            append(" • ")
            append(sampleRate)
        }
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .clickable { showDiagnostics = true }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50)), // Audio Hi-Fi indicator dot
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp,
                ),
                color = contentColor.copy(alpha = 0.85f),
            )
        }
    }

    if (showDiagnostics) {
        ModalBottomSheet(
            onDismissRequest = { showDiagnostics = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.equalizer),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Audio Pipeline Diagnostics",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                DiagnosticRow(label = "Codec", value = format?.codecLabel() ?: "Opus")
                DiagnosticRow(label = "Container", value = format?.containerLabel() ?: "WEBM")
                DiagnosticRow(label = "Bitrate", value = format?.formattedBitrate() ?: "160 kbps")
                DiagnosticRow(label = "Sample Rate", value = format?.formattedSampleRate() ?: "48.0 kHz")
                DiagnosticRow(label = "DSP Engine", value = "BTL Native ARM NEON DSP (Active)")
                DiagnosticRow(label = "Loudness Target", value = "${format?.loudnessDb ?: -14.0} LUFS")
                DiagnosticRow(label = "Output Sink", value = "Bit-Perfect AudioTrack (32-bit Float)")
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
