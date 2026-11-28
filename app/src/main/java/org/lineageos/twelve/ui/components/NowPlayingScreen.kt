/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.components

import android.graphics.PixelFormat
import android.os.Build
import android.view.SurfaceView
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import org.lineageos.twelve.R
import org.lineageos.twelve.models.OutputConfiguration
import org.lineageos.twelve.models.RepeatMode
import org.lineageos.twelve.models.Thumbnail
import org.lineageos.twelve.ui.haptics.rememberTwelveHaptics
import org.lineageos.twelve.ui.theme.TwelveMorphShapes
import org.lineageos.twelve.ui.theme.rememberMorphingShape

@Immutable
data class NowPlayingUiState(
    val title: String,
    val artist: String,
    val album: String,
    val artwork: Thumbnail? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val canShuffle: Boolean = true,
    val canPrevious: Boolean = true,
    val canNext: Boolean = true,
    val canPlayPause: Boolean = true,
    val canRepeat: Boolean = true,
    val canChangeSpeed: Boolean = true,
    val speedLabel: String = "1×",
    val isFavorite: Boolean? = null,
    val isVisualizerEnabled: Boolean = false,
    val isHiFi: Boolean = false,
    val trackFormat: String? = null,
    val trackSampleRateAndBitDepth: String? = null,
    val trackBitrateKbps: Int? = null,
    val outputDeviceType: OutputConfiguration.Device.Type? = null,
    val hasPotentialIssues: Boolean = false,
    val isDirectOutputActive: Boolean = false,
    val dynamicBackgroundEnabled: Boolean = true,
    val previousLyric: String? = null,
    val currentLyric: String? = null,
    val nextLyric: String? = null,
    val showLyricsCard: Boolean = false,
)

@Immutable
data class NowPlayingCallbacks(
    val onNavigateUp: () -> Unit = {},
    val onOpenStats: () -> Unit = {},
    val onSeekToPosition: (Long) -> Unit = {},
    val onToggleShuffle: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onTogglePlayPause: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onToggleRepeat: () -> Unit = {},
    val onAddToPlaylist: () -> Unit = {},
    val onOpenTrackDetails: () -> Unit = {},
    val onAddToQueue: () -> Unit = {},
    val onPlayNext: () -> Unit = {},
    val onOpenSpeedControl: () -> Unit = {},
    val onOpenEqualizer: () -> Unit = {},
    val onCycleVisualizer: () -> Unit = {},
    val onOpenQueue: () -> Unit = {},
    val onToggleFavorite: () -> Unit = {},
    val onOpenLyrics: () -> Unit = {},
    val onVisualizerSurfaceReady: ((SurfaceView) -> Unit)? = null,
)

