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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Essentials Design System: Grouped Rounded Card Container (Vertical Pill Pattern).
 * Groups settings or features into a pill-shaped 24dp container with 2dp gaps between cards.
 */
@Composable
fun RoundedCardContainer(
    modifier: Modifier = Modifier,
    spacing: Dp = 2.dp,
    cornerRadius: Dp = 24.dp,
    containerColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
                .background(containerColor),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

/**
 * Essentials Design System: Deterministic Pastel & Vibrant Icon Palette Generator.
 */
object IconColorUtil {
    private val pastelColors =
        listOf(
            Color(0xFFF48FB1), // Pink
            Color(0xFFCE93D8), // Purple
            Color(0xFFB39DDB), // Deep Purple
            Color(0xFF9FA8DA), // Indigo
            Color(0xFF90CAF9), // Blue
            Color(0xFF81D4FA), // Light Blue
            Color(0xFF80DEEA), // Cyan
            Color(0xFF80CBC4), // Teal
            Color(0xFFA5D6A7), // Green
            Color(0xFFC5E1A5), // Light Green
            Color(0xFFE6EE9C), // Lime
            Color(0xFFFFF59D), // Yellow
            Color(0xFFFFE082), // Amber
            Color(0xFFFFCC80), // Orange
            Color(0xFFFFAB91), // Deep Orange
            Color(0xFFBCAAA4), // Brown
            Color(0xFFB0BEC5), // Blue Grey
        )

    fun getPastelColorFor(key: Any): Color {
        val index = abs(key.hashCode()) % pastelColors.size
        return pastelColors[index]
    }

    fun getVibrantColorFor(key: Any): Color {
        val base = getPastelColorFor(key)
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(base.toArgb(), hsv)
        hsv[1] = (hsv[1] * 2.5f).coerceIn(0.6f, 1f)
        hsv[2] = (hsv[2] * 0.65f).coerceIn(0.2f, 0.75f)
        return Color(android.graphics.Color.HSVToColor(hsv))
    }
}

/**
 * Essentials Design System: Standardized Feature Card Row.
 */
@Composable
fun FeatureCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val cardBackground = MaterialTheme.colorScheme.surfaceBright
    val pastelColor = IconColorUtil.getPastelColorFor(title)
    val vibrantColor = IconColorUtil.getVibrantColorFor(title)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(cardBackground)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(color = pastelColor.copy(alpha = 0.25f), shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(22.dp),
                    tint = vibrantColor,
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}

/**
 * Essentials Design System: Feature Toggle Row with M3 Switch.
 */
@Composable
fun FeatureToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    hasMoreSettings: Boolean = false,
    onSettingsClick: (() -> Unit)? = null,
) {
    FeatureCard(
        title = title,
        description = description,
        icon = icon,
        onClick = onSettingsClick ?: { onCheckedChange(!checked) },
        modifier = modifier,
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasMoreSettings && onSettingsClick != null) {
                    VerticalDivider(
                        modifier =
                            Modifier
                                .height(28.dp)
                                .width(1.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                )
            }
        },
    )
}
