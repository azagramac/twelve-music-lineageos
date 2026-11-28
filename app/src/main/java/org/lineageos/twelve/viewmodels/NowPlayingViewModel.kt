/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.viewmodels

import android.app.Application
import android.os.Bundle
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import me.bogerchan.niervisualizer.renderer.IRenderer
import me.bogerchan.niervisualizer.renderer.circle.CircleBarRenderer
import me.bogerchan.niervisualizer.renderer.circle.CircleRenderer
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType1Renderer
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType2Renderer
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType3Renderer
import me.bogerchan.niervisualizer.renderer.columnar.ColumnarType4Renderer
import me.bogerchan.niervisualizer.renderer.line.LineRenderer
import org.lineageos.twelve.ext.applicationContext
import org.lineageos.twelve.ext.availableCommandsFlow
import org.lineageos.twelve.ext.isPlayingFlow
import org.lineageos.twelve.ext.mediaItemFlow
import org.lineageos.twelve.ext.mediaMetadataFlow
import org.lineageos.twelve.ext.next
import org.lineageos.twelve.ext.playbackParametersFlow
import org.lineageos.twelve.ext.playbackStateFlow
import org.lineageos.twelve.ext.repeatModeFlow
import org.lineageos.twelve.ext.shuffleModeFlow
import org.lineageos.twelve.ext.toThumbnail
import org.lineageos.twelve.ext.tracksFlow
import org.lineageos.twelve.models.Error
import org.lineageos.twelve.models.FlowResult
import org.lineageos.twelve.models.FlowResult.Companion.asFlowResult
import org.lineageos.twelve.models.FlowResult.Companion.flatMapLatestData
import org.lineageos.twelve.models.FlowResult.Companion.getOrNull
import org.lineageos.twelve.models.OutputConfiguration
import org.lineageos.twelve.models.PlaybackState
import org.lineageos.twelve.models.RepeatMode
import org.lineageos.twelve.models.Result
import org.lineageos.twelve.services.PlaybackService
import org.lineageos.twelve.services.PlaybackService.CustomCommand.Companion.sendCustomCommand
import org.lineageos.twelve.utils.AudioMetadataExtractor
import org.lineageos.twelve.utils.MimeUtils
import org.lineageos.twelve.utils.OutputConfigurationUtils
import org.lineageos.twelve.utils.OutputConfigurationUtils.toModel
import org.lineageos.twelve.utils.TrackAudioSpecs

@androidx.annotation.OptIn(UnstableApi::class)
open class NowPlayingViewModel(application: Application) : TwelveViewModel(application) {
    enum class VisualizerType(val factory: () -> Array<IRenderer>?) {
        NONE({ null }),
        TYPE_1({ arrayOf(ColumnarType1Renderer()) }),
        TYPE_2({ arrayOf(ColumnarType2Renderer()) }),
        TYPE_3({ arrayOf(ColumnarType3Renderer()) }),
        TYPE_4({ arrayOf(ColumnarType4Renderer()) }),
        LINE({ arrayOf(LineRenderer(true)) }),
        CIRCLE_BAR({ arrayOf(CircleBarRenderer()) }),
        CIRCLE({ arrayOf(CircleRenderer(true)) }),
    }

    enum class LyricsLineState {
        /**
         * Line is still to be reached.
         */
        PENDING,

        /**
         * Line is relevant with current timestamp.
         */
        ACTIVE,

        /**
         * Line is past the current timestamp.
         */
        PAST,

        /**
         * Lyrics' position is unknown.
         */
        UNKNOWN,
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaMetadata = mediaControllerFlow
        .flatMapLatest { it.mediaMetadataFlow(eventsFlow) }
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = MediaMetadata.EMPTY
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val mediaItem = mediaControllerFlow
        .flatMapLatest { it.mediaItemFlow(eventsFlow) }
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val mediaItemUri = mediaItem
        .mapLatest { mediaItem ->
            mediaItem?.let {
                runCatching { it.mediaId.toUri() }.getOrNull()
            }
        }
        .flowOn(Dispatchers.IO)
        .shareIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val audio = mediaItemUri
        .flatMapLatest { mediaItemUri ->
            mediaItemUri?.let {
                mediaRepository.audio(it)
            } ?: flowOf(Result.Failure(Error.NOT_FOUND))
        }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = FlowResult.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val playbackState = mediaControllerFlow
        .flatMapLatest { it.playbackStateFlow(eventsFlow) }
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = PlaybackState.IDLE
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val isPlaying = mediaControllerFlow
        .flatMapLatest { it.isPlayingFlow(eventsFlow) }
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val shuffleMode = mediaControllerFlow
        .flatMapLatest { it.shuffleModeFlow(eventsFlow) }
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val repeatMode = mediaControllerFlow
        .flatMapLatest { it.repeatModeFlow(eventsFlow) }
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = RepeatMode.NONE
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val playbackParameters = mediaControllerFlow
        .flatMapLatest { it.playbackParametersFlow(eventsFlow) }
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = PlaybackParameters.DEFAULT
        )

