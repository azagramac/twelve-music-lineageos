/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve

import android.content.Intent
import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.util.Consumer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.lineageos.twelve.ext.dynamicBackground
import org.lineageos.twelve.ext.dynamicBackgroundFlow
import org.lineageos.twelve.models.FlowResult
import org.lineageos.twelve.models.MediaType
import org.lineageos.twelve.ui.components.NowPlayingCallbacks
import org.lineageos.twelve.ui.components.NowPlayingScreen
import org.lineageos.twelve.ui.components.NowPlayingUiState
import org.lineageos.twelve.ui.theme.TwelveExpressiveTheme
import org.lineageos.twelve.utils.AudioMetadataExtractor
import org.lineageos.twelve.utils.TrackAudioSpecs
import org.lineageos.twelve.viewmodels.IntentsViewModel
import org.lineageos.twelve.viewmodels.LocalPlayerViewModel
import java.util.Locale

/**
 * External VIEW-intent player activity rendered in Compose with the full Lune-inspired
 * Material 3 Expressive [NowPlayingScreen].
 */
class ViewActivity : AppCompatActivity() {
    private val intentsViewModel by viewModels<IntentsViewModel>()
    private val localPlayerViewModel by viewModels<LocalPlayerViewModel>()

    private var activeContentUri by mutableStateOf<Uri?>(null)
    private val intentListener = Consumer<Intent> { intentsViewModel.onIntent(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        intentsViewModel.onIntent(intent)
        addOnNewIntentListener(intentListener)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                intentsViewModel.parsedIntent.collectLatest { parsedIntent ->
                    parsedIntent?.handle {
                        if (it.action != IntentsViewModel.ParsedIntent.Action.VIEW) {
                            Log.e(LOG_TAG, "Cannot handle action ${it.action}")
                            finish()
                            return@handle
                        }

                        if (it.contents.isEmpty()) {
                            Log.e(LOG_TAG, "No content to play")
                            finish()
                            return@handle
                        }

                        val contentType = it.contents.first().type
                        if (contentType != MediaType.AUDIO) {
                            Log.e(LOG_TAG, "Cannot handle content type $contentType")
                            finish()
                            return@handle
                        }

                        if (it.contents.any { content -> content.type != contentType }) {
                            Log.e(LOG_TAG, "All contents must have the same type")
                            finish()
                            return@handle
                        }

                        val uris = it.contents.map { content -> content.uri }
                        activeContentUri = uris.firstOrNull()
                        localPlayerViewModel.setMediaUris(uris)
                    }
                }
            }
        }

        setContent {
            TwelveExpressiveTheme {
                val mediaMetadata by localPlayerViewModel.mediaMetadata.collectAsState()
                val mediaArtwork by localPlayerViewModel.mediaArtwork.collectAsState()
                val isPlaying by localPlayerViewModel.isPlaying.collectAsState()
                val durationCurrentPositionMs by
                    localPlayerViewModel.durationCurrentPositionMs.collectAsState()
                val shuffleMode by localPlayerViewModel.shuffleMode.collectAsState()
                val repeatMode by localPlayerViewModel.repeatMode.collectAsState()
                val availableCommands by localPlayerViewModel.availableCommands.collectAsState()
                val playbackParameters by localPlayerViewModel.playbackParameters.collectAsState()

                val prefs = remember {
                    PreferenceManager.getDefaultSharedPreferences(applicationContext)
                }
                val dynamicBackgroundEnabled by prefs.dynamicBackgroundFlow.collectAsState(
                    initial = prefs.dynamicBackground,
                )

                var specs by remember { mutableStateOf(TrackAudioSpecs()) }
                val durationMs = durationCurrentPositionMs.first ?: 0L

                LaunchedEffect(activeContentUri, durationMs) {
                    val uri = activeContentUri ?: return@LaunchedEffect
                    specs = AudioMetadataExtractor.extract(
                        context = applicationContext,
                        uri = uri,
                        mimeType = null,
                        durationMs = durationMs.takeIf { it > 0L },
                        deepExtract = true,
                    )
                }

                val artworkThumbnail = (mediaArtwork as? FlowResult.Success)?.data
                val state = NowPlayingUiState(
                    title = (mediaMetadata.displayTitle ?: mediaMetadata.title)?.toString()
                        ?: getString(R.string.unknown),
                    artist = mediaMetadata.artist?.toString()
                        ?: getString(R.string.artist_unknown),
                    album = mediaMetadata.albumTitle?.toString()
                        ?: getString(R.string.album_unknown),
                    genre = mediaMetadata.genre?.toString(),
                    year = mediaMetadata.releaseYear,
                    artwork = artworkThumbnail,
                    isPlaying = isPlaying,
                    durationMs = durationMs,
                    currentPositionMs = durationCurrentPositionMs.second ?: 0L,
                    shuffleEnabled = shuffleMode,
                    repeatMode = repeatMode,
                    canShuffle = availableCommands.contains(Player.COMMAND_SET_SHUFFLE_MODE),
                    canPrevious = availableCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS),
                    canPlayPause = availableCommands.contains(Player.COMMAND_PLAY_PAUSE),
                    canNext = availableCommands.contains(Player.COMMAND_SEEK_TO_NEXT),
                    canRepeat = availableCommands.contains(Player.COMMAND_SET_REPEAT_MODE),
                    canChangeSpeed =
                        availableCommands.contains(Player.COMMAND_SET_SPEED_AND_PITCH),
                    speedLabel = getString(
                        R.string.playback_speed_format,
                        playbackSpeedFormatter.format(playbackParameters.speed),
                    ),
                    isVisualizerEnabled = false,
                    isHiRes = specs.isHiRes,
                    isHiFi = specs.isHiFi,
                    isHq = specs.isHq,
                    trackFormat = specs.format,
                    trackSampleRateAndBitDepth = specs.sampleRateAndBitDepth,
                    trackBitrateKbps = specs.bitrateKbps,
                    sampleRateHz = specs.sampleRateHz,
                    bitDepth = specs.bitDepth,
                    channelCount = specs.channelCount,
                    fileSizeBytes = specs.fileSizeBytes,
                    fileDisplayName = specs.displayName,
                    dynamicBackgroundEnabled = dynamicBackgroundEnabled,
                )

                val callbacks = NowPlayingCallbacks(
                    onNavigateUp = { finish() },
                    onSeekToPosition = localPlayerViewModel::seekToPosition,
                    onToggleShuffle = localPlayerViewModel::toggleShuffleMode,
                    onPrevious = localPlayerViewModel::seekToPrevious,
                    onTogglePlayPause = localPlayerViewModel::togglePlayPause,
                    onNext = localPlayerViewModel::seekToNext,
                    onToggleRepeat = localPlayerViewModel::toggleRepeatMode,
                    onOpenSpeedControl = localPlayerViewModel::shufflePlaybackSpeed,
                )

                NowPlayingScreen(
                    state = state,
                    callbacks = callbacks,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    override fun onDestroy() {
        removeOnNewIntentListener(intentListener)

        super.onDestroy()
    }

    companion object {
        private val LOG_TAG = ViewActivity::class.simpleName!!

        private val decimalFormatSymbols = DecimalFormatSymbols(Locale.ROOT)

        private val playbackSpeedFormatter = DecimalFormat("0.#", decimalFormatSymbols)
    }
}
