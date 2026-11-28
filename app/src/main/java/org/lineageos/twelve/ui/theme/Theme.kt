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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy

/**
 * Root Material 3 Expressive theme for Twelve.
 *
 * - Android 12+ (API 31+): automatically injects system Dynamic Color (`dynamicLightColorScheme` /
 *   `dynamicDarkColorScheme`) when [dynamicColor] is true.
 * - Android 11 (API 30): falls back to the high-chroma static palettes in `Color.kt`.
 * - Applies [MotionScheme.expressive], [TwelveExpressiveShapes], and [TwelveExpressiveTypography].
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TwelveExpressiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> TwelveExpressiveDarkColorScheme
        else -> TwelveExpressiveLightColorScheme
    }

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
