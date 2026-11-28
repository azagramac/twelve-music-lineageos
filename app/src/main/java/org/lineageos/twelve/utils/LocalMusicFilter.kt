/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.utils

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore

data class AllowedLocalMediaSnapshot(
    val audioIds: Set<Long>,
    val albumIds: Set<Long>,
    val artistIds: Set<Long>,
)

data class AudioFolderInfo(
    val relativePath: String,
    val trackCount: Int,
)

/**
 * Utilities for filtering local MediaStore audio files against customizable root directories
 * (defaulting to `Music`) while always excluding system sounds (ringtones, notifications, alarms).
 */
object LocalMusicFilter {
    private val excludedSystemPrefixes = listOf(
        "ringtones/",
        "notifications/",
        "alarms/",
        "android/media/",
    )

    fun normalizeDirectoryPath(raw: String): String =
        raw.trim().trim('/')

    fun extractRelativePathFromTreeUri(treeUri: Uri): String? {
        val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            ?: return null
        val pathPart = if (':' in docId) docId.substringAfter(':') else docId
        return normalizeDirectoryPath(pathPart).takeIf { it.isNotBlank() }
    }

    fun parseMediaStoreId(uri: Uri?): Long? {
        uri ?: return null
        return runCatching { ContentUris.parseId(uri) }.getOrNull()
    }

    fun queryAllowedLocalMedia(
        contentResolver: ContentResolver,
        allowedDirectories: Set<String>,
    ): AllowedLocalMediaSnapshot {
        val normalizedDirs = allowedDirectories
            .map(::normalizeDirectoryPath)
            .filter { it.isNotBlank() }
            .map { "${it.lowercase()}/" }

        val audioIds = HashSet<Long>()
        val albumIds = HashSet<Long>()
        val artistIds = HashSet<Long>()

        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.IS_RINGTONE,
                MediaStore.Audio.Media.IS_NOTIFICATION,
                MediaStore.Audio.Media.IS_ALARM,
                MediaStore.Audio.Media.RELATIVE_PATH,
            )

            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID)
                val ringtoneCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_RINGTONE)
                val notificationCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_NOTIFICATION)
                val alarmCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_ALARM)
                val relPathCol = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)

                while (cursor.moveToNext()) {
                    if (isSystemSoundRow(cursor, ringtoneCol, notificationCol, alarmCol, relPathCol)) {
                        continue
                    }

                    val relPath = if (relPathCol != -1) {
                        (cursor.getString(relPathCol) ?: "").trimStart('/')
                    } else {
                        ""
                    }
                    val lowerRel = relPath.lowercase()

                    if (normalizedDirs.isNotEmpty()) {
                        val matchesDir = normalizedDirs.any { prefix ->
                            lowerRel.startsWith(prefix) ||
                                lowerRel == prefix.removeSuffix("/")
                        }
                        if (!matchesDir) continue
                    }

                    if (idCol != -1) audioIds.add(cursor.getLong(idCol))
                    if (albumCol != -1) albumIds.add(cursor.getLong(albumCol))
                    if (artistCol != -1) artistIds.add(cursor.getLong(artistCol))
                }
            }
        } catch (_: Throwable) {
            // Keep empty snapshot if storage permissions are not granted yet
        }

        return AllowedLocalMediaSnapshot(
            audioIds = audioIds,
            albumIds = albumIds,
            artistIds = artistIds,
        )
    }

    fun queryAvailableAudioFolders(contentResolver: ContentResolver): List<AudioFolderInfo> {
        val counts = LinkedHashMap<String, Int>()
        counts["Music"] = 0

        try {
            val projection = arrayOf(
                MediaStore.Audio.Media.IS_RINGTONE,
                MediaStore.Audio.Media.IS_NOTIFICATION,
                MediaStore.Audio.Media.IS_ALARM,
                MediaStore.Audio.Media.RELATIVE_PATH,
            )

            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null,
            )?.use { cursor ->
                val ringtoneCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_RINGTONE)
                val notificationCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_NOTIFICATION)
                val alarmCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_ALARM)
                val relPathCol = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)

                while (cursor.moveToNext()) {
                    if (isSystemSoundRow(cursor, ringtoneCol, notificationCol, alarmCol, relPathCol)) {
                        continue
                    }
                    val folder = if (relPathCol != -1) {
                        normalizeDirectoryPath(cursor.getString(relPathCol) ?: "")
                    } else {
                        ""
                    }
                    if (folder.isNotBlank()) {
                        counts[folder] = (counts[folder] ?: 0) + 1
                        if (folder.startsWith("Music/", ignoreCase = true)) {
                            counts["Music"] = (counts["Music"] ?: 0) + 1
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            // Ignore when permission is not yet granted
        }

        return counts.entries
            .map { AudioFolderInfo(relativePath = it.key, trackCount = it.value) }
            .sortedWith(
                compareByDescending<AudioFolderInfo> { it.trackCount }
                    .thenBy { it.relativePath.lowercase() }
            )
    }

    private fun isSystemSoundRow(
        cursor: android.database.Cursor,
        ringtoneCol: Int,
        notificationCol: Int,
        alarmCol: Int,
        relPathCol: Int,
    ): Boolean {
        if (ringtoneCol != -1 && cursor.getInt(ringtoneCol) != 0) return true
        if (notificationCol != -1 && cursor.getInt(notificationCol) != 0) return true
        if (alarmCol != -1 && cursor.getInt(alarmCol) != 0) return true

        val lowerRel = if (relPathCol != -1) {
            (cursor.getString(relPathCol) ?: "").trimStart('/').lowercase()
        } else {
            ""
        }
        return excludedSystemPrefixes.any { lowerRel.startsWith(it) }
    }
}
