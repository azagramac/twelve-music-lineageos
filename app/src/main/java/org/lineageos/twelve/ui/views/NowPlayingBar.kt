/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.views

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaMetadata
import coil3.compose.AsyncImage
import com.google.android.material.card.MaterialCardView
import org.lineageos.twelve.R
import org.lineageos.twelve.models.Thumbnail
import org.lineageos.twelve.ui.components.ExpressiveCircularWavyProgress
import org.lineageos.twelve.ui.haptics.rememberTwelveHaptics
import org.lineageos.twelve.ui.theme.TwelveExpressiveTheme
import org.lineageos.twelve.ui.theme.TwelveMorphShapes
import org.lineageos.twelve.ui.theme.rememberMorphingShape

/**
 * Persistent mini-player bar rendered via [ComposeView] with M3 Expressive
 * [androidx.compose.material3.CircularWavyProgressIndicator] and springy haptics.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class NowPlayingBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = com.google.android.material.R.attr.materialCardViewStyle,
) : MaterialCardView(context, attrs, defStyleAttr) {
    private var title by mutableStateOf("")
    private var artist by mutableStateOf("")
    private var album by mutableStateOf("")
    private var artwork by mutableStateOf<Thumbnail?>(null)
    private var isPlayingState by mutableStateOf(false)
    private var progressFraction by mutableFloatStateOf(0f)
    private var playPauseClickListener: OnClickListener? = null

    private val composeView = ComposeView(context)

    init {
        cardElevation = 0f
        radius = 0f
        strokeWidth = 0

        addView(
            composeView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
        )

        composeView.setContent {
            TwelveExpressiveTheme {
                val haptics = rememberTwelveHaptics()
                val playShape = rememberMorphingShape(
                    from = TwelveMorphShapes.Circle,
                    to = TwelveMorphShapes.Square,
                    morphed = isPlayingState,
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
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
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = title.ifEmpty { stringResource(R.string.unknown) },
                                style = MaterialTheme.typography.bodyLargeEmphasized,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = artist.ifEmpty { stringResource(R.string.artist_unknown) },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (album.isNotEmpty()) {
                                Text(
                                    text = album,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Box(
                            modifier = Modifier.size(56.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            ExpressiveCircularWavyProgress(
                                progress = progressFraction,
                                isPlaying = isPlayingState,
                                size = 54.dp,
                            )

                            FilledIconButton(
                                onClick = {
                                    haptics.click()
                                    playPauseClickListener?.onClick(this@NowPlayingBar)
                                },
                                shape = playShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                                modifier = Modifier.size(40.dp),
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
        album = mediaMetadata.albumTitle?.toString()
            ?: context.getString(R.string.album_unknown)
    }

    fun updateMediaArtwork(artwork: Thumbnail?) {
        this.artwork = artwork
    }

    fun updateDurationCurrentPositionMs(durationMs: Long?, currentPositionMs: Long?) {
        val duration = durationMs?.takeIf { it > 0L } ?: 1L
        val current = currentPositionMs?.coerceAtLeast(0L) ?: 0L
        progressFraction = (current.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }
}
