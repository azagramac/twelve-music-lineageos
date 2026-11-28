/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.fragments

import android.content.Intent
import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.media.audiofx.AudioEffect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.Player
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.bogerchan.niervisualizer.NierVisualizerManager
import org.lineageos.twelve.R
import org.lineageos.twelve.ext.dynamicBackground
import org.lineageos.twelve.ext.dynamicBackgroundFlow
import org.lineageos.twelve.ext.navigateSafe
import org.lineageos.twelve.models.FlowResult
import org.lineageos.twelve.models.OutputConfiguration
import org.lineageos.twelve.models.PlaybackState
import org.lineageos.twelve.models.Result
import org.lineageos.twelve.ui.components.NowPlayingCallbacks
import org.lineageos.twelve.ui.components.NowPlayingScreen
import org.lineageos.twelve.ui.components.NowPlayingUiState
import org.lineageos.twelve.ui.theme.setTwelveExpressiveContent
import org.lineageos.twelve.ui.visualizer.VisualizerNVDataSource
import org.lineageos.twelve.utils.PermissionsChecker
import org.lineageos.twelve.utils.PermissionsUtils
import org.lineageos.twelve.viewmodels.NowPlayingViewModel
import java.util.Locale

/**
 * Now playing fragment rendered via [ComposeView] with Material 3 Expressive and native
 * `Nier-Visualizer` [SurfaceView] encapsulation.
 */
class NowPlayingFragment : Fragment() {
    // View models
    private val viewModel by viewModels<NowPlayingViewModel>()

