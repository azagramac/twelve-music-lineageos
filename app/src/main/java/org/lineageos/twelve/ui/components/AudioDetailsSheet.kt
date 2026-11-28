/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.lineageos.twelve.R
import org.lineageos.twelve.utils.TimestampFormatter
import java.util.Locale

/**
 * Lune-inspired rich song & audio specifications modal sheet opened from Now Playing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AudioDetailsBottomSheet(
    state: NowPlayingUiState,
    accentColor: Color,
    onOpenOutputStats: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val qualityTitle = when {
        state.isHiRes -> "Hi-Res Lossless"
        state.isHiFi -> "Hi-Fi Lossless"
        state.isHq -> "High Quality"
        else -> "Standard Audio"
    }
    val qualityColor = when {
        state.isHiRes -> if (isDark) Color(0xFFFFB300) else Color(0xFFB26A00)
        state.isHiFi -> accentColor
        else -> MaterialTheme.colorScheme.primary
    }
    val formatLabel = state.trackFormat ?: stringResource(R.string.audio_file_type_unknown)
    val compressionLabel = if (state.isHiRes || state.isHiFi) {
        stringResource(R.string.audio_lossless)
    } else {
        stringResource(R.string.audio_compressed)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = qualityColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, qualityColor.copy(alpha = 0.42f)),
                    modifier = Modifier.size(52.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(
                                if (state.isHiRes || state.isHiFi) {
                                    R.drawable.ic_graphic_eq
                                } else {
                                    R.drawable.ic_music_note
                                }
                            ),
                            contentDescription = null,
                            tint = qualityColor,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = qualityTitle,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "$formatLabel • $compressionLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Song metadata card
            val songRows = buildList {
                add(stringResource(R.string.song_title) to state.title)
                add(stringResource(R.string.song_artist) to state.artist)
                add(stringResource(R.string.song_album) to state.album)
                state.genre?.takeIf { it.isNotBlank() }?.let {
                    add(stringResource(R.string.song_genre) to it)
                }
                state.year?.takeIf { it > 0 }?.let {
                    add(stringResource(R.string.song_year) to it.toString())
                }
                if (state.durationMs > 0L) {
                    add(
                        stringResource(R.string.song_duration) to
                            TimestampFormatter.formatTimestampMillis(state.durationMs)
                    )
                }
            }

            DetailsCard(rows = songRows)

            // Technical audio card
            val techRows = buildList {
                add(stringResource(R.string.audio_file_type) to formatLabel)
                state.trackBitrateKbps?.takeIf { it > 0 }?.let {
                    add(stringResource(R.string.audio_bitrate) to "$it kbps")
                }
                state.sampleRateHz?.takeIf { it > 0 }?.let { hz ->
                    val kHzStr = if (hz % 1000 == 0) {
                        "${hz / 1000} kHz ($hz Hz)"
                    } else {
                        "${String.format(Locale.US, "%.1f", hz / 1000f)} kHz ($hz Hz)"
                    }
                    add(stringResource(R.string.audio_sample_rate) to kHzStr)
                }
                state.bitDepth?.takeIf { it > 0 }?.let {
                    add(stringResource(R.string.audio_bit_depth) to "$it-bit")
                }
                state.channelCount?.takeIf { it > 0 }?.let { ch ->
                    val label = when (ch) {
                        1 -> stringResource(R.string.audio_channels_mono)
                        2 -> stringResource(R.string.audio_channels_stereo)
                        else -> "$ch ch"
                    }
                    add(stringResource(R.string.audio_channel_count) to label)
                }
                state.fileSizeBytes?.takeIf { it > 0L }?.let { bytes ->
                    add(stringResource(R.string.audio_file_size) to formatFileSize(bytes))
                }
                state.fileDisplayName?.takeIf { it.isNotBlank() }?.let {
                    add(stringResource(R.string.audio_file_name) to it)
                }
            }

            if (techRows.isNotEmpty()) {
                DetailsCard(rows = techRows)
            }

            FilledTonalButton(
                onClick = {
                    onDismiss()
                    onOpenOutputStats()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_media_output),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.audio_output_details))
            }
        }
    }
}

@Composable
private fun DetailsCard(rows: List<Pair<String, String>>) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, dividerColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            rows.forEachIndexed { index, (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (index < rows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = dividerColor,
                    )
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        else -> String.format(Locale.US, "%.0f KB", kb)
    }
}