/**
 *Material 3 Expressive Now Playing screen featuring a dynamic blurred album art
 * backdrop, crisp centered hierarchy, prominent Hi-Fi badge & telemetry, wavy progress indicator,
 * springy transport controls with haptics, and `Nier-Visualizer` [SurfaceView] encapsulation.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NowPlayingScreen(
    state: NowPlayingUiState,
    callbacks: NowPlayingCallbacks,
    modifier: Modifier = Modifier,
    previewVisualizerBars: Boolean = false,
) {
    val haptics = rememberTwelveHaptics()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        if (state.dynamicBackgroundEnabled) {
            DynamicArtworkBackground(
                artwork = state.artwork,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(
                        onClick = {
                            haptics.click()
                            callbacks.onNavigateUp()
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Text(
                        text = state.album,
                        style = MaterialTheme.typography.titleSmallEmphasized,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center,
                    )

                    OutputRoutePill(
                        deviceType = state.outputDeviceType,
                        hasIssues = state.hasPotentialIssues,
                        isDirect = state.isDirectOutputActive,
                        onClick = {
                            haptics.click()
                            callbacks.onOpenStats()
                        },
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Crisp rounded square album artwork + optional Nier-Visualizer
                Surface(
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 12.dp,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (state.artwork != null) {
                            AsyncImage(
                                model = state.artwork,
                                contentDescription = stringResource(
                                    R.string.content_description_thumbnail
                                ),
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(R.drawable.ic_music_note),
                                error = painterResource(R.drawable.ic_music_note),
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.tertiaryContainer,
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_music_note),
                                    contentDescription = stringResource(
                                        R.string.content_description_thumbnail
                                    ),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(72.dp),
                                )
                            }
                        }

                        if (state.isVisualizerEnabled && !previewVisualizerBars) {
                            AndroidView(
                                factory = { context ->
                                    SurfaceView(context).apply {
                                        setZOrderOnTop(true)
                                        holder.setFormat(PixelFormat.TRANSPARENT)
                                        setBackgroundColor(0x44000000)
                                        callbacks.onVisualizerSurfaceReady?.invoke(this)
                                    }
                                },
                                update = { view ->
                                    callbacks.onVisualizerSurfaceReady?.invoke(view)
                                },
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else if (previewVisualizerBars) {
                            PreviewNierVisualizerOverlay(
                                barColor = MaterialTheme.colorScheme.primary,
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                // Track title + artist + right-aligned favorite toggle + Hi-Fi badge row
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.headlineSmallEmphasized,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(),
                            )
                            Text(
                                text = state.artist,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(),
                            )
                        }

                        if (state.isFavorite != null) {
                            FilledTonalIconButton(
                                onClick = {
                                    haptics.click()
                                    callbacks.onToggleFavorite()
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = if (state.isFavorite) {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                    contentColor = if (state.isFavorite) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                ),
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (state.isFavorite) {
                                            R.drawable.ic_heart_filled
                                        } else {
                                            R.drawable.ic_heart_unfilled
                                        }
                                    ),
                                    contentDescription = stringResource(
                                        if (state.isFavorite) {
                                            R.string.remove_from_favorites
                                        } else {
                                            R.string.add_to_favorites
                                        }
                                    ),
                                )
                            }
                        }
                    }

                    HiFiMetadataRow(
                        isHiFi = state.isHiFi,
                        isPlaying = state.isPlaying,
                        trackFormat = state.trackFormat,
                        sampleRateAndBitDepth = state.trackSampleRateAndBitDepth,
                        bitrateKbps = state.trackBitrateKbps,
                        onClick = {
                            haptics.click()
                            callbacks.onOpenStats()
                        },
                    )
                }

                // Wavy Progress Indicator + Buffering
                ExpressiveTrackProgressBar(
                    durationMs = state.durationMs,
                    currentPositionMs = state.currentPositionMs,
                    isPlaying = state.isPlaying,
                    isBuffering = state.isBuffering,
                    onSeekToPosition = callbacks.onSeekToPosition,
                )

                // Centered transport controls with morphing Play/Pause & haptics
                val playButtonShape = rememberMorphingShape(
                    from = TwelveMorphShapes.Circle,
                    to = TwelveMorphShapes.Square,
                    morphed = state.isPlaying,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            haptics.click()
                            callbacks.onToggleShuffle()
                        },
                        enabled = state.canShuffle,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_shuffle),
                            contentDescription = stringResource(R.string.shuffle_on),
                            tint = if (state.shuffleEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            haptics.click()
                            callbacks.onPrevious()
                        },
                        enabled = state.canPrevious,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_skip_previous),
                            contentDescription = null,
                        )
                    }

                    FilledIconButton(
                        onClick = {
                            haptics.click()
                            callbacks.onTogglePlayPause()
                        },
                        enabled = state.canPlayPause,
                        shape = playButtonShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.size(76.dp),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                            ),
                            contentDescription = stringResource(
                                R.string.content_description_play_pause
                            ),
                            modifier = Modifier.size(34.dp),
                        )
                    }

                    FilledTonalIconButton(
                        onClick = {
                            haptics.click()
                            callbacks.onNext()
                        },
                        enabled = state.canNext,
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_skip_next),
                            contentDescription = null,
                        )
                    }

                    IconButton(
                        onClick = {
                            haptics.click()
                            callbacks.onToggleRepeat()
                        },
                        enabled = state.canRepeat,
                    ) {
                        Icon(
                            painter = painterResource(
                                when (state.repeatMode) {
                                    RepeatMode.ONE -> R.drawable.ic_repeat_one
                                    else -> R.drawable.ic_repeat
                                }
                            ),
                            contentDescription = null,
                            tint = if (state.repeatMode != RepeatMode.NONE) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                // Centered utility dock + SplitButton actions
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                haptics.click()
                                callbacks.onOpenSpeedControl()
                            },
                            enabled = state.canChangeSpeed,
                        ) {
                            Text(
                                text = state.speedLabel,
                                style = MaterialTheme.typography.labelLargeEmphasized,
                            )
                        }

                        IconButton(
                            onClick = {
                                haptics.click()
                                callbacks.onOpenEqualizer()
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_audiofx),
                                contentDescription = stringResource(R.string.audio_effects),
                            )
                        }

                        val visualizerTint by animateColorAsState(
                            targetValue = if (state.isVisualizerEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            label = "VisualizerTint",
                        )

                        IconButton(
                            onClick = {
                                haptics.click()
                                callbacks.onCycleVisualizer()
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_graphic_eq),
                                contentDescription = stringResource(R.string.visualizer),
                                tint = visualizerTint,
                            )
                        }

                        IconButton(
                            onClick = {
                                haptics.click()
                                callbacks.onOpenQueue()
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_queue_play_next),
                                contentDescription = stringResource(R.string.queue),
                            )
                        }

                        IconButton(
                            onClick = {
                                haptics.click()
                                callbacks.onOpenLyrics()
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lyrics),
                                contentDescription = stringResource(R.string.lyrics),
                            )
                        }
                    }
                }

                TrackActionSplitButton(
                    onPrimaryClick = callbacks.onAddToPlaylist,
                    secondaryActions = listOf(
                        SplitButtonMenuAction(
                            label = stringResource(R.string.track_details),
                            iconRes = R.drawable.ic_info,
                            onClick = callbacks.onOpenTrackDetails,
                        ),
                        SplitButtonMenuAction(
                            label = stringResource(R.string.audio_add_to_queue),
                            iconRes = R.drawable.ic_playlist_add,
                            onClick = callbacks.onAddToQueue,
                        ),
                        SplitButtonMenuAction(
                            label = stringResource(R.string.audio_play_next),
                            iconRes = R.drawable.ic_playlist_play,
                            onClick = callbacks.onPlayNext,
                        ),
                    ),
                )

                // Live lyrics preview card
                AnimatedVisibility(visible = state.showLyricsCard) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .clickable {
                                haptics.click()
                                callbacks.onOpenLyrics()
                            },
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.lyrics),
                                    style = MaterialTheme.typography.labelLargeEmphasized,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Icon(
                                    painter = painterResource(R.drawable.ic_lyrics),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            state.previousLyric?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.7f
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            state.currentLyric?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyLargeEmphasized,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            state.nextLyric?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                        alpha = 0.7f
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dynamic ambient defocus backdrop inspired by: blurs the playing album artwork with
 * hardware RenderEffect on API 31+ (with a smooth tonal gradient fallback on API 30 / empty art)
 * and layers a soft vertical scrim for crisp contrast.
 */
