/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * High-chroma expressive fallback palette for Android 11 (API 30) devices
 * where system Dynamic Color (API 31+) is unavailable.
 */
val TwelvePrimaryLight = Color(0xFF5B3CE6)
val TwelveOnPrimaryLight = Color(0xFFFFFFFF)
val TwelvePrimaryContainerLight = Color(0xFFE6DEFF)
val TwelveOnPrimaryContainerLight = Color(0xFF1A0063)

val TwelveSecondaryLight = Color(0xFF6B4FE0)
val TwelveOnSecondaryLight = Color(0xFFFFFFFF)
val TwelveSecondaryContainerLight = Color(0xFFEADDFF)
val TwelveOnSecondaryContainerLight = Color(0xFF21005D)

val TwelveTertiaryLight = Color(0xFFBC2A6E)
val TwelveOnTertiaryLight = Color(0xFFFFFFFF)
val TwelveTertiaryContainerLight = Color(0xFFFFD9E4)
val TwelveOnTertiaryContainerLight = Color(0xFF3E001F)

val TwelveErrorLight = Color(0xFFBA1A1A)
val TwelveOnErrorLight = Color(0xFFFFFFFF)
val TwelveErrorContainerLight = Color(0xFFFFDAD6)
val TwelveOnErrorContainerLight = Color(0xFF410002)

val TwelveBackgroundLight = Color(0xFFFDF8FF)
val TwelveOnBackgroundLight = Color(0xFF1C1B22)
val TwelveSurfaceLight = Color(0xFFFDF8FF)
val TwelveOnSurfaceLight = Color(0xFF1C1B22)
val TwelveSurfaceVariantLight = Color(0xFFE6E0EC)
val TwelveOnSurfaceVariantLight = Color(0xFF48454F)
val TwelveOutlineLight = Color(0xFF797580)
val TwelveOutlineVariantLight = Color(0xFFCAC4D0)

val TwelveSurfaceContainerLowestLight = Color(0xFFFFFFFF)
val TwelveSurfaceContainerLowLight = Color(0xFFF7F2FA)
val TwelveSurfaceContainerLight = Color(0xFFF1ECF4)
val TwelveSurfaceContainerHighLight = Color(0xFFEBE6EE)
val TwelveSurfaceContainerHighestLight = Color(0xFFE5E0E9)

val TwelvePrimaryDark = Color(0xFFCBC0FF)
val TwelveOnPrimaryDark = Color(0xFF2C009D)
val TwelvePrimaryContainerDark = Color(0xFF421EC7)
val TwelveOnPrimaryContainerDark = Color(0xFFE6DEFF)

val TwelveSecondaryDark = Color(0xFFD0BCFF)
val TwelveOnSecondaryDark = Color(0xFF381E72)
val TwelveSecondaryContainerDark = Color(0xFF4F378B)
val TwelveOnSecondaryContainerDark = Color(0xFFEADDFF)

val TwelveTertiaryDark = Color(0xFFFFB0CC)
val TwelveOnTertiaryDark = Color(0xFF640038)
val TwelveTertiaryContainerDark = Color(0xFF930E52)
val TwelveOnTertiaryContainerDark = Color(0xFFFFD9E4)

val TwelveErrorDark = Color(0xFFFFB4AB)
val TwelveOnErrorDark = Color(0xFF690005)
val TwelveErrorContainerDark = Color(0xFF93000A)
val TwelveOnErrorContainerDark = Color(0xFFFFDAD6)

val TwelveBackgroundDark = Color(0xFF141219)
val TwelveOnBackgroundDark = Color(0xFFE6E1EB)
val TwelveSurfaceDark = Color(0xFF141219)
val TwelveOnSurfaceDark = Color(0xFFE6E1EB)
val TwelveSurfaceVariantDark = Color(0xFF48454F)
val TwelveOnSurfaceVariantDark = Color(0xFFCAC4D0)
val TwelveOutlineDark = Color(0xFF938F9A)
val TwelveOutlineVariantDark = Color(0xFF48454F)

