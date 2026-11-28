/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.utils

import android.content.ContentResolver
import android.content.Context
import android.media.AudioFormat
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Resolved audio technical specifications and Lune-equivalent quality tier flags for a track.
 */
@Immutable
data class TrackAudioSpecs(
    val format: String? = null,
    val bitrateBps: Int? = null,
    val sampleRateHz: Int? = null,
    val bitDepth: Int? = null,
    val channelCount: Int? = null,
    val fileSizeBytes: Long? = null,
    val displayName: String? = null,
    val isHiRes: Boolean = false,
    val isHiFi: Boolean = false,
    val isHq: Boolean = false,
    val deepExtracted: Boolean = false,
) {
    val bitrateKbps: Int?
        get() = bitrateBps?.takeIf { it > 0 }?.let { (it / 1000f).roundToInt().coerceAtLeast(1) }

    val sampleRateLabel: String?
        get() = sampleRateHz?.takeIf { it > 0 }?.let { hz ->
            if (hz % 1000 == 0) {
                "${hz / 1000} kHz"
            } else {
                "${String.format(Locale.US, "%.1f", hz / 1000f)} kHz"
            }
        }

    val bitDepthLabel: String?
        get() = bitDepth?.takeIf { it > 0 }?.let { "$it-bit" }

    val sampleRateAndBitDepth: String?
        get() = listOfNotNull(bitDepthLabel, sampleRateLabel)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" / ")
}

/**
 * Extracts audio container format, bitrate, sample rate, and bit depth using MediaStore,
 * file-size/duration fallback, [MediaMetadataRetriever], and [MediaExtractor], and classifies
 * tracks into `HI-RES`, `HI-FI`, or `HQ` following Lune's thresholds.
 */
object AudioMetadataExtractor {
    private val hiResFormats = setOf("DSD", "DSF", "DFF")
    private val hiFiFormats = setOf("FLAC", "WAV", "ALAC", "APE", "AIFF", "PCM", "WV", "TRUEHD")
    private val uncompressedFormats = setOf("WAV", "PCM", "AIFF")

    private val cache = object : LruCache<String, TrackAudioSpecs>(512) {}

    fun getCached(uri: Uri?): TrackAudioSpecs? {
        val key = uri?.toString() ?: return null
        return synchronized(cache) { cache.get(key) }
    }

    fun classify(
        format: String?,
        bitrateBps: Int?,
        sampleRateHz: Int?,
        bitDepth: Int?,
        isLosslessHint: Boolean = false,
        deepExtracted: Boolean = false,
        channelCount: Int? = null,
        fileSizeBytes: Long? = null,
        displayName: String? = null,
    ): TrackAudioSpecs {
        val normalizedFormat = format?.uppercase()?.takeIf { it.isNotBlank() }
        val isHiRes = ((sampleRateHz ?: 0) >= 48_000 && (bitDepth ?: 0) >= 24) ||
                (sampleRateHz ?: 0) >= 88_200 ||
                normalizedFormat in hiResFormats ||
                (bitrateBps ?: 0) >= 2_304_000

        val isHiFi = !isHiRes && (
                isLosslessHint ||
                        normalizedFormat in hiFiFormats ||
                        (bitrateBps ?: 0) > 320_000
                )

        val isHq = !isHiRes && !isHiFi && (bitrateBps ?: 0) >= 256_000

        return TrackAudioSpecs(
            format = normalizedFormat,
            bitrateBps = bitrateBps,
            sampleRateHz = sampleRateHz,
            bitDepth = bitDepth,
            channelCount = channelCount,
            fileSizeBytes = fileSizeBytes,
            displayName = displayName,
            isHiRes = isHiRes,
            isHiFi = isHiFi,
            isHq = isHq,
            deepExtracted = deepExtracted,
        )
    }

