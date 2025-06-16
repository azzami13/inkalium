package com.example.kaliumapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CircularProgress(progress: Float, maxProgress: Float, size: Int, color: Color) {
    Canvas(
        modifier = Modifier.size(size.dp)
    ) {
        val strokeWidth = 16f
        val radius = size / 2f

        drawRoundRect(
            color = Color.LightGray,
            size = Size(radius * 2, radius * 2),
            cornerRadius = CornerRadius(strokeWidth, strokeWidth)
        )

        drawRoundRect(
            color = color,
            size = Size(radius * 2 * (progress / maxProgress), radius * 2),
            cornerRadius = CornerRadius(strokeWidth, strokeWidth)
        )
    }
}
