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
        "audio/aiff",
        "audio/x-aiff",
        "audio/ape",
        "audio/x-ape",
        "audio/wavpack",
        "audio/x-wavpack",
    )

    private val losslessExtensions = setOf(
        "flac", "alac", "wav", "wave", "aif", "aiff", "dsf", "dff", "wv", "ape", "pcm"
    )

    fun extensionToDisplayName(extension: String): String? =
        when (val normalized = extension.trim().lowercase()) {
            "mp3" -> "MP3"
            "flac" -> "FLAC"
            "wav", "wave" -> "WAV"
            "aac", "m4a" -> "AAC"
            "ogg", "oga" -> "OGG"
            "opus" -> "OPUS"
            "wma" -> "WMA"
            "alac" -> "ALAC"
            "dsf", "dff" -> "DSD"
            "aif", "aiff" -> "AIFF"
            "ape" -> "APE"
            "wv" -> "WV"
            else -> normalized.takeIf { it.isNotBlank() && it.length <= 5 }?.uppercase()
        }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun mimeTypeToDisplayName(mimeType: String, codecs: String? = null): String? {
        val normalizedCodecs = codecs?.lowercase().orEmpty()
        if (normalizedCodecs.contains("alac")) return "ALAC"
        if (normalizedCodecs.contains("flac")) return "FLAC"
        if (normalizedCodecs.contains("mlp") || normalizedCodecs.contains("truehd")) return "TRUEHD"

        return when (val it = MimeTypes.normalizeMimeType(mimeType)) {
            MimeTypes.AUDIO_MPEG -> "MP3"
            MimeTypes.AUDIO_FLAC, "audio/x-flac" -> "FLAC"
            MimeTypes.AUDIO_ALAC, "audio/x-alac" -> "ALAC"
            MimeTypes.AUDIO_WAV, "audio/x-wav", "audio/vnd.wave" -> "WAV"
            MimeTypes.AUDIO_AAC, "audio/mp4a-latm", "audio/mp4", "audio/m4a", "audio/x-m4a" -> "AAC"
            MimeTypes.AUDIO_OPUS -> "OPUS"
            MimeTypes.AUDIO_VORBIS -> "OGG"
            MimeTypes.AUDIO_RAW -> "PCM"
            "audio/x-ms-wma" -> "WMA"
            "audio/aiff", "audio/x-aiff" -> "AIFF"
            "audio/ape", "audio/x-ape" -> "APE"
            "audio/wavpack", "audio/x-wavpack" -> "WV"
            "audio/dsd", "audio/x-dsd" -> "DSD"

            else -> it.takeIf { it.contains('/') }
                ?.substringAfterLast('/')
                ?.removePrefix("x-")
                ?.uppercase()
        }
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    fun isLosslessMimeType(
        mimeType: String?,
        codecs: String? = null,
        uriString: String? = null,
    ): Boolean {
        val normalizedCodecs = codecs?.lowercase().orEmpty()
        if (
            normalizedCodecs.contains("alac") ||
            normalizedCodecs.contains("flac") ||
            normalizedCodecs.contains("pcm") ||
            normalizedCodecs.contains("dsd") ||
            normalizedCodecs.contains("truehd")
        ) {
            return true
        }

        if (mimeType != null) {
            val normalized = MimeTypes.normalizeMimeType(mimeType).lowercase()
            if (
                normalized in losslessMimeTypes ||
                normalized.contains("flac") ||
                normalized.contains("alac") ||
                normalized.contains("wav") ||
                normalized.contains("aiff") ||
                normalized.contains("dsd") ||
                normalized.contains("pcm") ||
                normalized.contains("lossless")
            ) {
                return true
            }
        }

        val ext = uriString
            ?.substringBefore('?')
            ?.substringBefore('#')
            ?.substringAfterLast('.', "")
            ?.lowercase()
        return ext in losslessExtensions
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
