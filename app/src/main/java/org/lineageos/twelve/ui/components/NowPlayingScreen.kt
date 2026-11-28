/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.components

import android.graphics.PixelFormat
import android.view.SurfaceView
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import org.lineageos.twelve.R
import org.lineageos.twelve.models.OutputConfiguration
import org.lineageos.twelve.models.RepeatMode as PlayerRepeatMode
import org.lineageos.twelve.models.Thumbnail
import org.lineageos.twelve.ui.haptics.rememberTwelveHaptics
import org.lineageos.twelve.utils.TimestampFormatter
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Immutable
data class NowPlayingUiState(
    val title: String,
    val artist: String,
    val album: String,
    val genre: String? = null,
    val year: Int? = null,
    val artwork: Thumbnail? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val currentPositionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: PlayerRepeatMode = PlayerRepeatMode.NONE,
    val canShuffle: Boolean = true,
    val canPrevious: Boolean = true,
    val canNext: Boolean = true,
    val canPlayPause: Boolean = true,
    val canRepeat: Boolean = true,
    val canChangeSpeed: Boolean = true,
    val speedLabel: String = "1×",
    val isFavorite: Boolean? = null,
    val isVisualizerEnabled: Boolean = false,
    val isHiRes: Boolean = false,
    val isHiFi: Boolean = false,
    val isHq: Boolean = false,
    val trackFormat: String? = null,
    val trackSampleRateAndBitDepth: String? = null,
    val trackBitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val bitDepth: Int? = null,
    val channelCount: Int? = null,
    val fileSizeBytes: Long? = null,
    val fileDisplayName: String? = null,
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
 * Lune-inspired Material 3 Expressive Now Playing screen with an animated 5-blob cover-palette
 * gradient background, dynamic Play/Pause spring expansion + sliding skip buttons, rotating-thumb
 * `LinearWavyProgressIndicator`, M3E morphing option pills, connected Lyrics/Favorite split pill,
 * rich Audio Details sheet, and `Nier-Visualizer` encapsulation.
 */
@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
fun NowPlayingScreen(
    state: NowPlayingUiState,
    callbacks: NowPlayingCallbacks,
    modifier: Modifier = Modifier,
    previewVisualizerBars: Boolean = false,
) {
    val haptics = rememberTwelveHaptics()
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val coverPalette = rememberCoverPalette(state.artwork, isDarkTheme)
    var showAudioDetailsSheet by remember { mutableStateOf(false) }

    val dynamicAccent by animateColorAsState(
        targetValue = if (state.dynamicBackgroundEnabled) {
            coverPalette.primary
        } else {
            MaterialTheme.colorScheme.primary
        },
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "DynamicAccent",
    )
    val onDynamicAccent = if (dynamicAccent.luminance() > 0.45f) {
        Color(0xFF141316)
    } else {
        Color.White
    }

    val tonalPillBg = if (state.dynamicBackgroundEnabled) {
        if (isDarkTheme) Color.Black.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val secondaryControlBg = if (state.dynamicBackgroundEnabled) {
        dynamicAccent.copy(alpha = if (isDarkTheme) 0.24f else 0.16f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val secondaryControlTint = if (state.dynamicBackgroundEnabled) {
        if (isDarkTheme) Color.White else dynamicAccent
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        if (state.dynamicBackgroundEnabled) {
            PlayerGradientBackground(
                colors = coverPalette,
                isDarkTheme = isDarkTheme,
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
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        onClick = {
                            haptics.click()
                            callbacks.onNavigateUp()
                        },
                        shape = CircleShape,
                        color = tonalPillBg,
                        modifier = Modifier
                            .size(42.dp)
                            .bounceClick(),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(R.drawable.ic_keyboard_arrow_down),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.now_playing_label).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        Text(
                            text = state.album,
                            style = MaterialTheme.typography.titleSmallEmphasized,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.basicMarquee(),
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutputRoutePill(
                            deviceType = state.outputDeviceType,
                            hasIssues = state.hasPotentialIssues,
                            isDirect = state.isDirectOutputActive,
                            containerColor = tonalPillBg,
                            onClick = {
                                haptics.click()
                                callbacks.onOpenStats()
                            },
                        )

                        Surface(
                            onClick = {
                                haptics.click()
                                callbacks.onOpenTrackDetails()
                            },
                            shape = CircleShape,
                            color = tonalPillBg,
                            modifier = Modifier
                                .size(42.dp)
                                .bounceClick(),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert),
                                    contentDescription = stringResource(R.string.track_details),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
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
                // Hero Artwork with spring play/pause scale + swipe-to-skip gesture
                val coverScale by animateFloatAsState(
                    targetValue = if (state.isPlaying) 1f else 0.94f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                    label = "CoverScale",
                )

                Surface(
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .scale(coverScale)
                        .songSwipeGestures(
                            enabled = !state.isVisualizerEnabled,
                            onNext = {
                                haptics.click()
                                callbacks.onNext()
                            },
                            onPrevious = {
                                haptics.click()
                                callbacks.onPrevious()
                            },
                        ),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 18.dp,
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
                                                dynamicAccent.copy(alpha = 0.65f),
                                                coverPalette.tertiary.copy(alpha = 0.65f),
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
                                    tint = onDynamicAccent,
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
                                barColor = dynamicAccent,
                                accentColor = coverPalette.tertiary,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                // Lune Unified Header: Title + Artist pill + Metadata + Quality Badges + Connected Split Action Pill
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
                                .padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = state.title,
                                style = MaterialTheme.typography.headlineSmallEmphasized,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(),
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Surface(
                                    color = dynamicAccent.copy(alpha = if (isDarkTheme) 0.24f else 0.16f),
                                    shape = RoundedCornerShape(percent = 50),
                                ) {
                                    Text(
                                        text = state.artist,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                            .basicMarquee(),
                                    )
                                }

                                val metaText = listOfNotNull(
                                    state.year?.takeIf { it > 0 }?.toString(),
                                    state.genre?.takeIf { it.isNotBlank() },
                                ).takeIf { it.isNotEmpty() }?.joinToString(" • ")

                                if (metaText != null) {
                                    Surface(
                                        color = tonalPillBg,
                                        shape = RoundedCornerShape(percent = 50),
                                    ) {
                                        Text(
                                            text = metaText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = 4.dp,
                                            ),
                                        )
                                    }
                                }
                            }
                        }

                        // Connected M3 Expressive Split Pill: Lyrics + Favorite
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val lyricsBtnBg = if (state.showLyricsCard) {
                                dynamicAccent.copy(alpha = 0.28f)
                            } else {
                                tonalPillBg
                            }
                            Surface(
                                onClick = {
                                    haptics.click()
                                    callbacks.onOpenLyrics()
                                },
                                shape = RoundedCornerShape(
                                    topStart = 24.dp,
                                    bottomStart = 24.dp,
                                    topEnd = 4.dp,
                                    bottomEnd = 4.dp,
                                ),
                                color = lyricsBtnBg,
                                modifier = Modifier
                                    .size(48.dp)
                                    .bounceClick(),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_lyrics),
                                        contentDescription = stringResource(R.string.lyrics),
                                        tint = if (state.showLyricsCard) {
                                            dynamicAccent
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }

                            val isFav = state.isFavorite == true
                            Surface(
                                onClick = {
                                    haptics.click()
                                    callbacks.onToggleFavorite()
                                },
                                enabled = state.isFavorite != null,
                                shape = RoundedCornerShape(
                                    topStart = 4.dp,
                                    bottomStart = 4.dp,
                                    topEnd = 24.dp,
                                    bottomEnd = 24.dp,
                                ),
                                color = if (isFav) {
                                    dynamicAccent.copy(alpha = 0.28f)
                                } else {
                                    tonalPillBg
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .bounceClick(),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(
                                            if (isFav) {
                                                R.drawable.ic_heart_filled
                                            } else {
                                                R.drawable.ic_heart_unfilled
                                            }
                                        ),
                                        contentDescription = stringResource(
                                            if (isFav) {
                                                R.string.remove_from_favorites
                                            } else {
                                                R.string.add_to_favorites
                                            }
                                        ),
                                        tint = if (isFav) {
                                            dynamicAccent
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    }

                    // Audio Quality & Telemetry Pills (tappable to open AudioDetailsBottomSheet)
                    HiFiMetadataRow(
                        isHiRes = state.isHiRes,
                        isHiFi = state.isHiFi,
                        isHq = state.isHq,
                        trackFormat = state.trackFormat,
                        sampleRateAndBitDepth = state.trackSampleRateAndBitDepth,
                        bitrateKbps = state.trackBitrateKbps,
                        dynamicBackgroundEnabled = state.dynamicBackgroundEnabled,
                        customActiveColor = dynamicAccent,
                        onClick = {
                            haptics.click()
                            showAudioDetailsSheet = true
                        },
                    )
                }

                // Lune Expressive Progress Slider: LinearWavyProgressIndicator + Rotating Rounded-Square Thumb
                LuneExpressiveProgressSection(
                    durationMs = state.durationMs,
                    currentPositionMs = state.currentPositionMs,
                    isPlaying = state.isPlaying,
                    isBuffering = state.isBuffering,
                    accentColor = dynamicAccent,
                    pillBg = tonalPillBg,
                    onSeekToPosition = { posMs ->
                        haptics.click()
                        callbacks.onSeekToPosition(posMs)
                    },
                )

                // Lune Dynamic Spring Transport Row: Expanding Play/Pause + Sliding Previous/Next
                LuneDynamicTransportRow(
                    isPlaying = state.isPlaying,
                    canPrevious = state.canPrevious,
                    canPlayPause = state.canPlayPause,
                    canNext = state.canNext,
                    playBgColor = dynamicAccent,
                    playIconTint = onDynamicAccent,
                    skipBgColor = secondaryControlBg,
                    skipIconTint = secondaryControlTint,
                    onPrevious = {
                        haptics.click()
                        callbacks.onPrevious()
                    },
                    onTogglePlayPause = {
                        haptics.click()
                        callbacks.onTogglePlayPause()
                    },
                    onNext = {
                        haptics.click()
                        callbacks.onNext()
                    },
                )

                // Lune M3 Expressive Morphing Option Pills Row 1: Shuffle, Repeat, Queue
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerOptionPill(
                        iconRes = R.drawable.ic_shuffle,
                        label = stringResource(R.string.option_shuffle),
                        isActive = state.shuffleEnabled,
                        enabled = state.canShuffle,
                        activeAccent = dynamicAccent,
                        onActiveTint = onDynamicAccent,
                        inactiveBg = tonalPillBg,
                        onClick = {
                            haptics.click()
                            callbacks.onToggleShuffle()
                        },
                        modifier = Modifier.weight(1f),
                    )

                    PlayerOptionPill(
                        iconRes = when (state.repeatMode) {
                            PlayerRepeatMode.ONE -> R.drawable.ic_repeat_one
                            else -> R.drawable.ic_repeat
                        },
                        label = stringResource(R.string.option_repeat),
                        isActive = state.repeatMode != PlayerRepeatMode.NONE,
                        enabled = state.canRepeat,
                        activeAccent = dynamicAccent,
                        onActiveTint = onDynamicAccent,
                        inactiveBg = tonalPillBg,
                        onClick = {
                            haptics.click()
                            callbacks.onToggleRepeat()
                        },
                        modifier = Modifier.weight(1f),
                    )

                    PlayerOptionPill(
                        iconRes = R.drawable.ic_queue_play_next,
                        label = stringResource(R.string.option_queue),
                        isActive = false,
                        enabled = true,
                        activeAccent = dynamicAccent,
                        onActiveTint = onDynamicAccent,
                        inactiveBg = tonalPillBg,
                        onClick = {
                            haptics.click()
                            callbacks.onOpenQueue()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Utility Pills Row 2: Speed, EQ, Visualizer, Audio Info, Add to Playlist
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UtilityCompactPill(
                        text = state.speedLabel,
                        isActive = state.speedLabel != "1×",
                        enabled = state.canChangeSpeed,
                        activeAccent = dynamicAccent,
                        inactiveBg = tonalPillBg,
                        onClick = {
                            haptics.click()
                            callbacks.onOpenSpeedControl()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    UtilityCompactIconPill(
                        iconRes = R.drawable.ic_audiofx,
                        contentDescription = stringResource(R.string.audio_effects),
                        isActive = false,
                        activeAccent = dynamicAccent,
                        inactiveBg = tonalPillBg,
                        onClick = {
                            haptics.click()
                            callbacks.onOpenEqualizer()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    UtilityCompactIconPill(
                        iconRes = R.drawable.ic_graphic_eq,
                        contentDescription = stringResource(R.string.visualizer),
                        isActive = state.isVisualizerEnabled,
                        activeAccent = dynamicAccent,
                        inactiveBg = tonalPillBg,
                        onClick = {
                            haptics.click()
                            callbacks.onCycleVisualizer()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    UtilityCompactIconPill(
                        iconRes = R.drawable.ic_info,
                        contentDescription = stringResource(R.string.audio_details_title),
                        isActive = showAudioDetailsSheet,
                        activeAccent = dynamicAccent,
                        inactiveBg = tonalPillBg,
                        onClick = {
                            haptics.click()
                            showAudioDetailsSheet = true
                        },
                        modifier = Modifier.weight(1f),
                    )
                    UtilityCompactIconPill(
                        iconRes = R.drawable.ic_playlist_add,
                        contentDescription = stringResource(R.string.add_to_playlist),
                        isActive = false,
                        activeAccent = dynamicAccent,
                        inactiveBg = tonalPillBg,
                        onClick = {
                            haptics.click()
                            callbacks.onAddToPlaylist()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Live Lyrics Preview Card
                AnimatedVisibility(visible = state.showLyricsCard) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick(0.98f)
                            .clip(MaterialTheme.shapes.large)
                            .clickable {
                                haptics.click()
                                callbacks.onOpenLyrics()
                            },
                        shape = MaterialTheme.shapes.large,
                        color = tonalPillBg,
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
                                    color = dynamicAccent,
                                )
                                Icon(
                                    painter = painterResource(R.drawable.ic_lyrics),
                                    contentDescription = null,
                                    tint = dynamicAccent,
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

    if (showAudioDetailsSheet) {
        AudioDetailsBottomSheet(
            state = state,
            accentColor = dynamicAccent,
            onOpenOutputStats = callbacks.onOpenStats,
            onDismiss = { showAudioDetailsSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun LuneExpressiveProgressSection(
    durationMs: Long,
    currentPositionMs: Long,
    isPlaying: Boolean,
    isBuffering: Boolean,
    accentColor: Color,
    pillBg: Color,
    onSeekToPosition: (Long) -> Unit,
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val safeDuration = durationMs.coerceAtLeast(1L)
    val playbackFraction = (currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    val displayFraction = if (isDragging) dragFraction else playbackFraction
    val displayMs = if (isDragging) {
        (dragFraction * safeDuration).roundToLong()
    } else {
        currentPositionMs.coerceAtLeast(0L)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ThumbRotation")
    val thumbRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ThumbRotationAngle",
    )

    val sliderState = remember { SliderState(displayFraction) }
    LaunchedEffect(playbackFraction, isDragging) {
        if (!isDragging) {
            sliderState.value = playbackFraction
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        AnimatedVisibility(visible = isBuffering) {
            LinearWavyProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.24f),
            )
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            LinearWavyProgressIndicator(
                progress = { displayFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.24f),
                amplitude = { if (isPlaying && !isDragging) 1f else 0f },
            )

            Slider(
                state = sliderState,
                onValueChange = { fraction: Float ->
                    isDragging = true
                    val clamped = fraction.coerceIn(0f, 1f)
                    dragFraction = clamped
                    sliderState.value = clamped
                },
                onValueChangeFinished = {
                    isDragging = false
                    onSeekToPosition((dragFraction * safeDuration).roundToLong())
                },
                modifier = Modifier.fillMaxWidth(),
                thumb = { _: SliderState ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .graphicsLayer {
                                rotationZ = if (isPlaying) thumbRotation else 0f
                            }
                            .background(
                                color = accentColor,
                                shape = RoundedCornerShape(5.dp),
                            ),
                    )
                },
                track = { _: SliderState ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.dp),
                    )
                },
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                ),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = pillBg,
                shape = RoundedCornerShape(percent = 50),
            ) {
                Text(
                    text = TimestampFormatter.formatTimestampMillis(displayMs),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }

            Surface(
                color = pillBg,
                shape = RoundedCornerShape(percent = 50),
            ) {
                Text(
                    text = TimestampFormatter.formatTimestampMillis(durationMs.coerceAtLeast(0L)),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LuneDynamicTransportRow(
    isPlaying: Boolean,
    canPrevious: Boolean,
    canPlayPause: Boolean,
    canNext: Boolean,
    playBgColor: Color,
    playIconTint: Color,
    skipBgColor: Color,
    skipIconTint: Color,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    var showPlayStateLabel by remember { mutableStateOf(false) }
    var isFirstComposition by remember { mutableStateOf(true) }
    var labelTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(isPlaying, labelTrigger) {
        if (isFirstComposition) {
            isFirstComposition = false
            return@LaunchedEffect
        }
        showPlayStateLabel = true
        delay(1300L)
        showPlayStateLabel = false
    }

    val playButtonWidth by animateDpAsState(
        targetValue = if (showPlayStateLabel) 156.dp else 96.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "PlayButtonWidth",
    )

    val playCornerRadius by animateDpAsState(
        targetValue = if (isPlaying) 22.dp else 32.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "PlayCornerRadius",
    )

    val playRowHeight = 68.dp
    val skipButtonSize = 68.dp
    val skipGap = 14.dp
    val skipShape = RoundedCornerShape(26.dp)
    val playShape = RoundedCornerShape(playCornerRadius)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(playRowHeight),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onPrevious,
            enabled = canPrevious,
            shape = skipShape,
            color = skipBgColor,
            modifier = Modifier
                .size(skipButtonSize)
                .offset {
                    val halfPlay = playButtonWidth.toPx() / 2f
                    val gap = skipGap.toPx()
                    val halfSkip = skipButtonSize.toPx() / 2f
                    IntOffset(x = -(halfPlay + gap + halfSkip).roundToInt(), y = 0)
                }
                .bounceClick(),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_skip_previous),
                    contentDescription = null,
                    tint = skipIconTint,
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        Surface(
            onClick = {
                labelTrigger++
                onTogglePlayPause()
            },
            enabled = canPlayPause,
            shape = playShape,
            color = playBgColor,
            modifier = Modifier
                .height(playRowHeight)
                .width(playButtonWidth)
                .bounceClick(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                        ),
                        contentDescription = stringResource(R.string.content_description_play_pause),
                        tint = playIconTint,
                        modifier = Modifier.size(36.dp),
                    )

                    AnimatedVisibility(
                        visible = showPlayStateLabel,
                        enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                                expandHorizontally(
                                    expandFrom = Alignment.CenterHorizontally,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                ) +
                                scaleIn(
                                    initialScale = 0.8f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                    ),
                                ),
                        exit = fadeOut(tween(150)) +
                                shrinkHorizontally(
                                    shrinkTowards = Alignment.CenterHorizontally,
                                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                ),
                    ) {
                        Text(
                            text = stringResource(
                                if (isPlaying) R.string.play_state_play else R.string.play_state_pause
                            ),
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            fontWeight = FontWeight.Bold,
                            color = playIconTint,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.padding(start = 6.dp, end = 4.dp),
                        )
                    }
                }
            }
        }

        Surface(
            onClick = onNext,
            enabled = canNext,
            shape = skipShape,
            color = skipBgColor,
            modifier = Modifier
                .size(skipButtonSize)
                .offset {
                    val halfPlay = playButtonWidth.toPx() / 2f
                    val gap = skipGap.toPx()
                    val halfSkip = skipButtonSize.toPx() / 2f
                    IntOffset(x = (halfPlay + gap + halfSkip).roundToInt(), y = 0)
                }
                .bounceClick(),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_skip_next),
                    contentDescription = null,
                    tint = skipIconTint,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerOptionPill(
    @DrawableRes iconRes: Int,
    label: String,
    isActive: Boolean,
    enabled: Boolean,
    activeAccent: Color,
    onActiveTint: Color,
    inactiveBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconCornerPercent by animateIntAsState(
        targetValue = if (isActive) 28 else 50,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "OptionIconCornerMorph",
    )
    val pillCornerPercent by animateIntAsState(
        targetValue = if (isActive) 30 else 50,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "OptionPillCornerMorph",
    )

    val pillShape = RoundedCornerShape(percent = pillCornerPercent)
    val iconBadgeShape = RoundedCornerShape(percent = iconCornerPercent)

    val pillBg by animateColorAsState(
        targetValue = if (isActive) activeAccent.copy(alpha = 0.22f) else inactiveBg,
        animationSpec = tween(220),
        label = "OptionPillBg",
    )
    val iconBadgeBg by animateColorAsState(
        targetValue = if (isActive) {
            activeAccent
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        },
        animationSpec = tween(220),
        label = "OptionBadgeBg",
    )
    val iconTint by animateColorAsState(
        targetValue = if (isActive) onActiveTint else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "OptionIconTint",
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(220),
        label = "OptionTextColor",
    )

    Box(
        modifier = modifier
            .height(42.dp)
            .bounceClick(0.95f)
            .clip(pillShape)
            .background(pillBg, pillShape)
            .then(
                if (enabled) Modifier.combinedClickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(color = iconBadgeBg, shape = iconBadgeShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.5.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun UtilityCompactPill(
    text: String,
    isActive: Boolean,
    enabled: Boolean,
    activeAccent: Color,
    inactiveBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(percent = if (isActive) 32 else 50),
        color = if (isActive) activeAccent.copy(alpha = 0.22f) else inactiveBg,
        modifier = modifier
            .height(40.dp)
            .bounceClick(0.94f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isActive) activeAccent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun UtilityCompactIconPill(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    isActive: Boolean,
    activeAccent: Color,
    inactiveBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cornerPercent by animateIntAsState(
        targetValue = if (isActive) 30 else 50,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "UtilIconCorner",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(percent = cornerPercent),
        color = if (isActive) activeAccent.copy(alpha = 0.24f) else inactiveBg,
        modifier = modifier
            .height(40.dp)
            .bounceClick(0.94f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = if (isActive) activeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun OutputRoutePill(
    deviceType: OutputConfiguration.Device.Type?,
    hasIssues: Boolean,
    isDirect: Boolean,
    containerColor: Color,
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
        onClick = onClick,
        modifier = Modifier.bounceClick(),
        shape = CircleShape,
        color = if (isDirect) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            containerColor
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
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
                    fontWeight = FontWeight.Bold,
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
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
            .padding(horizontal = 16.dp, vertical = 20.dp),
    ) {
        val barCount = heights.size
        val slotWidth = size.width / barCount
        val barWidth = slotWidth * 0.58f
        val maxHeight = size.height * 0.65f
        val brush = Brush.verticalGradient(listOf(accentColor, barColor))

        heights.forEachIndexed { index, ratio ->
            val h = maxHeight * ratio
            val x = index * slotWidth + (slotWidth - barWidth) / 2f
            val y = size.height - h
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