    // AudioFX
    private val audioEffectsStartForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Empty
        }

    // Visualizer
    private val visualizerManager = NierVisualizerManager()
    private val visualizerNVDataSource by lazy { VisualizerNVDataSource() }
    private var visualizerSurfaceView: SurfaceView? = null
    private var isVisualizerStarted = false

    // Permissions
    private val visualizerPermissionsChecker = PermissionsChecker(
        this,
        PermissionsUtils.visualizerPermissions,
        true,
        R.string.visualizer_permissions_toast,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        visualizerManager.init(visualizerNVDataSource)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setTwelveExpressiveContent {
            val mediaMetadata by viewModel.mediaMetadata.collectAsState()
            val mediaArtwork by viewModel.mediaArtwork.collectAsState()
            val isPlaying by viewModel.isPlaying.collectAsState()
            val playbackState by viewModel.playbackState.collectAsState()
            val durationCurrentPositionMs by viewModel.durationCurrentPositionMs.collectAsState()
            val shuffleMode by viewModel.shuffleMode.collectAsState()
            val repeatMode by viewModel.repeatMode.collectAsState()
            val availableCommands by viewModel.availableCommands.collectAsState()
            val playbackParameters by viewModel.playbackParameters.collectAsState()
            val audioResult by viewModel.audio.collectAsState()
            val isVisualizerEnabled by viewModel.isVisualizerEnabled.collectAsState()
            val isHiRes by viewModel.isHiRes.collectAsState()
            val isHiFi by viewModel.isHiFi.collectAsState()
            val isHq by viewModel.isHq.collectAsState()
            val trackFormat by viewModel.trackFormat.collectAsState()
            val trackSampleRateAndBitDepth by viewModel.trackSampleRateAndBitDepth.collectAsState()
            val trackBitrateKbps by viewModel.trackBitrateKbps.collectAsState()
            val outputConfiguration by viewModel.outputConfiguration.collectAsState()
            val lyricsResult by viewModel.lyricsLines.collectAsState()

            val prefs = remember {
                PreferenceManager.getDefaultSharedPreferences(requireContext().applicationContext)
            }
            val dynamicBackgroundEnabled by prefs.dynamicBackgroundFlow.collectAsState(
                initial = prefs.dynamicBackground
            )

            val currentAudio = (audioResult as? FlowResult.Success)?.data
            val artworkThumbnail = (mediaArtwork as? Result.Success)?.data
            val lyricsPair = (lyricsResult as? FlowResult.Success)?.data
            val lyricsIndex = lyricsPair?.second ?: 0
            val lyricsList = lyricsPair?.first

            val state = NowPlayingUiState(
                title = (mediaMetadata.displayTitle ?: mediaMetadata.title)?.toString()
                    ?: getString(R.string.unknown),
                artist = mediaMetadata.artist?.toString()
                    ?: getString(R.string.artist_unknown),
                album = mediaMetadata.albumTitle?.toString()
                    ?: getString(R.string.album_unknown),
                artwork = artworkThumbnail,
                isPlaying = isPlaying,
                isBuffering = playbackState == PlaybackState.BUFFERING,
                durationMs = durationCurrentPositionMs.first ?: 0L,
                currentPositionMs = durationCurrentPositionMs.second ?: 0L,
                shuffleEnabled = shuffleMode,
                repeatMode = repeatMode,
                canShuffle = availableCommands.contains(Player.COMMAND_SET_SHUFFLE_MODE),
                canPrevious = availableCommands.contains(Player.COMMAND_SEEK_TO_PREVIOUS),
                canPlayPause = availableCommands.contains(Player.COMMAND_PLAY_PAUSE),
                canNext = availableCommands.contains(Player.COMMAND_SEEK_TO_NEXT),
                canRepeat = availableCommands.contains(Player.COMMAND_SET_REPEAT_MODE),
                canChangeSpeed = availableCommands.contains(Player.COMMAND_SET_SPEED_AND_PITCH),
                speedLabel = getString(
                    R.string.playback_speed_format,
                    playbackSpeedFormatter.format(playbackParameters.speed),
                ),
                isFavorite = currentAudio?.isFavorite,
                isVisualizerEnabled = isVisualizerEnabled,
                isHiRes = isHiRes,
                isHiFi = isHiFi,
                isHq = isHq,
                trackFormat = trackFormat,
                trackSampleRateAndBitDepth = trackSampleRateAndBitDepth,
                trackBitrateKbps = trackBitrateKbps,
                outputDeviceType = outputConfiguration?.device?.type,
                hasPotentialIssues =
                    outputConfiguration?.verdict?.potentialIssues?.isNotEmpty() == true,
                isDirectOutputActive = outputConfiguration?.transcoding?.outputMode ==
                    OutputConfiguration.Transcoding.OutputMode.DIRECT,
                dynamicBackgroundEnabled = dynamicBackgroundEnabled,
                previousLyric = lyricsList?.getOrNull(lyricsIndex - 1)?.first?.text,
                currentLyric = lyricsList?.getOrNull(lyricsIndex)?.first?.text,
                nextLyric = lyricsList?.getOrNull(lyricsIndex + 1)?.first?.text,
                showLyricsCard = lyricsResult is FlowResult.Success,
            )

            val callbacks = NowPlayingCallbacks(
                onNavigateUp = { findNavController().navigateUp() },
                onOpenStats = {
                    findNavController().navigateSafe(
                        R.id.action_nowPlayingFragment_to_fragment_now_playing_stats_dialog
                    )
                },
                onSeekToPosition = viewModel::seekToPosition,
                onToggleShuffle = viewModel::toggleShuffleMode,
                onPrevious = viewModel::seekToPrevious,
                onTogglePlayPause = viewModel::togglePlayPause,
                onNext = viewModel::seekToNext,
                onToggleRepeat = viewModel::toggleRepeatMode,
                onAddToPlaylist = {
                    currentAudio?.let { audio ->
                        findNavController().navigateSafe(
                            R.id.action_nowPlayingFragment_to_fragment_add_or_remove_from_playlists,
                            AddOrRemoveFromPlaylistsFragment.createBundle(audio.uri),
                        )
                    }
                },
                onOpenTrackDetails = {
                    currentAudio?.let { audio ->
                        findNavController().navigateSafe(
                            R.id.action_nowPlayingFragment_to_fragment_media_item_bottom_sheet_dialog,
                            MediaItemBottomSheetDialogFragment.createBundle(
                                audio.uri,
                                fromNowPlaying = true,
                            ),
                        )
                    }
                },
                onAddToQueue = {
                    currentAudio?.let { audio -> viewModel.addToQueue(listOf(audio)) }
                },
                onPlayNext = {
                    currentAudio?.let { audio -> viewModel.playNext(listOf(audio)) }
                },
                onOpenSpeedControl = {
                    findNavController().navigateSafe(
                        R.id.action_nowPlayingFragment_to_fragment_playback_control_bottom_sheet_dialog
                    )
                },
                onOpenEqualizer = {
                    viewModel.audioSessionId.value?.let { audioSessionId ->
                        audioEffectsStartForResult.launch(
                            Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                                putExtra(
                                    AudioEffect.EXTRA_PACKAGE_NAME,
                                    requireContext().packageName,
                                )
                                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId)
                                putExtra(
                                    AudioEffect.EXTRA_CONTENT_TYPE,
                                    AudioEffect.CONTENT_TYPE_MUSIC,
                                )
                            },
                            null,
                        )
                    }
                },
                onCycleVisualizer = viewModel::nextVisualizerType,
                onOpenQueue = {
                    findNavController().navigateSafe(
                        R.id.action_nowPlayingFragment_to_fragment_queue
                    )
                },
                onToggleFavorite = {
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.toggleFavorites()
                    }
                },
                onOpenLyrics = {
                    findNavController().navigateSafe(
                        R.id.action_nowPlayingFragment_to_fragment_lyrics
                    )
                },
                onVisualizerSurfaceReady = { surfaceView ->
                    if (visualizerSurfaceView !== surfaceView) {
                        visualizerSurfaceView = surfaceView
                        val renderers = viewModel.currentVisualizerType.value.factory.invoke()
                        if (renderers != null) {
                            visualizerManager.start(surfaceView, renderers)
                            isVisualizerStarted = true
                        }
                    }
                },
            )

            NowPlayingScreen(
                state = state,
                callbacks = callbacks,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.audioSessionId.collectLatest {
                        visualizerNVDataSource.setAudioSessionId(it)
                    }
                }

                launch {
                    viewModel.isVisualizerEnabled.collectLatest { isVisualizerEnabled ->
                        if (!isVisualizerEnabled) {
                            if (isVisualizerStarted) {
                                visualizerManager.stop()
                                isVisualizerStarted = false
                            }
                            return@collectLatest
                        }

                        visualizerPermissionsChecker.withPermissionsGranted {
                            launch {
                                visualizerNVDataSource.workFlow.collect()
                            }

                            launch {
                                viewModel.currentVisualizerType.collectLatest { type ->
                                    val surfaceView = visualizerSurfaceView
                                    val renderers = type.factory.invoke()
                                    if (surfaceView != null && renderers != null) {
                                        visualizerManager.start(surfaceView, renderers)
                                        isVisualizerStarted = true
                                    } else if (isVisualizerStarted) {
                                        visualizerManager.stop()
                                        isVisualizerStarted = false
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isVisualizerStarted) {
            visualizerManager.resume()
        }
    }

    override fun onPause() {
        if (isVisualizerStarted) {
            visualizerManager.pause()
        }

        super.onPause()
    }

    override fun onDestroyView() {
        if (isVisualizerStarted) {
            visualizerManager.stop()
        }
        isVisualizerStarted = false
        visualizerSurfaceView = null

        super.onDestroyView()
    }

    override fun onDestroy() {
        visualizerManager.release()

        super.onDestroy()
    }

    companion object {
        private val decimalFormatSymbols = DecimalFormatSymbols(Locale.ROOT)
        private val playbackSpeedFormatter = DecimalFormat("0.#", decimalFormatSymbols)
    }
}
