/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lineageos.twelve.models.Thumbnail
import kotlin.math.abs
import kotlin.random.Random

@Immutable
data class CoverPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color,
)

/**
 * Extracts a 4-color harmonious palette from 5 spatial zones of [bitmap], tuned for
 * either dark or light themes (with chromatic hue-shift expansion for monochrome covers).
 */
fun extractPaletteFromBitmap(bitmap: Bitmap, isDarkTheme: Boolean): CoverPalette {
    val scaled = if (bitmap.width > 32 || bitmap.height > 32) {
        Bitmap.createScaledBitmap(bitmap, 32, 32, false)
    } else {
        bitmap
    }

    val w = scaled.width.coerceAtLeast(2)
    val h = scaled.height.coerceAtLeast(2)
    val hsl = FloatArray(3)

    fun sampleZone(startX: Int, endX: Int, startY: Int, endY: Int): Color? {
        var bestColor: Color? = null
        var maxScore = -1f
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var count = 0

        for (y in startY until endY) {
            for (x in startX until endX) {
                val pixel = scaled.getPixel(x, y)
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha < 128) continue

                val r = (pixel ushr 16) and 0xFF
                val g = (pixel ushr 8) and 0xFF
                val b = pixel and 0xFF
                sumR += r
                sumG += g
                sumB += b
                count++

                ColorUtils.RGBToHSL(r, g, b, hsl)
                val saturation = hsl[1]
                val lightness = hsl[2]
                val score =
                    saturation * (1f - abs(lightness - 0.5f) * 1.5f).coerceAtLeast(0.05f)
                if (score > maxScore && saturation > 0.10f && lightness in 0.08f..0.92f) {
                    maxScore = score
                    bestColor = Color(r, g, b)
                }
            }
        }

        return bestColor ?: if (count > 0) {
            Color((sumR / count).toInt(), (sumG / count).toInt(), (sumB / count).toInt())
        } else {
            null
        }
    }

    val cTopLeft = sampleZone(0, w / 2, 0, h / 2)
    val cTopRight = sampleZone(w / 2, w, 0, h / 2)
    val cBottomLeft = sampleZone(0, w / 2, h / 2, h)
    val cBottomRight = sampleZone(w / 2, w, h / 2, h)
    val cCenter = sampleZone(w / 4, (3 * w) / 4, h / 4, (3 * h) / 4)

    val fallbackBase = cCenter ?: Color(0xFF6750A4)
    val raw1 = cTopLeft ?: fallbackBase
    val raw2 = cTopRight ?: cCenter ?: fallbackBase
    val raw3 = cBottomLeft ?: cCenter ?: fallbackBase
    val raw4 = cBottomRight ?: fallbackBase

    fun adjust(color: Color): Color {
        val tempHsl = FloatArray(3)
        ColorUtils.RGBToHSL(
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt(),
            tempHsl,
        )
        if (isDarkTheme) {
            tempHsl[1] = tempHsl[1].coerceIn(0.42f, 0.90f)
            tempHsl[2] = tempHsl[2].coerceIn(0.30f, 0.56f)
        } else {
            tempHsl[1] = tempHsl[1].coerceIn(0.28f, 0.68f)
            tempHsl[2] = tempHsl[2].coerceIn(0.56f, 0.80f)
        }
        return Color(ColorUtils.HSLToColor(tempHsl))
    }

    val adj1 = adjust(raw1)
    val adj2 = adjust(raw2)
    val adj3 = adjust(raw3)
    val adj4 = adjust(raw4)

    val hsl0 = FloatArray(3)
    val hsl1 = FloatArray(3)
    ColorUtils.colorToHSL(adj1.toArgb(), hsl0)
    ColorUtils.colorToHSL(adj2.toArgb(), hsl1)
    val hueDiff = abs(hsl0[0] - hsl1[0])

    if (hueDiff < 25f || hsl0[1] < 0.15f) {
        val baseHue = if (hsl0[1] < 0.15f) (if (isDarkTheme) 220f else 200f) else hsl0[0]
        val sat = if (isDarkTheme) 0.58f else 0.44f
        val lit = if (isDarkTheme) 0.40f else 0.68f

        fun shift(hShift: Float): Color {
            val hValue = (baseHue + hShift + 360f) % 360f
            return Color(ColorUtils.HSLToColor(floatArrayOf(hValue, sat, lit)))
        }

        return CoverPalette(
            primary = adj1,
            secondary = shift(35f),
            tertiary = shift(-35f),
            accent = shift(70f),
        )
    }

    return CoverPalette(adj1, adj2, adj3, adj4)
}

/**
 * Extracts and remembers a dynamic [CoverPalette] from [artwork] using Coil 3 or its direct
 * embedded bitmap, falling back to the active Material 3 scheme when unavailable.
 */