val TwelveSurfaceContainerLowestDark = Color(0xFF0E0D13)
val TwelveSurfaceContainerLowDark = Color(0xFF1C1B22)
val TwelveSurfaceContainerDark = Color(0xFF201F26)
val TwelveSurfaceContainerHighDark = Color(0xFF2B2931)
val TwelveSurfaceContainerHighestDark = Color(0xFF36343C)

val TwelveExpressiveLightColorScheme = lightColorScheme(
    primary = TwelvePrimaryLight,
    onPrimary = TwelveOnPrimaryLight,
    primaryContainer = TwelvePrimaryContainerLight,
    onPrimaryContainer = TwelveOnPrimaryContainerLight,
    secondary = TwelveSecondaryLight,
    onSecondary = TwelveOnSecondaryLight,
    secondaryContainer = TwelveSecondaryContainerLight,
    onSecondaryContainer = TwelveOnSecondaryContainerLight,
    tertiary = TwelveTertiaryLight,
    onTertiary = TwelveOnTertiaryLight,
    tertiaryContainer = TwelveTertiaryContainerLight,
    onTertiaryContainer = TwelveOnTertiaryContainerLight,
    error = TwelveErrorLight,
    onError = TwelveOnErrorLight,
    errorContainer = TwelveErrorContainerLight,
    onErrorContainer = TwelveOnErrorContainerLight,
    background = TwelveBackgroundLight,
    onBackground = TwelveOnBackgroundLight,
    surface = TwelveSurfaceLight,
    onSurface = TwelveOnSurfaceLight,
    surfaceVariant = TwelveSurfaceVariantLight,
    onSurfaceVariant = TwelveOnSurfaceVariantLight,
    outline = TwelveOutlineLight,
    outlineVariant = TwelveOutlineVariantLight,
    surfaceContainerLowest = TwelveSurfaceContainerLowestLight,
    surfaceContainerLow = TwelveSurfaceContainerLowLight,
    surfaceContainer = TwelveSurfaceContainerLight,
    surfaceContainerHigh = TwelveSurfaceContainerHighLight,
    surfaceContainerHighest = TwelveSurfaceContainerHighestLight,
)

val TwelveExpressiveDarkColorScheme = darkColorScheme(
    primary = TwelvePrimaryDark,
    onPrimary = TwelveOnPrimaryDark,
    primaryContainer = TwelvePrimaryContainerDark,
    onPrimaryContainer = TwelveOnPrimaryContainerDark,
    secondary = TwelveSecondaryDark,
    onSecondary = TwelveOnSecondaryDark,
    secondaryContainer = TwelveSecondaryContainerDark,
    onSecondaryContainer = TwelveOnSecondaryContainerDark,
    tertiary = TwelveTertiaryDark,
    onTertiary = TwelveOnTertiaryDark,
    tertiaryContainer = TwelveTertiaryContainerDark,
    onTertiaryContainer = TwelveOnTertiaryContainerDark,
    error = TwelveErrorDark,
    onError = TwelveOnErrorDark,
    errorContainer = TwelveErrorContainerDark,
    onErrorContainer = TwelveOnErrorContainerDark,
    background = TwelveBackgroundDark,
    onBackground = TwelveOnBackgroundDark,
    surface = TwelveSurfaceDark,
    onSurface = TwelveOnSurfaceDark,
    surfaceVariant = TwelveSurfaceVariantDark,
    onSurfaceVariant = TwelveOnSurfaceVariantDark,
    outline = TwelveOutlineDark,
    outlineVariant = TwelveOutlineVariantDark,
    surfaceContainerLowest = TwelveSurfaceContainerLowestDark,
    surfaceContainerLow = TwelveSurfaceContainerLowDark,
    surfaceContainer = TwelveSurfaceContainerDark,
    surfaceContainerHigh = TwelveSurfaceContainerHighDark,
    surfaceContainerHighest = TwelveSurfaceContainerHighestDark,
)

fun ColorScheme.asAmoledPureBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0A0A0D),
    surfaceContainer = Color(0xFF121216),
    surfaceContainerHigh = Color(0xFF1A1A20),
    surfaceContainerHighest = Color(0xFF23232B),
)
