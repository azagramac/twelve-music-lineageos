/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import org.lineageos.twelve.R

/**
 * Lune-style audio quality pill row shown in Now Playing (`HI-RES` / `HI-FI` / `HQ`, container
 * format, bit depth / sample rate, and bitrate in kbps).
 */
@Composable
fun HiFiMetadataRow(
    isHiRes: Boolean,
    isHiFi: Boolean,
    isHq: Boolean,
    trackFormat: String?,
    sampleRateAndBitDepth: String?,
    bitrateKbps: Int?,
    dynamicBackgroundEnabled: Boolean = true,
    customActiveColor: Color? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val format = trackFormat?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
    val specsText = sampleRateAndBitDepth?.takeIf { it.isNotBlank() }
    val bitrateText = bitrateKbps?.takeIf { it > 0 }?.let { "$it kbps" }

    val tierName = when {
        isHiRes -> "HI-RES"
        isHiFi -> "HI-FI"
        isHq -> "HQ"
        else -> null
    }

    if (tierName == null && format == null && specsText == null && bitrateText == null) return

    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val dynamicPrimary = customActiveColor ?: MaterialTheme.colorScheme.primary

    val tierBg = when {
        isHiRes -> {
            if (!isDarkTheme) {
                Color(0xFF241A00).copy(alpha = if (dynamicBackgroundEnabled) 0.88f else 0.82f)
            } else {
                if (dynamicBackgroundEnabled) Color(0xFF261D00).copy(alpha = 0.78f)
                else Color(0xFFFFB300).copy(alpha = 0.20f)
            }
        }

        isHiFi -> {
            if (isDarkTheme) dynamicPrimary.copy(alpha = 0.26f)
            else dynamicPrimary.copy(alpha = 0.16f)
        }

        else -> {
            if (isDarkTheme && dynamicBackgroundEnabled) Color.White.copy(alpha = 0.16f)
            else dynamicPrimary.copy(alpha = 0.12f)
        }
    }

    val tierBorder = when {
        isHiRes -> BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(Color(0xFFFFD54F), Color(0xFFFFA000))),
        )

        isHiFi -> BorderStroke(1.dp, dynamicPrimary.copy(alpha = 0.35f))
        else -> BorderStroke(
            0.8.dp,
            if (isDarkTheme && dynamicBackgroundEnabled) {
                Color.White.copy(alpha = 0.25f)
            } else {
                dynamicPrimary.copy(alpha = 0.25f)
            },
        )
    }

    val tierColor = when {
        isHiRes -> if (!isDarkTheme) Color(0xFFFFD54F) else Color(0xFFFFC107)
        isHiFi -> dynamicPrimary
        else -> if (isDarkTheme && dynamicBackgroundEnabled) Color.White else dynamicPrimary
    }

    val badgeBorder = BorderStroke(
        width = 0.8.dp,
        color = when {
            dynamicBackgroundEnabled && isDarkTheme -> Color.White.copy(alpha = 0.20f)
            dynamicBackgroundEnabled -> Color.Black.copy(alpha = 0.12f)
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        },
    )

    val badgeBg = when {
        dynamicBackgroundEnabled && isDarkTheme -> Color.Black.copy(alpha = 0.45f)
        dynamicBackgroundEnabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f)
    }

    val badgeTextColor = if (dynamicBackgroundEnabled && isDarkTheme) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val specsTextColor = if (dynamicBackgroundEnabled && isDarkTheme) {
        Color.White.copy(alpha = 0.85f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (tierName != null) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = tierBg,
                border = tierBorder,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    if (isHiRes) {
                        Icon(
                            painter = painterResource(R.drawable.ic_graphic_eq),
                            contentDescription = null,
                            tint = tierColor,
                            modifier = Modifier.size(11.dp),
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }
                    Text(
                        text = tierName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = tierColor,
                        maxLines = 1,
                    )
                }
            }
        }

        if (format != null) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = badgeBg,
                border = badgeBorder,
            ) {
                Text(
                    text = format,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        if (specsText != null) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = badgeBg,
                border = badgeBorder,
            ) {
                Text(
                    text = specsText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = specsTextColor,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }

        if (bitrateText != null) {
            Surface(
                shape = RoundedCornerShape(percent = 50),
                color = badgeBg,
                border = badgeBorder,
            ) {
                Text(
                    text = bitrateText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = specsTextColor,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

/**
 * Lune-style gold `"Hi-Res"` inline badge for tracklist titles.
 */
@Composable
fun ListHiResBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFF2E2400),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500))),
        ),
    ) {
        Text(
            text = "Hi-Res",
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = Color(0xFFFFD700),
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

/**
 * Lune-style compact `"HI-FI"` inline badge for tracklist titles.
 */
@Composable
fun ListHiFiBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            text = "HI-FI",
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