@Composable
fun rememberCoverPalette(
    artwork: Thumbnail?,
    isDarkTheme: Boolean,
): CoverPalette {
    val context = LocalContext.current
    val primaryScheme = MaterialTheme.colorScheme.primary
    val secondaryScheme = MaterialTheme.colorScheme.secondary
    val tertiaryScheme = MaterialTheme.colorScheme.tertiary
    val containerScheme = MaterialTheme.colorScheme.primaryContainer

    val defaultPalette = remember(primaryScheme, secondaryScheme, tertiaryScheme, containerScheme) {
        CoverPalette(
            primary = primaryScheme,
            secondary = secondaryScheme,
            tertiary = tertiaryScheme,
            accent = containerScheme,
        )
    }

    var palette by remember(artwork, isDarkTheme) { mutableStateOf(defaultPalette) }

    LaunchedEffect(artwork, isDarkTheme) {
        if (artwork == null) {
            palette = defaultPalette
            return@LaunchedEffect
        }

        val extracted = withContext(Dispatchers.IO) {
            try {
                val bitmap = artwork.bitmap ?: run {
                    val request = ImageRequest.Builder(context)
                        .data(artwork)
                        .size(64, 64)
                        .allowHardware(false)
                        .build()
                    val result = SingletonImageLoader.get(context).execute(request)
                    (result as? SuccessResult)?.image?.toBitmap()
                }
                bitmap?.let { extractPaletteFromBitmap(it, isDarkTheme) }
            } catch (_: Throwable) {
                null
            }
        }
        if (extracted != null) {
            palette = extracted
        }
    }

    return palette
}

/**
 * Lune-inspired animated 5-blob organic mesh gradient driven by the track's artwork palette.
 */
@Composable
fun PlayerGradientBackground(
    colors: CoverPalette,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
) {
    val posX1 = remember { Animatable(0.20f) }
    val posY1 = remember { Animatable(0.20f) }
    val scale1 = remember { Animatable(0.85f) }

    val posX2 = remember { Animatable(0.80f) }
    val posY2 = remember { Animatable(0.30f) }
    val scale2 = remember { Animatable(0.80f) }

    val posX3 = remember { Animatable(0.22f) }
    val posY3 = remember { Animatable(0.72f) }
    val scale3 = remember { Animatable(0.85f) }

    val posX4 = remember { Animatable(0.82f) }
    val posY4 = remember { Animatable(0.80f) }
    val scale4 = remember { Animatable(0.80f) }

    val posX5 = remember { Animatable(0.50f) }
    val posY5 = remember { Animatable(0.50f) }
    val scale5 = remember { Animatable(0.65f) }

    LaunchedEffect(Unit) {
        val smoothEasing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)

        fun animateWander(
            animatable: Animatable<Float, AnimationVector1D>,
            min: Float,
            max: Float,
            minDur: Int,
            maxDur: Int,
        ) = launch {
            while (isActive) {
                val target = Random.nextFloat() * (max - min) + min
                val duration = Random.nextInt(minDur, maxDur)
                animatable.animateTo(
                    targetValue = target,
                    animationSpec = tween(durationMillis = duration, easing = smoothEasing),
                )
            }
        }

        animateWander(posX1, 0.06f, 0.38f, 8000, 15000)
        animateWander(posY1, 0.06f, 0.38f, 10000, 18000)
        animateWander(scale1, 0.75f, 0.95f, 9000, 16000)

        animateWander(posX2, 0.62f, 0.94f, 9000, 17000)
        animateWander(posY2, 0.12f, 0.44f, 8500, 16000)
        animateWander(scale2, 0.70f, 0.92f, 10000, 18000)

        animateWander(posX3, 0.08f, 0.40f, 8500, 16500)
        animateWander(posY3, 0.58f, 0.88f, 9500, 17500)
        animateWander(scale3, 0.75f, 0.95f, 9000, 15500)

        animateWander(posX4, 0.60f, 0.94f, 10000, 18000)
        animateWander(posY4, 0.60f, 0.92f, 9000, 16000)
        animateWander(scale4, 0.70f, 0.92f, 8500, 16500)

        animateWander(posX5, 0.35f, 0.65f, 11000, 19000)
        animateWander(posY5, 0.35f, 0.65f, 10500, 18500)
        animateWander(scale5, 0.55f, 0.75f, 9500, 17000)
    }

    val c1 by animateColorAsState(colors.primary, tween(1100), label = "MeshC1")
    val c2 by animateColorAsState(colors.secondary, tween(1100), label = "MeshC2")
    val c3 by animateColorAsState(colors.tertiary, tween(1100), label = "MeshC3")
    val c4 by animateColorAsState(colors.accent, tween(1100), label = "MeshC4")

    val surfaceColor = MaterialTheme.colorScheme.surface
    val baseAlpha = (if (isDarkTheme) 0.55f else 0.45f) * alpha

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val maxDim = maxOf(w, h).coerceAtLeast(1f)

        val blobs = listOf(
            Triple(Offset(w * posX1.value, h * posY1.value), maxDim * scale1.value, c1.copy(alpha = baseAlpha)),
            Triple(Offset(w * posX2.value, h * posY2.value), maxDim * scale2.value, c2.copy(alpha = baseAlpha * 0.90f)),
            Triple(Offset(w * posX3.value, h * posY3.value), maxDim * scale3.value, c3.copy(alpha = baseAlpha * 0.95f)),
            Triple(Offset(w * posX4.value, h * posY4.value), maxDim * scale4.value, c4.copy(alpha = baseAlpha * 0.85f)),
            Triple(Offset(w * posX5.value, h * posY5.value), maxDim * scale5.value, c1.copy(alpha = baseAlpha * 0.40f)),
        )

        blobs.forEach { (center, radius, color) ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                center = center,
                radius = radius,
            )
        }

        drawRect(
            brush = if (isDarkTheme) {
                Brush.verticalGradient(
                    0.00f to Color.Transparent,
                    0.50f to Color.Transparent,
                    0.78f to surfaceColor.copy(alpha = 0.42f * alpha),
                    1.00f to surfaceColor.copy(alpha = 0.72f * alpha),
                )
            } else {
                Brush.verticalGradient(
                    0.00f to surfaceColor.copy(alpha = 0.20f * alpha),
                    0.45f to surfaceColor.copy(alpha = 0.14f * alpha),
                    0.78f to surfaceColor.copy(alpha = 0.45f * alpha),
                    1.00f to surfaceColor.copy(alpha = 0.75f * alpha),
                )
            }
        )
    }
}

