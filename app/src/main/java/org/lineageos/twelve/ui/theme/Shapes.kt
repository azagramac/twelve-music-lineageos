/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.twelve.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

/**
 * Container corner shape scale for Material 3 Expressive.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val TwelveExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
    largeIncreased = RoundedCornerShape(24.dp),
    extraLargeIncreased = RoundedCornerShape(32.dp),
    extraExtraLarge = RoundedCornerShape(40.dp),
)

/**
 * Expanded 35+ geometric elastic shape catalog from Material 3 Expressive ([MaterialShapes])
 * ready for Shape Morphing across artwork, media controls, and Hi-Fi badges.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object TwelveMorphShapes {
    val Circle: RoundedPolygon get() = MaterialShapes.Circle
    val Square: RoundedPolygon get() = MaterialShapes.Square
    val Slanted: RoundedPolygon get() = MaterialShapes.Slanted
    val Arch: RoundedPolygon get() = MaterialShapes.Arch
    val Fan: RoundedPolygon get() = MaterialShapes.Fan
    val Arrow: RoundedPolygon get() = MaterialShapes.Arrow
    val SemiCircle: RoundedPolygon get() = MaterialShapes.SemiCircle
    val Oval: RoundedPolygon get() = MaterialShapes.Oval
    val Pill: RoundedPolygon get() = MaterialShapes.Pill
    val Triangle: RoundedPolygon get() = MaterialShapes.Triangle
    val Diamond: RoundedPolygon get() = MaterialShapes.Diamond
    val ClamShell: RoundedPolygon get() = MaterialShapes.ClamShell
    val Pentagon: RoundedPolygon get() = MaterialShapes.Pentagon
    val Gem: RoundedPolygon get() = MaterialShapes.Gem
    val Sunny: RoundedPolygon get() = MaterialShapes.Sunny
    val VerySunny: RoundedPolygon get() = MaterialShapes.VerySunny
    val Cookie4Sided: RoundedPolygon get() = MaterialShapes.Cookie4Sided
    val Cookie6Sided: RoundedPolygon get() = MaterialShapes.Cookie6Sided
    val Cookie7Sided: RoundedPolygon get() = MaterialShapes.Cookie7Sided
    val Cookie9Sided: RoundedPolygon get() = MaterialShapes.Cookie9Sided
    val Cookie12Sided: RoundedPolygon get() = MaterialShapes.Cookie12Sided
    val Ghostish: RoundedPolygon get() = MaterialShapes.Ghostish
    val Clover4Leaf: RoundedPolygon get() = MaterialShapes.Clover4Leaf
    val Clover8Leaf: RoundedPolygon get() = MaterialShapes.Clover8Leaf
    val Burst: RoundedPolygon get() = MaterialShapes.Burst
    val SoftBurst: RoundedPolygon get() = MaterialShapes.SoftBurst
    val Boom: RoundedPolygon get() = MaterialShapes.Boom
    val SoftBoom: RoundedPolygon get() = MaterialShapes.SoftBoom
    val Flower: RoundedPolygon get() = MaterialShapes.Flower
    val Puffy: RoundedPolygon get() = MaterialShapes.Puffy
    val PuffyDiamond: RoundedPolygon get() = MaterialShapes.PuffyDiamond
    val PixelCircle: RoundedPolygon get() = MaterialShapes.PixelCircle
    val PixelTriangle: RoundedPolygon get() = MaterialShapes.PixelTriangle
    val Bun: RoundedPolygon get() = MaterialShapes.Bun
    val Heart: RoundedPolygon get() = MaterialShapes.Heart

    val all: List<RoundedPolygon> by lazy {
        listOf(
            Circle,
            Square,
            Slanted,
            Arch,
            Fan,
            Arrow,
            SemiCircle,
            Oval,
            Pill,
            Triangle,
            Diamond,
            ClamShell,
            Pentagon,
            Gem,
            Sunny,
            VerySunny,
            Cookie4Sided,
            Cookie6Sided,
            Cookie7Sided,
            Cookie9Sided,
            Cookie12Sided,
            Ghostish,
            Clover4Leaf,
            Clover8Leaf,
            Burst,
            SoftBurst,
            Boom,
            SoftBoom,
            Flower,
            Puffy,
            PuffyDiamond,
            PixelCircle,
            PixelTriangle,
            Bun,
            Heart,
        )
    }
}

/**
 * [Shape] backed by an androidx.graphics.shapes [Morph] interpolated at [progress] in `[0f, 1f]`.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class MorphPolygonShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = morph.toPath(progress = progress.coerceIn(0f, 1f))
        val matrix = Matrix().apply {
            scale(x = size.width, y = size.height)
        }
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

/**
 * Remembers and animates a springy [MorphPolygonShape] between [from] and [to] driven by
 * [MaterialTheme.motionScheme]'s expressive spatial spring physics.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun rememberMorphingShape(
    from: RoundedPolygon,
    to: RoundedPolygon,
    morphed: Boolean,
): Shape {
    val morph = remember(from, to) { Morph(from, to) }
    val progress by animateFloatAsState(
        targetValue = if (morphed) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "MorphPolygonProgress",
    )
    return remember(morph, progress) {
        MorphPolygonShape(morph, progress)
    }
}