    val mediaArtwork = combine(
        mediaMetadata,
        playbackState,
    ) { mediaMetadata, playbackState ->
        when (playbackState) {
            PlaybackState.BUFFERING -> null
            else -> mediaMetadata.toThumbnail(applicationContext)?.let {
                Result.Success(it)
            } ?: Result.Failure(Error.NOT_FOUND)
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentTrackFormat = mediaControllerFlow
        .flatMapLatest { it.tracksFlow(eventsFlow) }
        .flowOn(Dispatchers.Main)
        .mapLatest { tracks ->
            val groups = tracks.groups.filter { group ->
                group.type == C.TRACK_TYPE_AUDIO && group.isSelected
            }

            require(groups.size <= 1) { "More than one audio track selected" }

            groups.firstOrNull()?.let { group ->
                (0 until group.length).firstNotNullOfOrNull { i ->
                    when (group.isTrackSelected(i)) {
                        true -> group.getTrackFormat(i)
                        false -> null
                    }
                }
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    private val effectiveFormat = combine(
        currentTrackFormat,
        outputConfigurationRepository.format,
    ) { trackFormat, repoFormat ->
        trackFormat ?: repoFormat
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    val mimeType = combine(effectiveFormat, audio, mediaItem) { format, audioResult, mediaItem ->
        format?.sampleMimeType
            ?: format?.containerMimeType
            ?: audioResult.getOrNull()?.mimeType
            ?: mediaItem?.localConfiguration?.mimeType
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    @androidx.annotation.OptIn(UnstableApi::class)
    @OptIn(ExperimentalCoroutinesApi::class)
    val displayFileType = combine(mimeType, effectiveFormat) { mimeType, format ->
        mimeType?.let {
            MimeUtils.mimeTypeToDisplayName(it, format?.codecs)
        }
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val availableCommands = mediaControllerFlow
        .flatMapLatest { it.availableCommandsFlow(eventsFlow) }
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = Player.Commands.EMPTY
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val durationCurrentPositionMs = mediaControllerFlow
        .flatMapLatest { mediaController ->
            flow {
                while (true) {
                    val duration = mediaController.duration.takeIf { it != C.TIME_UNSET }
                    val currentPosition =
                        mediaController.currentPosition.takeIf { duration != null }
                    emit(duration to currentPosition)
                    delay(200)
                }
            }
        }
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null to null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val audioSessionId = mediaControllerFlow
        .mapLatest { mediaController ->
            mediaController.sendCustomCommand(
                PlaybackService.CustomCommand.GET_AUDIO_SESSION_ID,
                Bundle.EMPTY
            ).extras.getInt(PlaybackService.CustomCommand.RSP_VALUE)
        }
        .flowOn(Dispatchers.Main)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    private val _currentVisualizerType = MutableStateFlow(VisualizerType.entries.first())
    val currentVisualizerType = combine(
        _currentVisualizerType,
        isPlaying,
    ) { currentVisualizerType, isPlaying ->
        currentVisualizerType.takeIf { isPlaying } ?: VisualizerType.NONE
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = VisualizerType.NONE
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val isVisualizerEnabled = currentVisualizerType
        .mapLatest { it != VisualizerType.NONE }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val lyrics = mediaItemUri
        .flatMapLatest { mediaItemUri ->
            mediaItemUri?.let {
                mediaRepository.lyrics(it)
            } ?: flowOf(Result.Failure(Error.NOT_FOUND))
        }
        .asFlowResult()
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = FlowResult.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val lyricsLines = lyrics
        .flatMapLatestData { lyrics ->
            durationCurrentPositionMs.mapLatest { durationCurrentPositionMs ->
                var currentIndex: Int? = null

                val linesWithState = lyrics.lines.withIndex().map { (i, line) ->
                    val lyricsLineState = line.durationMs?.let { durationMs ->
                        durationCurrentPositionMs.second?.let { currentPositionMs ->
                            when {
                                currentPositionMs < durationMs.first -> LyricsLineState.PENDING
                                currentPositionMs in durationMs -> LyricsLineState.ACTIVE
                                currentPositionMs > durationMs.last -> LyricsLineState.PAST
                                else -> LyricsLineState.UNKNOWN
                            }
                        }
                    } ?: LyricsLineState.UNKNOWN

                    if (lyricsLineState == LyricsLineState.ACTIVE
                        || lyricsLineState == LyricsLineState.PAST
                    ) {
                        currentIndex = i
                    }

                    line to lyricsLineState
                }

                FlowResult.Success(linesWithState to currentIndex)
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = FlowResult.Loading
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val outputSource = effectiveFormat
        .mapLatest { it?.toModel() }
        .flowOn(Dispatchers.IO)
        .shareIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1
        )

    private val outputTranscoding = combine(
        outputConfigurationRepository.audioTrackConfig,
        outputConfigurationRepository.format,
        outputConfigurationRepository.directOutputActive,
    ) { audioTrackConfig, format, directOutputActive ->
        OutputConfigurationUtils.buildOutputTranscoding(
            audioTrackConfig = audioTrackConfig,
            format = format,
            directOutputActive = directOutputActive,
        )
    }
        .flowOn(Dispatchers.IO)
        .shareIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            replay = 1
        )

    @androidx.annotation.OptIn(UnstableApi::class)
    val outputConfiguration = combine(
        outputSource,
        outputTranscoding,
        outputConfigurationRepository.device,
    ) { outputSource, outputTranscoding, outputDevice ->
        OutputConfigurationUtils.classify(
            source = outputSource,
            transcoding = outputTranscoding,
            device = outputDevice,
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    private val extractedTrackSpecs: StateFlow<TrackAudioSpecs?> = combine(
        audio,
        mediaItem,
    ) { audioResult, mediaItem ->
        val resolvedAudio = audioResult.getOrNull()
        val uri = resolvedAudio?.playbackUri ?: mediaItem?.localConfiguration?.uri
        val mime = resolvedAudio?.mimeType ?: mediaItem?.localConfiguration?.mimeType
        val durationMs = resolvedAudio?.durationMs
        Triple(uri, mime, durationMs)
    }
        .flatMapLatest { (uri, mime, durationMs) ->
            if (uri == null && mime == null) {
                flowOf<TrackAudioSpecs?>(null)
            } else {
                flow<TrackAudioSpecs?> {
                    emit(
                        AudioMetadataExtractor.extract(
                            context = applicationContext,
                            uri = uri,
                            mimeType = mime,
                            durationMs = durationMs,
                            deepExtract = true,
                        )
                    )
                }
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    /**
     * Unified track audio specs combining ExoPlayer telemetry with deep file extraction
     * (MediaStore bitrate/size/duration fallback + MediaMetadataRetriever + MediaExtractor).
     */
    val trackAudioSpecs: StateFlow<TrackAudioSpecs> = combine(
        effectiveFormat,
        outputConfiguration,
        extractedTrackSpecs,
        mimeType,
        audio,
    ) { format, config, extracted, mime, audioResult ->
        val formatModel = format?.toModel()
        val formatDisplayName = format?.let { f ->
            (f.sampleMimeType ?: f.containerMimeType)?.let {
                MimeUtils.mimeTypeToDisplayName(it, f.codecs)
            }
        }
        val resolvedFormat = when {
            extracted?.format == "ALAC" -> "ALAC"
            !formatDisplayName.isNullOrBlank() -> formatDisplayName
            !extracted?.format.isNullOrBlank() -> extracted.format
            else -> mime?.let { MimeUtils.mimeTypeToDisplayName(it, format?.codecs) }
        }

        val sampleRateHz = config?.source?.sampleRateHz
            ?: format?.sampleRate?.takeIf { it > 0 }
            ?: extracted?.sampleRateHz
            ?: config?.transcoding?.sampleRateHz

        val bitDepth = config?.source?.bitDepth
            ?: format?.pcmEncoding?.let { OutputConfigurationUtils.pcmEncodingBitDepth(it) }
            ?: extracted?.bitDepth
            ?: config?.transcoding?.bitDepth

        val channels = config?.source?.channelCount
            ?: format?.channelCount?.takeIf { it > 0 }
        val pcmFallbackBps = if (sampleRateHz != null && channels != null && bitDepth != null) {
            (sampleRateHz.toLong() * channels * bitDepth).toInt().takeIf { it > 0 }
        } else {
            null
        }

        val bitrateBps = config?.source?.averageBitrateBps?.takeIf { it > 0 }
            ?: config?.source?.peakBitrateBps?.takeIf { it > 0 }
            ?: format?.averageBitrate?.takeIf { it > 0 }
            ?: format?.peakBitrate?.takeIf { it > 0 }
            ?: format?.bitrate?.takeIf { it > 0 }
            ?: extracted?.bitrateBps?.takeIf { it > 0 }
            ?: config?.transcoding?.bitrateBps?.takeIf { it > 0 }
            ?: pcmFallbackBps

        val compression = config?.source?.compression ?: formatModel?.compression
        val isLosslessCompression = compression == OutputConfiguration.Compression.LOSSLESS ||
                compression == OutputConfiguration.Compression.UNCOMPRESSED
        val trackUriString = audioResult.getOrNull()?.playbackUri?.toString()
        val isLosslessHint = isLosslessCompression ||
                extracted?.isHiRes == true ||
                extracted?.isHiFi == true ||
                MimeUtils.isLosslessMimeType(mime, format?.codecs, trackUriString)

        AudioMetadataExtractor.classify(
            format = resolvedFormat,
            bitrateBps = bitrateBps,
            sampleRateHz = sampleRateHz,
            bitDepth = bitDepth,
            isLosslessHint = isLosslessHint,
            deepExtracted = extracted?.deepExtracted == true,
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = TrackAudioSpecs(),
        )

    /**
     * Reactive track format label (e.g., "FLAC", "AAC", "WAV", "ALAC").
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val trackFormat: StateFlow<String?> = trackAudioSpecs
        .mapLatest { it.format }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    /**
     * Reactive track bitrate in kbps (e.g., 912).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val trackBitrateKbps: StateFlow<Int?> = trackAudioSpecs
        .mapLatest { it.bitrateKbps }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    /**
     * Reactive track sample rate & bit depth formatted string (e.g., "24-bit / 96 kHz").
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val trackSampleRateAndBitDepth: StateFlow<String?> = trackAudioSpecs
        .mapLatest { it.sampleRateAndBitDepth }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val isHiRes: StateFlow<Boolean> = trackAudioSpecs
        .mapLatest { it.isHiRes }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    /**
     * Reactive boolean indicating whether the current track qualifies as Hi-Fi.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val isHiFi: StateFlow<Boolean> = trackAudioSpecs
        .mapLatest { it.isHiFi }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val isHq: StateFlow<Boolean> = trackAudioSpecs
        .mapLatest { it.isHq }
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = false,
        )

    /**
     * Returns true if the current playing track qualifies as Hi-Fi or Hi-Res.
     */
    fun isHiFi(): Boolean = isHiFi.value || isHiRes.value

    fun togglePlayPause() {
        mediaController.value?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun seekToPosition(positionMs: Long) {
        mediaController.value?.seekTo(positionMs)
    }

    fun seekToPrevious() {
        mediaController.value?.let {
            val currentMediaItemIndex = it.currentMediaItemIndex
            it.seekToPrevious()
            if (it.currentMediaItemIndex < currentMediaItemIndex) {
                it.play()
            }
        }
    }

    fun seekToNext() {
        mediaController.value?.let {
            it.seekToNext()
            it.play()
        }
    }

    fun toggleShuffleMode() {
        shuffleModeEnabled = shuffleModeEnabled.not()
    }

    fun toggleRepeatMode() {
        typedRepeatMode = typedRepeatMode.next()
    }

    fun nextVisualizerType() {
        _currentVisualizerType.value = _currentVisualizerType.value.next()
    }

    suspend fun toggleFavorites() {
        audio.value.getOrNull()?.let {
            withContext(Dispatchers.IO) {
                mediaRepository.setFavorite(it.uri, !it.isFavorite)
            }
        }
    }
}