/**
 * Springy press-down micro-interaction matching Lune's Expressive buttons and pills.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.94f,
    dampingRatio: Float = Spring.DampingRatioMediumBouncy,
    stiffness: Float = Spring.StiffnessMedium,
): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()

    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(scaleDown, dampingRatio, stiffness) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                val animJob = coroutineScope.launch {
                    scale.animateTo(
                        targetValue = scaleDown,
                        animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness),
                    )
                }
                waitForUpOrCancellation()
                animJob.cancel()
                coroutineScope.launch {
                    scale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness),
                    )
                }
            }
        }
}

/**
 * Horizontal swipe gesture on the artwork cover to skip next/previous with spring snap-back.
 */
fun Modifier.songSwipeGestures(
    enabled: Boolean,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
): Modifier = composed {
    if (!enabled) return@composed this

    val coroutineScope = rememberCoroutineScope()
    var dragTranslationX by remember { mutableFloatStateOf(0f) }
    var dragScale by remember { mutableFloatStateOf(1f) }
    val animOffsetX = remember { Animatable(0f) }
    val animScale = remember { Animatable(1f) }
    var isDragging by remember { mutableStateOf(false) }

    this
        .graphicsLayer {
            translationX = if (isDragging) dragTranslationX else animOffsetX.value
            scaleX = if (isDragging) dragScale else animScale.value
            scaleY = if (isDragging) dragScale else animScale.value
        }
        .pointerInput(enabled) {
            var gestureConsumed = false
            detectDragGestures(
                onDragStart = {
                    isDragging = true
                    dragTranslationX = 0f
                    dragScale = 1f
                    gestureConsumed = false
                },
                onDragEnd = {
                    isDragging = false
                    val currentX = dragTranslationX
                    val currentS = dragScale
                    coroutineScope.launch {
                        animOffsetX.snapTo(currentX)
                        animScale.snapTo(currentS)
                        if (gestureConsumed) {
                            animScale.animateTo(0.94f, tween(50))
                        }
                        launch {
                            animOffsetX.animateTo(
                                0f,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                        launch {
                            animScale.animateTo(
                                1f,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                    }
                },
                onDragCancel = {
                    isDragging = false
                    val currentX = dragTranslationX
                    val currentS = dragScale
                    coroutineScope.launch {
                        animOffsetX.snapTo(currentX)
                        animScale.snapTo(currentS)
                        launch {
                            animOffsetX.animateTo(
                                0f,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                        launch {
                            animScale.animateTo(
                                1f,
                                spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                    }
                },
                onDrag = { change, dragAmount ->
                    dragTranslationX = (dragTranslationX + dragAmount.x * 0.35f).coerceIn(-75f, 75f)
                    val absX = abs(dragTranslationX)
                    val absY = abs(dragAmount.y)
                    dragScale = (1f - (absX / 1600f)).coerceIn(0.96f, 1f)

                    if (!gestureConsumed && absX > 45f && absX > absY * 1.2f) {
                        change.consume()
                        gestureConsumed = true
                        if (dragTranslationX < 0f) onNext() else onPrevious()
                    }
                },
            )
        }
}
