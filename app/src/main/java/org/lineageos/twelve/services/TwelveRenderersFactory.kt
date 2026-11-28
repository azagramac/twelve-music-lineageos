/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.services

import android.content.Context
import android.media.AudioDeviceInfo
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.AudioTrackAudioOutputProvider
import androidx.media3.exoplayer.audio.DefaultAudioOffloadSupportProvider
import androidx.media3.exoplayer.audio.DefaultAudioSink

@OptIn(UnstableApi::class)
class TwelveRenderersFactory(
    context: Context,
    private val enableAudioFloatOutput: Boolean,
    private var enableDirectOutput: Boolean = false,
    private val onAudioDeviceInfoChanged: (AudioDeviceInfo?) -> Unit,
    private val onAudioTrackConfigChanged: (AudioSink.AudioTrackConfig?) -> Unit,
    private val onDirectOutputActiveChanged: (Boolean) -> Unit = {},
) : DefaultRenderersFactory(context) {
    private var audioSink: TwelveAudioSink? = null

    init {
        setEnableAudioFloatOutput(enableAudioFloatOutput)
        setEnableAudioOutputPlaybackParameters(!enableDirectOutput)
        setExtensionRendererMode(EXTENSION_RENDERER_MODE_ON)
    }

    fun setDirectOutputEnabled(enabled: Boolean) {
        enableDirectOutput = enabled
        audioSink?.setDirectOutputEnabled(enabled)
    }

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ) = TwelveAudioSink(
        context = context,
        defaultAudioSink = DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput || enableAudioFloatOutput)
            .setEnableAudioOutputPlaybackParameters(
                enableAudioTrackPlaybackParams && !enableDirectOutput
            )
            .setAudioOutputProvider(
                AudioTrackAudioOutputProvider.Builder(context)
                    .setAudioOffloadSupportProvider(DefaultAudioOffloadSupportProvider(context))
                    .build()
            )
            .build(),
        initialDirectOutputEnabled = enableDirectOutput,
        onAudioDeviceInfoChanged = onAudioDeviceInfoChanged,
        onAudioTrackConfigChanged = onAudioTrackConfigChanged,
        onDirectOutputActiveChanged = onDirectOutputActiveChanged,
    ).also {
        audioSink = it
    }
}
