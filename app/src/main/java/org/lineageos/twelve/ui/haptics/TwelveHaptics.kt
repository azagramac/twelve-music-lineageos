/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.haptics

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Lightweight haptic feedback controller backed by Android's [VibratorManager] (API 31+) or
 * [Vibrator] (API 30) for springy tactile confirmation on Material 3 Expressive controls.
 */
@Immutable
class TwelveHaptics(private val context: Context) {
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Triggers an ultra-light, short click haptic aligned with springy M3 Expressive motion.
     */
    @SuppressLint("MissingPermission")
    fun click() {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        runCatching {
            val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            } else {
                VibrationEffect.createOneShot(10L, 60)
            }
            vib.vibrate(effect)
        }.onFailure {
            runCatching {
                vib.vibrate(VibrationEffect.createOneShot(10L, 60))
            }
        }
    }

    /**
     * Triggers a subtle tick haptic for secondary selections or scrubbing detents.
     */
    @SuppressLint("MissingPermission")
    fun tick() {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        runCatching {
            val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            } else {
                VibrationEffect.createOneShot(8L, 40)
            }
            vib.vibrate(effect)
        }
    }

    companion object {
        fun click(context: Context) {
            TwelveHaptics(context.applicationContext).click()
        }
    }
}

/**
 * Remembers a [TwelveHaptics] instance bound to the current composition [Context].
 */
@Composable
fun rememberTwelveHaptics(): TwelveHaptics {
    val context = LocalContext.current
    return remember(context) { TwelveHaptics(context.applicationContext) }
}
