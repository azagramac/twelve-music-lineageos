/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.lineageos.twelve.models.OutputConfiguration
import org.lineageos.twelve.models.RepeatMode
import org.lineageos.twelve.ui.theme.TwelveExpressiveTheme

private val sampleNowPlayingState = NowPlayingUiState(
    title = "Tokyo Story (2025 Remaster)",
    artist = "Ryuichi Sakamoto",
    album = "Sweet Revenge",
    isPlaying = true,
    isBuffering = true,
    durationMs = 264_000L,
    currentPositionMs = 118_000L,
    shuffleEnabled = true,
    repeatMode = RepeatMode.ALL,
    isFavorite = true,
    isVisualizerEnabled = true,
    isHiFi = true,
    trackFormat = "FLAC",
    trackSampleRateAndBitDepth = "24-bit / 96 kHz",
    trackBitrateKbps = 1411,
    outputDeviceType = OutputConfiguration.Device.Type.USB,
    isDirectOutputActive = true,
    previousLyric = "Silent rain falls over Shibuya crossing",
    currentLyric = "Echoes in warm analog tape saturation",
    nextLyric = "Waiting for the last train home",
    showLyricsCard = true,
)

@Preview(
    name = "Now Playing M3E + Nier-Visualizer (Light Dynamic)",
    showBackground = true,
    widthDp = 412,
    heightDp = 915,
)
@Composable
fun NowPlayingExpressiveLightPreview() {
    TwelveExpressiveTheme(darkTheme = false, dynamicColor = true) {
        NowPlayingScreen(
            state = sampleNowPlayingState,
            callbacks = NowPlayingCallbacks(),
            previewVisualizerBars = true,
        )
    }
}

@Preview(
    name = "Now Playing M3E + Nier-Visualizer (Dark Dynamic)",
    showBackground = true,
    widthDp = 412,
    heightDp = 915,
)
@Composable
fun NowPlayingExpressiveDarkPreview() {
    TwelveExpressiveTheme(darkTheme = true, dynamicColor = true) {
        NowPlayingScreen(
            state = sampleNowPlayingState,
            callbacks = NowPlayingCallbacks(),
            previewVisualizerBars = true,
        )
    }
}

@Preview(
    name = "Now Playing M3E (Android 11 High-Chroma Fallback)",
    showBackground = true,
    widthDp = 412,
    heightDp = 915,
)
@Composable
fun NowPlayingExpressiveFallbackPreview() {
    TwelveExpressiveTheme(darkTheme = true, dynamicColor = false) {
        NowPlayingScreen(
            state = sampleNowPlayingState,
            callbacks = NowPlayingCallbacks(),
            previewVisualizerBars = true,
        )
    }
}
