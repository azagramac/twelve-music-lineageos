/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.components

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.lineageos.twelve.R
import org.lineageos.twelve.ui.haptics.rememberTwelveHaptics
import org.lineageos.twelve.utils.AudioMetadataExtractor
import org.lineageos.twelve.utils.MimeUtils
import org.lineageos.twelve.utils.TimestampFormatter

/**
 * Lune-inspired Material 3 Expressive tracklist row displaying inline `Hi-Res` / `HI-FI` quality
 * badges next to the track title and `"FORMAT | XXX kbps"` below the artist name.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TrackListItem(
    title: String?,
    artistName: String?,
    durationMs: Long?,
    mimeType: String?,
    playbackUri: Uri? = null,
    trackNumber: Int? = null,
    isCurrent: Boolean = false,
    showDragHandle: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptics = rememberTwelveHaptics()

    val fallbackSpecs = remember(mimeType, playbackUri) {
        AudioMetadataExtractor.classify(
            format = mimeType?.let { MimeUtils.mimeTypeToDisplayName(it) },
            bitrateBps = null,
            sampleRateHz = null,
            bitDepth = null,
            isLosslessHint = MimeUtils.isLosslessMimeType(
                mimeType = mimeType,
                uriString = playbackUri?.toString(),
            ),
        )
    }

    var specs by remember(playbackUri, mimeType) {
        mutableStateOf(AudioMetadataExtractor.getCached(playbackUri) ?: fallbackSpecs)
    }

    LaunchedEffect(playbackUri, mimeType, durationMs) {
        if (playbackUri != null && AudioMetadataExtractor.getCached(playbackUri) == null) {
            specs = AudioMetadataExtractor.extract(
                context = context,
                uri = playbackUri,
                mimeType = mimeType,
                durationMs = durationMs,
                deepExtract = false,
            )
        }
    }

    val formatBitrateText = remember(specs) {
        listOfNotNull(
            specs.format,
            specs.bitrateKbps?.let { "$it kbps" },
        )
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" | ")
    }

    val containerColor = if (isCurrent) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
    } else {
        Color.Transparent
    }
    val headlineColor = if (isCurrent) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    haptics.click()
                    onClick()
                },
                onLongClick = onLongClick?.let { callback ->
                    {
                        haptics.click()
                        callback()
                    }
                },
            ),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrent) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isCurrent -> Icon(
                        painter = painterResource(R.drawable.ic_track_playing),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )

                    trackNumber != null -> Text(
                        text = trackNumber.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    else -> Icon(
                        painter = painterResource(R.drawable.ic_music_note),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (specs.isHiRes) {
                        ListHiResBadge()
                        Spacer(modifier = Modifier.width(6.dp))
                    } else if (specs.isHiFi) {
                        ListHiFiBadge()
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = title ?: stringResource(R.string.unknown),
                        style = if (isCurrent) {
                            MaterialTheme.typography.bodyLargeEmphasized
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = headlineColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = artistName ?: stringResource(R.string.artist_unknown),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (formatBitrateText != null) {
                    Text(
                        text = formatBitrateText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                durationMs?.let {
                    Text(
                        text = TimestampFormatter.formatTimestampMillis(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }

                if (showDragHandle) {
                    Icon(
                        painter = painterResource(R.drawable.ic_drag_handle),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
