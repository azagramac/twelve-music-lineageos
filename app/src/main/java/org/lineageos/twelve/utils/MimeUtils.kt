/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.utils

import android.provider.MediaStore
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import org.lineageos.twelve.models.MediaType

object MimeUtils {
    private val losslessMimeTypes = setOf(
        MimeTypes.AUDIO_FLAC,
        "audio/x-flac",
        MimeTypes.AUDIO_ALAC,
        "audio/x-alac",
        MimeTypes.AUDIO_WAV,
        "audio/x-wav",
        "audio/vnd.wave",
        MimeTypes.AUDIO_RAW,
        MimeTypes.AUDIO_TRUEHD,
        "audio/dsd",
        "audio/x-dsd",
    )

    @androidx.annotation.OptIn(UnstableApi::class)
    fun mimeTypeToDisplayName(mimeType: String) =
        when (val it = MimeTypes.normalizeMimeType(mimeType)) {
            MimeTypes.AUDIO_MPEG -> "MP3"
            MimeTypes.AUDIO_FLAC, "audio/x-flac" -> "FLAC"
            MimeTypes.AUDIO_ALAC, "audio/x-alac" -> "ALAC"
            MimeTypes.AUDIO_WAV, "audio/x-wav", "audio/vnd.wave" -> "WAV"
            MimeTypes.AUDIO_AAC, "audio/mp4a-latm" -> "AAC"
            MimeTypes.AUDIO_OPUS -> "OPUS"
            MimeTypes.AUDIO_VORBIS -> "OGG"
            MimeTypes.AUDIO_RAW -> "PCM"
            "audio/dsd", "audio/x-dsd" -> "DSD"

            else -> it.takeIf { it.contains('/') }
                ?.substringAfterLast('/')
                ?.removePrefix("x-")
                ?.uppercase()
        }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun isLosslessMimeType(mimeType: String?): Boolean {
        mimeType ?: return false
        val normalized = MimeTypes.normalizeMimeType(mimeType).lowercase()
        return normalized in losslessMimeTypes ||
            normalized.contains("flac") ||
            normalized.contains("alac") ||
            normalized.contains("wav") ||
            normalized.contains("dsd") ||
            normalized.contains("pcm")
    }

    fun mimeTypeToMediaType(mimeType: String) = when {
        mimeType.startsWith("audio/") -> MediaType.AUDIO

        else -> when (mimeType) {
            "application/itunes",
            "application/ogg",
            "application/vnd.apple.mpegurl",
            "application/vnd.ms-sstr+xml",
            "application/x-mpegurl",
            "application/x-ogg" -> MediaType.AUDIO

            MediaStore.Audio.Media.ENTRY_CONTENT_TYPE -> MediaType.AUDIO

            MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> MediaType.ALBUM

            MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> MediaType.ARTIST

            MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE -> MediaType.GENRE

            @Suppress("DEPRECATION")
            MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE -> MediaType.PLAYLIST

            else -> null
        }
    }
}
