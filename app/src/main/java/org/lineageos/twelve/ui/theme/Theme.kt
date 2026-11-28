/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.preference.PreferenceManager
import org.lineageos.twelve.ext.THEME_MODE_AMOLED
import org.lineageos.twelve.ext.THEME_MODE_DARK
import org.lineageos.twelve.ext.THEME_MODE_LIGHT
import org.lineageos.twelve.ext.THEME_MODE_SYSTEM
import org.lineageos.twelve.ext.dynamicColor
import org.lineageos.twelve.ext.dynamicColorFlow
import org.lineageos.twelve.ext.themeMode
import org.lineageos.twelve.ext.themeModeFlow

/**
 * Root Material 3 Expressive theme for Twelve.
 *
 * - Reactively observes user preferences for theme mode (System / Light / Dark / AMOLED Pure Black)
 *   and Dynamic Color when [darkTheme] / [dynamicColor] are omitted.
 * - Android 12+ (API 31+): injects system Dynamic Color (`dynamicLightColorScheme` /
 *   `dynamicDarkColorScheme`) when enabled.
 * - Android 11 (API 30): falls back to the high-chroma static palettes in `Color.kt`.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TwelveExpressiveTheme(
    darkTheme: Boolean? = null,
    dynamicColor: Boolean? = null,
    amoledBlack: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val systemDark = isSystemInDarkTheme()

    val resolvedThemeMode: String
    val resolvedDynamicColor: Boolean

    if (isInspection || (darkTheme != null && dynamicColor != null)) {
        resolvedThemeMode = when {
            amoledBlack == true -> THEME_MODE_AMOLED
            darkTheme == true -> THEME_MODE_DARK
            darkTheme == false -> THEME_MODE_LIGHT
            else -> THEME_MODE_SYSTEM
        }
        resolvedDynamicColor = dynamicColor ?: true
    } else {
        val prefs = remember(context) {
            PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        }
        val prefThemeMode by prefs.themeModeFlow.collectAsState(initial = prefs.themeMode)
        val prefDynamicColor by prefs.dynamicColorFlow.collectAsState(initial = prefs.dynamicColor)
        resolvedThemeMode = when {
            amoledBlack == true -> THEME_MODE_AMOLED
            darkTheme == true -> THEME_MODE_DARK
            darkTheme == false -> THEME_MODE_LIGHT
            else -> prefThemeMode
        }
        resolvedDynamicColor = dynamicColor ?: prefDynamicColor
    }

    val useDark = when (resolvedThemeMode) {
        THEME_MODE_LIGHT -> false
        THEME_MODE_DARK, THEME_MODE_AMOLED -> true
        else -> systemDark
    }
    val useAmoled = resolvedThemeMode == THEME_MODE_AMOLED

    val baseScheme = when {
        resolvedDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        useDark -> TwelveExpressiveDarkColorScheme
        else -> TwelveExpressiveLightColorScheme
    }

    val colorScheme = if (useAmoled) baseScheme.asAmoledPureBlack() else baseScheme

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = TwelveExpressiveShapes,
        typography = TwelveExpressiveTypography,
        content = content,
    )
}

/**
 * Convenience bridge for mounting Compose UI inside Fragments and custom Views with safe
 * lifecycle disposal and [TwelveExpressiveTheme] applied.
 */
fun ComposeView.setTwelveExpressiveContent(
    content: @Composable () -> Unit,
) {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        TwelveExpressiveTheme(content = content)
    }
}