    suspend fun extract(
        context: Context,
        uri: Uri?,
        mimeType: String?,
        durationMs: Long?,
        deepExtract: Boolean = false,
    ): TrackAudioSpecs {
        val cacheKey = uri?.toString()
        if (cacheKey != null) {
            synchronized(cache) { cache.get(cacheKey) }?.let { cached ->
                if (!deepExtract || cached.deepExtracted) {
                    return cached
                }
            }
        }

        return withContext(Dispatchers.IO) {
            var format: String? = null
            var bitrateBps: Int? = null
            var sampleRateHz: Int? = null
            var bitDepth: Int? = null
            var channelCount: Int? = null
            var sizeBytes: Long? = null
            var displayName: String? = null
            var effectiveDurationMs: Long? = durationMs?.takeIf { it > 0L }
            var resolvedMime: String? = mimeType

            if (uri != null && uri.scheme == ContentResolver.SCHEME_CONTENT) {
                try {
                    val projection = arrayOf(
                        MediaStore.Audio.Media.BITRATE,
                        MediaStore.Audio.Media.SIZE,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.MIME_TYPE,
                        MediaStore.Audio.Media.DURATION,
                    )
                    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val bitrateCol = cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
                            val sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
                            val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                            val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                            val durationCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)

                            if (bitrateCol != -1) {
                                bitrateBps = cursor.getInt(bitrateCol).takeIf { it > 0 }
                            }
                            if (sizeCol != -1) {
                                sizeBytes = cursor.getLong(sizeCol).takeIf { it > 0L }
                            }
                            if (mimeCol != -1) {
                                cursor.getString(mimeCol)?.takeIf { it.isNotBlank() }?.let {
                                    resolvedMime = it
                                }
                            }
                            if (durationCol != -1 && effectiveDurationMs == null) {
                                effectiveDurationMs = cursor.getLong(durationCol).takeIf { it > 0L }
                            }
                            if (nameCol != -1) {
                                val resolvedName = cursor.getString(nameCol)?.takeIf { it.isNotBlank() }
                                displayName = resolvedName
                                val ext = resolvedName?.substringAfterLast('.', "")
                                if (!ext.isNullOrBlank()) {
                                    format = MimeUtils.extensionToDisplayName(ext)
                                }
                            }
                        }
                    }
                } catch (_: Throwable) {
                    // Ignore restricted ContentResolver columns
                }
            } else if (uri != null && uri.scheme == ContentResolver.SCHEME_FILE) {
                uri.path?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        sizeBytes = file.length().takeIf { it > 0L }
                        displayName = file.name.takeIf { it.isNotBlank() }
                        val ext = file.extension
                        if (ext.isNotBlank()) {
                            format = MimeUtils.extensionToDisplayName(ext)
                        }
                    }
                }
            }

            if (format == null) {
                val extFromUri = uri?.lastPathSegment?.substringAfterLast('.', "")
                if (!extFromUri.isNullOrBlank() && extFromUri != uri.lastPathSegment) {
                    format = MimeUtils.extensionToDisplayName(extFromUri)
                }
            }
            if (format == null) {
                resolvedMime?.let { format = MimeUtils.mimeTypeToDisplayName(it) }
            }

            val knownSizeBytes = sizeBytes
            val knownDurationMs = effectiveDurationMs
            if (bitrateBps == null &&
                knownSizeBytes != null &&
                knownSizeBytes > 0L &&
                knownDurationMs != null &&
                knownDurationMs > 0L
            ) {
                bitrateBps = ((knownSizeBytes * 8L * 1000L) / knownDurationMs)
                    .toInt()
                    .takeIf { it > 0 }
            }

            val shouldDeepExtract = deepExtract ||
                    format in hiFiFormats ||
                    format in hiResFormats ||
                    format == "AAC" ||
                    bitrateBps == null ||
                    format == null

            val isLocalUri = uri != null && (
                    uri.scheme == ContentResolver.SCHEME_CONTENT ||
                            uri.scheme == ContentResolver.SCHEME_FILE
                    )

            if (shouldDeepExtract && isLocalUri) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    if (bitrateBps == null) {
                        bitrateBps = retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                            ?.toIntOrNull()
                            ?.takeIf { it > 0 }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (sampleRateHz == null) {
                            sampleRateHz = retriever
                                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                                ?.toIntOrNull()
                                ?.takeIf { it > 0 }
                        }
                        if (bitDepth == null) {
                            bitDepth = retriever
                                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)
                                ?.toIntOrNull()
                                ?.takeIf { it > 0 }
                        }
                    }
                    if (format == null) {
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                            ?.let { format = MimeUtils.mimeTypeToDisplayName(it) }
                    }
                } catch (_: Throwable) {
                    // Best-effort metadata inspection
                } finally {
                    try {
                        retriever.release()
                    } catch (_: Throwable) {
                    }
                }

                if (sampleRateHz == null ||
                    bitDepth == null ||
                    bitrateBps == null ||
                    format == null ||
                    format == "AAC"
                ) {
                    val extractor = MediaExtractor()
                    try {
                        extractor.setDataSource(context, uri, null)
                        for (i in 0 until extractor.trackCount) {
                            val trackFormat = extractor.getTrackFormat(i)
                            val trackMime = trackFormat.getString(MediaFormat.KEY_MIME)
                            if (trackMime?.startsWith("audio/") == true) {
                                if (trackMime.contains("alac", ignoreCase = true)) {
                                    format = "ALAC"
                                } else if (format == null) {
                                    format = MimeUtils.mimeTypeToDisplayName(trackMime)
                                }

                                if (sampleRateHz == null &&
                                    trackFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                                ) {
                                    sampleRateHz = trackFormat
                                        .getInteger(MediaFormat.KEY_SAMPLE_RATE)
                                        .takeIf { it > 0 }
                                }
                                if (channelCount == null &&
                                    trackFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                                ) {
                                    channelCount = trackFormat
                                        .getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                                        .takeIf { it > 0 }
                                }
                                if (bitrateBps == null &&
                                    trackFormat.containsKey(MediaFormat.KEY_BIT_RATE)
                                ) {
                                    bitrateBps = trackFormat
                                        .getInteger(MediaFormat.KEY_BIT_RATE)
                                        .takeIf { it > 0 }
                                }
                                if (bitDepth == null &&
                                    trackFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)
                                ) {
                                    bitDepth = pcmEncodingToBitDepth(
                                        trackFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                                    )
                                }
                                if (bitDepth == null && trackFormat.containsKey("bits-per-sample")) {
                                    bitDepth = try {
                                        trackFormat.getInteger("bits-per-sample").takeIf { it > 0 }
                                    } catch (_: Throwable) {
                                        null
                                    }
                                }
                                break
                            }
                        }
                    } catch (_: Throwable) {
                        // Ignore unsupported containers
                    } finally {
                        try {
                            extractor.release()
                        } catch (_: Throwable) {
                        }
                    }
                }
            }

            val knownSampleRateHz = sampleRateHz
            val knownBitrateBps = bitrateBps
            if (bitDepth == null &&
                format in uncompressedFormats &&
                knownSampleRateHz != null &&
                knownBitrateBps != null
            ) {
                val channels = channelCount ?: 2
                val denom = knownSampleRateHz.toLong() * channels
                if (denom > 0L) {
                    val estimatedDepth = (knownBitrateBps.toLong() / denom).toInt()
                    if (estimatedDepth in setOf(8, 16, 24, 32)) {
                        bitDepth = estimatedDepth
                    }
                }
            }

            val isLosslessHint = MimeUtils.isLosslessMimeType(
                mimeType = resolvedMime,
                uriString = uri?.toString(),
            ) || format in hiFiFormats || format in hiResFormats

            val specs = classify(
                format = format,
                bitrateBps = bitrateBps,
                sampleRateHz = sampleRateHz,
                bitDepth = bitDepth,
                isLosslessHint = isLosslessHint,
                deepExtracted = shouldDeepExtract,
                channelCount = channelCount,
                fileSizeBytes = knownSizeBytes,
                displayName = displayName,
            )

            if (cacheKey != null) {
                synchronized(cache) { cache.put(cacheKey, specs) }
            }

            specs
        }
    }

    private fun pcmEncodingToBitDepth(encoding: Int): Int? = when {
        encoding == AudioFormat.ENCODING_PCM_8BIT -> 8
        encoding == AudioFormat.ENCODING_PCM_16BIT -> 16
        encoding == AudioFormat.ENCODING_PCM_FLOAT -> 32
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                encoding == AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                encoding == AudioFormat.ENCODING_PCM_32BIT -> 32

        else -> null
    }
}
