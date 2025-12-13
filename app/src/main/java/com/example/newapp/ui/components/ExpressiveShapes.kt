package com.example.newapp.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * A wrapper around RoundedPolygon that implements the Shape interface.
 */
class StarShape(
    private val numVertices: Int = 5,
    private val innerRadiusRatio: Float = 0.5f,
    private val rounding: Float = 0.1f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radius = minOf(size.width, size.height) / 2f
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        
        val polygon = RoundedPolygon.star(
            numVerticesPerRadius = numVertices,
            innerRadius = radius * innerRadiusRatio,
            radius = radius,
            rounding = CornerRounding(radius * rounding),
            innerRounding = CornerRounding(radius * rounding)
        )
        
        val path = polygon.toPath().asComposePath()
        val matrix = androidx.compose.ui.graphics.Matrix()
        matrix.translate(centerX, centerY)
        path.transform(matrix)
        
        return Outline.Generic(path)
    }
}

class WavySquareShape(
    private val period: Int = 10,
    private val amplitude: Float = 0.1f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
         val radius = minOf(size.width, size.height) / 2f
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        
        val polygon = RoundedPolygon.star(
            numVerticesPerRadius = period,
            innerRadius = radius * (1f - amplitude),
            radius = radius,
            rounding = CornerRounding(radius * 0.5f)
        )

        val path = polygon.toPath().asComposePath()
        val matrix = androidx.compose.ui.graphics.Matrix()
        matrix.translate(centerX, centerY)
        path.transform(matrix)
        
        return Outline.Generic(path)
    }
}
