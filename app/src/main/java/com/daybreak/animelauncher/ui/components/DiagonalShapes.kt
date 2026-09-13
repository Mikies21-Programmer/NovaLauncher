package com.daybreak.animelauncher.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density

class BottomLeftTriangleShape(private val sizeRatio: Float = 0.4f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val offset = size.width * sizeRatio
            moveTo(0f, size.height - offset)
            lineTo(0f, size.height)
            lineTo(offset, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

class BottomRightTriangleShape(private val sizeRatio: Float = 0.4f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val offset = size.width * sizeRatio
            moveTo(size.width, size.height - offset)
            lineTo(size.width, size.height)
            lineTo(size.width - offset, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

class DiagonalBarShapeLeft(private val startRatio: Float = 0.4f, private val thicknessRatio: Float = 0.15f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val startOffset = size.width * startRatio
            val thicknessOffset = size.width * thicknessRatio * 1.414f // T / sin(45)
            
            // Bottom edge of the bar (touching the triangle)
            moveTo(0f, size.height - startOffset)
            lineTo(startOffset, size.height)
            
            // Right bottom point to top right
            // We just project the 45 degree line to the top or right edge
            lineTo(size.width, size.height - size.width + startOffset)
            
            // Top edge
            lineTo(size.width, size.height - size.width + startOffset - thicknessOffset)
            
            // Back to left edge
            lineTo(0f, size.height - startOffset - thicknessOffset)
            
            close()
        }
        return Outline.Generic(path)
    }
}

class DiagonalBarShapeRight(private val startRatio: Float = 0.4f, private val thicknessRatio: Float = 0.15f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val startOffset = size.width * startRatio
            val thicknessOffset = size.width * thicknessRatio * 1.414f
            
            moveTo(size.width, size.height - startOffset)
            lineTo(size.width - startOffset, size.height)
            
            lineTo(0f, size.height - size.width + startOffset)
            
            lineTo(0f, size.height - size.width + startOffset - thicknessOffset)
            
            lineTo(size.width, size.height - startOffset - thicknessOffset)
            
            close()
        }
        return Outline.Generic(path)
    }
}
