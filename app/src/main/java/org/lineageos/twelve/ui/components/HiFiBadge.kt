/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.lineageos.twelve.R
import org.lineageos.twelve.ui.theme.TwelveMorphShapes
import org.lineageos.twelve.ui.theme.rememberMorphingShape

/**
 * Minimalist Hi-Fi micro-container shown next to / below the track title in Now Playing.
 *
 * When [isHiFi] is true, displays a subtle tonal "Hi-Fi" badge backed by a morphing M3 Expressive
 * geometric shape from [TwelveMorphShapes], paired with clean technical metadata such as
 * `"FLAC • 24-bit / 44.1 kHz • 912 kbps"`.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HiFiMetadataRow(
    isHiFi: Boolean,
    isPlaying: Boolean,
    trackFormat: String?,
    sampleRateAndBitDepth: String?,
    bitrateKbps: Int?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val infoParts = buildList {
        trackFormat?.takeIf { it.isNotBlank() }?.let(::add)
        sampleRateAndBitDepth?.takeIf { it.isNotBlank() }?.let(::add)
        bitrateKbps?.takeIf { it > 0 }?.let { add("$it kbps") }
    }

    if (!isHiFi && infoParts.isEmpty()) return

    val badgeShape = rememberMorphingShape(
        from = TwelveMorphShapes.Pill,
        to = TwelveMorphShapes.Cookie7Sided,
        morphed = isPlaying && isHiFi,
    )

    Surface(
        modifier = modifier
            .animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec())
            .clip(RoundedCornerShape(50))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isHiFi) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    shape = badgeShape,
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.hi_fi),
                            style = MaterialTheme.typography.labelSmallEmphasized,
                            maxLines = 1,
                        )
                    }
                }
            }

            if (infoParts.isNotEmpty()) {
                Text(
                    text = infoParts.joinToString(" • "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Compact format tag for tracklist rows (e.g. `"FLAC"`, `"WAV"`, `"ALAC"`).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CompactFormatBadge(
    format: String,
    isLossless: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = if (isLossless) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (isLossless) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = format,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmallEmphasized,
            maxLines = 1,
        )
    }
}
