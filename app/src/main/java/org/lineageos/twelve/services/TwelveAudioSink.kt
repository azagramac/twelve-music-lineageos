/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.services

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.media.AudioRouting
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.Format
import androidx.media3.common.util.Clock
import androidx.media3.exoplayer.analytics.PlayerId
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioOffloadSupport
import androidx.media3.exoplayer.audio.AudioOutputProvider
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.AudioTrackAudioOutput
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * An [AudioSink] implementation that wraps a [DefaultAudioSink], exposes the routed
 * [AudioDeviceInfo] and [AudioSink.AudioTrackConfig], and manages Exclusive Direct Output /
 * Bit-Perfect mixer negotiation on supported Android API levels.
 */
@androidx.media3.common.util.UnstableApi
class TwelveAudioSink(
    context: Context,
    private val defaultAudioSink: DefaultAudioSink,
    initialDirectOutputEnabled: Boolean = false,
    private val onAudioDeviceInfoChanged: (AudioDeviceInfo?) -> Unit,
    private val onAudioTrackConfigChanged: (AudioSink.AudioTrackConfig?) -> Unit,
    private val onDirectOutputActiveChanged: (Boolean) -> Unit = {},
) : AudioSink by defaultAudioSink {
    private val audioManager = context.getSystemService(AudioManager::class.java)

    private val audioOutputField = DefaultAudioSink::class.java.getDeclaredField(
        "audioOutput"
    ).apply {
        isAccessible = true
    }
    private val audioTrackField = AudioTrackAudioOutput::class.java.getDeclaredField(
        "audioTrack"
    ).apply {
        isAccessible = true
    }

    private val audioTrack
        get() = audioOutputField.get(defaultAudioSink)?.let {
            audioTrackField.get(it) as AudioTrack?
        }

    private val handler = Handler(Looper.getMainLooper())

    private var directOutputEnabled = initialDirectOutputEnabled
    private var lastPreferredDevice: AudioDeviceInfo? = null
    private var lastPreferredAttributes: AudioAttributes? = null

    private val routingListener = AudioRouting.OnRoutingChangedListener { routing ->
        val device = routing.routedDevice
        onAudioDeviceInfoChanged(device)
        applyDirectOutputConfig(currentAudioTrack, device)
    }

    private var currentAudioTrack: AudioTrack? = null

    private var externalListener: AudioSink.Listener? = null

    private val proxyListener = object : AudioSink.Listener {
        override fun onPositionDiscontinuity() {
            externalListener?.onPositionDiscontinuity()
        }

        override fun onPositionAdvancing(playoutStartSystemTimeMs: Long) {
            externalListener?.onPositionAdvancing(playoutStartSystemTimeMs)
        }

        override fun onUnderrun(bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
            externalListener?.onUnderrun(
                bufferSize,
                bufferSizeMs,
                elapsedSinceLastFeedMs,
            )
        }

        override fun onSkipSilenceEnabledChanged(skipSilenceEnabled: Boolean) {
            externalListener?.onSkipSilenceEnabledChanged(skipSilenceEnabled)
        }

        override fun onOffloadBufferEmptying() {
            externalListener?.onOffloadBufferEmptying()
        }

        override fun onOffloadBufferFull() {
            externalListener?.onOffloadBufferFull()
        }

        override fun onAudioSinkError(audioSinkError: Exception) {
            externalListener?.onAudioSinkError(audioSinkError)
        }

        override fun onAudioCapabilitiesChanged() {
            externalListener?.onAudioCapabilitiesChanged()
        }

        override fun onAudioTrackInitialized(audioTrackConfig: AudioSink.AudioTrackConfig) {
            currentAudioTrack?.removeOnRoutingChangedListener(routingListener)

            val track = audioTrack
            currentAudioTrack = track

            onAudioTrackConfigChanged(audioTrackConfig)
            track?.addOnRoutingChangedListener(routingListener, handler)
            val routedDevice = track?.routedDevice
            onAudioDeviceInfoChanged(routedDevice)
            applyDirectOutputConfig(track, routedDevice)

            externalListener?.onAudioTrackInitialized(audioTrackConfig)
        }

        override fun onAudioTrackReleased(audioTrackConfig: AudioSink.AudioTrackConfig) {
            if (currentAudioTrack == null) {
                onAudioTrackConfigChanged(null)
                onAudioDeviceInfoChanged(null)
                onDirectOutputActiveChanged(false)
            }

            externalListener?.onAudioTrackReleased(audioTrackConfig)
        }

        override fun onSilenceSkipped() {
            externalListener?.onSilenceSkipped()
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            externalListener?.onAudioSessionIdChanged(audioSessionId)
        }
    }

    init {
        defaultAudioSink.setListener(proxyListener)
    }

    /**
     * Enables or disables Exclusive Direct Output (Bit-Perfect mixer bypass) at runtime.
     */
    fun setDirectOutputEnabled(enabled: Boolean) {
        directOutputEnabled = enabled
        val track = currentAudioTrack ?: audioTrack
        applyDirectOutputConfig(track, track?.routedDevice)
    }

    private fun applyDirectOutputConfig(track: AudioTrack?, routedDevice: AudioDeviceInfo?) {
        if (!directOutputEnabled || track == null) {
            clearBitPerfectMixerAttributes()
            onDirectOutputActiveChanged(false)
            return
        }

        val audioFormat = runCatching { track.format }.getOrNull()
        val audioAttributes = runCatching { track.audioAttributes }.getOrNull()
        if (audioFormat == null || audioAttributes == null) {
            onDirectOutputActiveChanged(false)
            return
        }

        var bitPerfectActive = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && routedDevice != null) {
            bitPerfectActive = tryEnableBitPerfectMixer(
                audioAttributes = audioAttributes,
                audioFormat = audioFormat,
                device = routedDevice,
            )
        }

        val directPlaybackSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                AudioManager.getDirectPlaybackSupport(audioFormat, audioAttributes) !=
                    AudioManager.DIRECT_PLAYBACK_NOT_SUPPORTED
            }.getOrDefault(false)
        } else {
            false
        }

        onDirectOutputActiveChanged(bitPerfectActive || directPlaybackSupported)
    }

    private fun tryEnableBitPerfectMixer(
        audioAttributes: AudioAttributes,
        audioFormat: AudioFormat,
        device: AudioDeviceInfo,
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return false
        }
        val manager = audioManager ?: return false

        return runCatching {
            val supported = manager.getSupportedMixerAttributes(device)
            val bitPerfectCandidate = supported.firstOrNull { attrs ->
                attrs.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT &&
                    (attrs.format.sampleRate == audioFormat.sampleRate ||
                        attrs.format.sampleRate == AudioFormat.SAMPLE_RATE_UNSPECIFIED)
            } ?: supported.firstOrNull { attrs ->
                attrs.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT
            } ?: AudioMixerAttributes.Builder(audioFormat)
                .setMixerBehavior(AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT)
                .build()

            val applied = manager.setPreferredMixerAttributes(
                audioAttributes,
                device,
                bitPerfectCandidate,
            )
            if (applied) {
                lastPreferredAttributes = audioAttributes
                lastPreferredDevice = device
            }
            applied
        }.onFailure { error ->
            Log.d(LOG_TAG, "Bit-perfect mixer attributes unavailable for device", error)
        }.getOrDefault(false)
    }

    private fun clearBitPerfectMixerAttributes() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            lastPreferredAttributes = null
            lastPreferredDevice = null
            return
        }
        val manager = audioManager ?: return
        val attrs = lastPreferredAttributes
        val device = lastPreferredDevice
        if (attrs != null && device != null) {
            runCatching {
                manager.clearPreferredMixerAttributes(attrs, device)
            }
        }
        lastPreferredAttributes = null
        lastPreferredDevice = null
    }

    override fun setListener(listener: AudioSink.Listener) {
        externalListener = listener
    }

    override fun setPlayerId(playerId: PlayerId?) {
        defaultAudioSink.setPlayerId(playerId)
    }

    override fun setClock(clock: Clock) {
        defaultAudioSink.setClock(clock)
    }

    override fun getFormatOffloadSupport(format: Format): AudioOffloadSupport {
        return defaultAudioSink.getFormatOffloadSupport(format)
    }

    override fun configure(audioSinkConfig: AudioSink.AudioSinkConfig) {
        defaultAudioSink.configure(audioSinkConfig)
    }

    override fun getAudioCapabilities(): AudioCapabilities? {
        return defaultAudioSink.getAudioCapabilities()
    }

    override fun setPreferredDevice(audioDeviceInfo: AudioDeviceInfo?) {
        defaultAudioSink.setPreferredDevice(audioDeviceInfo)
    }

    override fun setVirtualDeviceId(virtualDeviceId: Int) {
        defaultAudioSink.setVirtualDeviceId(virtualDeviceId)
    }

    override fun setOutputStreamOffsetUs(outputStreamOffsetUs: Long) {
        defaultAudioSink.setOutputStreamOffsetUs(outputStreamOffsetUs)
    }

    override fun setOffloadMode(offloadMode: Int) {
        defaultAudioSink.setOffloadMode(offloadMode)
    }

    override fun setOffloadDelayPadding(delayInFrames: Int, paddingInFrames: Int) {
        defaultAudioSink.setOffloadDelayPadding(delayInFrames, paddingInFrames)
    }

    override fun setAudioOutputProvider(audioOutputProvider: AudioOutputProvider) {
        defaultAudioSink.setAudioOutputProvider(audioOutputProvider)
    }

    override fun flush() {
        defaultAudioSink.flush()
    }

    override fun release() {
        clearBitPerfectMixerAttributes()
        audioTrack?.removeOnRoutingChangedListener(routingListener)
        defaultAudioSink.release()
    }

    companion object {
        private val LOG_TAG = TwelveAudioSink::class.simpleName!!
    }
}
