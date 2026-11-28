/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaMetadata
import coil3.compose.AsyncImage
import com.google.android.material.card.MaterialCardView
import org.lineageos.twelve.R
import org.lineageos.twelve.models.Thumbnail
import org.lineageos.twelve.ui.components.ExpressiveCircularWavyProgress
import org.lineageos.twelve.ui.components.ListHiFiBadge
import org.lineageos.twelve.ui.components.ListHiResBadge
import org.lineageos.twelve.ui.components.bounceClick
import org.lineageos.twelve.ui.components.rememberCoverPalette
import org.lineageos.twelve.ui.haptics.rememberTwelveHaptics
import org.lineageos.twelve.ui.theme.TwelveExpressiveTheme
import org.lineageos.twelve.ui.theme.TwelveMorphShapes
import org.lineageos.twelve.ui.theme.rememberMorphingShape
import org.lineageos.twelve.utils.TrackAudioSpecs

/**
 * Lune-inspired floating Material 3 Expressive mini-player pill rendered via [ComposeView],
 * dynamically tinted by the playing track's artwork palette, with Hi-Res / HI-FI quality badges,
 * clickable navigation to full-screen Now Playing, and morphing wavy circular progress.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class NowPlayingBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle,
) : MaterialCardView(context, attrs, defStyleAttr) {
    private var title by mutableStateOf("")
    private var artist by mutableStateOf("")
    private var artwork by mutableStateOf<Thumbnail?>(null)
    private var isPlayingState by mutableStateOf(false)
    private var progressFraction by mutableFloatStateOf(0f)
    private var isHiRes by mutableStateOf(false)
    private var isHiFi by mutableStateOf(false)
    private var formatLabel by mutableStateOf<String?>(null)
    private var playPauseClickListener: OnClickListener? = null

    private val composeView = ComposeView(context)

    init {
        cardElevation = 0f
        radius = 0f
        strokeWidth = 0
        setCardBackgroundColor(android.graphics.Color.TRANSPARENT)

        addView(
            composeView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )

        composeView.setContent {
            TwelveExpressiveTheme {
                val haptics = rememberTwelveHaptics()
                val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
                val coverPalette = rememberCoverPalette(artwork, isDarkTheme)
                val accentColor by animateColorAsState(
                    targetValue = if (artwork != null) {
                        coverPalette.primary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    animationSpec = tween(500),
                    label = "MiniPlayerAccent",
                )
                val onAccentColor = if (accentColor.luminance() > 0.45f) {
                    Color(0xFF141316)
                } else {
                    Color.White
                }

                val playShape = rememberMorphingShape(
                    from = TwelveMorphShapes.Circle,
                    to = TwelveMorphShapes.Square,
                    morphed = isPlayingState,
                )

                val cardBg = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f)
                val cardShape = RoundedCornerShape(22.dp)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .bounceClick(scaleDown = 0.98f)
                        .clip(cardShape)
                        .clickable {
                            haptics.click()
                            this@NowPlayingBar.performClick()
                        },
                    shape = cardShape,
                    color = cardBg,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
                    shadowElevation = 6.dp,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accentColor.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (artwork != null) {
                                AsyncImage(
                                    model = artwork,
                                    contentDescription = stringResource(
                                        R.string.content_description_thumbnail
                                    ),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(R.drawable.ic_music_note),
                                    error = painterResource(R.drawable.ic_music_note),
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.ic_music_note),
                                    contentDescription = stringResource(
                                        R.string.content_description_thumbnail
                                    ),
                                    tint = accentColor,
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isHiRes) {
                                    ListHiResBadge()
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else if (isHiFi) {
                                    ListHiFiBadge()
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                Text(
                                    text = title.ifEmpty { stringResource(R.string.unknown) },
                                    style = MaterialTheme.typography.bodyLargeEmphasized,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.basicMarquee(),
                                )
                            }

                            val subtitle = buildString {
                                append(artist.ifEmpty { stringResource(R.string.artist_unknown) })
                                formatLabel?.takeIf { it.isNotBlank() }?.let { fmt ->
                                    append(" • ")
                                    append(fmt)
                                }
                            }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Box(
                            modifier = Modifier.size(54.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            ExpressiveCircularWavyProgress(
                                progress = progressFraction,
                                isPlaying = isPlayingState,
                                size = 52.dp,
                                color = accentColor,
                                trackColor = accentColor.copy(alpha = 0.22f),
                            )

                            FilledIconButton(
                                onClick = {
                                    haptics.click()
                                    playPauseClickListener?.onClick(this@NowPlayingBar)
                                },
                                shape = playShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = accentColor,
                                    contentColor = onAccentColor,
                                ),
                                modifier = Modifier
                                    .size(38.dp)
                                    .bounceClick(0.90f),
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isPlayingState) {
                                            R.drawable.ic_pause
                                        } else {
                                            R.drawable.ic_play_arrow
                                        }
                                    ),
                                    contentDescription = stringResource(
                                        R.string.content_description_play_pause
                                    ),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun setOnPlayPauseClickListener(l: OnClickListener?) {
        playPauseClickListener = l
    }

    fun updateIsPlaying(isPlaying: Boolean) {
        isPlayingState = isPlaying
    }

    fun updateMediaMetadata(mediaMetadata: MediaMetadata) {
        title = (mediaMetadata.displayTitle ?: mediaMetadata.title)?.toString()
            ?: context.getString(R.string.unknown)
        artist = mediaMetadata.artist?.toString()
            ?: context.getString(R.string.artist_unknown)
    }

    fun updateMediaArtwork(artwork: Thumbnail?) {
        this.artwork = artwork
    }

    fun updateDurationCurrentPositionMs(durationMs: Long?, currentPositionMs: Long?) {
        val duration = durationMs?.takeIf { it > 0L } ?: 1L
        val current = currentPositionMs?.coerceAtLeast(0L) ?: 0L
        progressFraction = (current.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    fun updateAudioSpecs(specs: TrackAudioSpecs) {
        isHiRes = specs.isHiRes
        isHiFi = specs.isHiFi
        formatLabel = specs.format
    }
}
