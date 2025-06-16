package com.example.kaliumapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun CalorieHistoryGraph(
    calorieData: List<Float>, // Data kalori untuk 7 hari
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val width = size.width
        val height = size.height
        val maxCalories = calorieData.maxOrNull()?.takeIf { it > 0 } ?: 1f
        val points = calorieData.mapIndexed { index, calories ->
            val x = (index / (calorieData.size - 1).toFloat()) * width
            val y = height - (calories / maxCalories) * height
            Offset(x, y)
        }

        // Gambar garis
        for (i in 0 until points.size - 1) {
            drawLine(
                color = Color(0xFFFF5733),
                start = points[i],
                end = points[i + 1],
                strokeWidth = 4f
            )
        }

        // Gambar titik
        points.forEach { point ->
            drawCircle(
                color = Color(0xFFFF5733),
                radius = 8f,
                center = point
            )
        }
    }
}