@Composable
private fun DynamicArtworkBackground(
    artwork: Thumbnail?,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    val primaryAmbient = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    val tertiaryAmbient = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)

    Box(modifier = modifier) {
        if (artwork != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(64.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(primaryAmbient, tertiaryAmbient, surface),
                            radius = 1200f,
                        )
                    ),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            surface.copy(alpha = 0.52f),
                            surface.copy(alpha = 0.80f),
                            surface.copy(alpha = 0.96f),
                        )
                    )
                ),
        )
    }
}

@Composable
private fun OutputRoutePill(
    deviceType: OutputConfiguration.Device.Type?,
    hasIssues: Boolean,
    isDirect: Boolean,
    onClick: () -> Unit,
) {
    @DrawableRes
    val iconRes = when (deviceType) {
        OutputConfiguration.Device.Type.BUILTIN -> R.drawable.ic_mobile_speaker
        OutputConfiguration.Device.Type.HEADPHONES -> R.drawable.ic_headphones
        OutputConfiguration.Device.Type.EXTERNAL_SPEAKERS -> R.drawable.ic_speaker_group
        OutputConfiguration.Device.Type.BLUETOOTH -> R.drawable.ic_bluetooth
        OutputConfiguration.Device.Type.HDMI -> R.drawable.ic_settings_input_hdmi
        OutputConfiguration.Device.Type.USB -> R.drawable.ic_usb
        OutputConfiguration.Device.Type.REMOTE -> R.drawable.ic_cast
        OutputConfiguration.Device.Type.HEARING_AID -> R.drawable.ic_hearing_aid
        null -> R.drawable.ic_media_output
    }

    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (isDirect) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (isDirect) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(16.dp),
            )
            if (isDirect) {
                Text(
                    text = "DIRECT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (hasIssues) {
                Icon(
                    painter = painterResource(R.drawable.ic_error),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/**
 * Preview-only visual simulation of `Nier-Visualizer` columnar equalizer bars overlaid on the
 * album artwork.
 */
@Composable
private fun PreviewNierVisualizerOverlay(
    barColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val heights = listOf(
        0.28f, 0.52f, 0.78f, 0.44f, 0.88f, 0.65f, 0.35f, 0.72f,
        0.92f, 0.58f, 0.40f, 0.80f, 0.62f, 0.32f, 0.68f, 0.46f,
    )
    Canvas(
        modifier = modifier.background(Color(0x44000000)),
    ) {
        val count = heights.size
        val slotWidth = size.width / (count + 1)
        val barWidth = slotWidth * 0.56f
        val maxHeight = size.height * 0.38f
        val baseY = size.height - 20.dp.toPx()

        heights.forEachIndexed { index, ratio ->
            val barH = maxHeight * ratio
            val left = slotWidth * (index + 0.72f)
            val top = baseY - barH
            val color = if (index % 3 == 0) accentColor else barColor
            drawRoundRect(
                color = color.copy(alpha = 0.86f),
                topLeft = Offset(left, top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
