package com.moneyhole.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

data class PieSlice(val label: String, val value: Double, val color: Color)

/**
 * Simple donut chart, no legend of its own - callers render the interactive
 * per-category rows below it (kept clickable, same as the numbers-only view).
 */
@Composable
fun PieChart(data: List<PieSlice>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return

    val total = data.sumOf { it.value }.coerceAtLeast(0.01)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = modifier.fillMaxWidth().height(180.dp).padding(vertical = 8.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val strokeWidth = 28.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = androidx.compose.ui.geometry.Offset(
                (size.width - diameter) / 2,
                (size.height - diameter) / 2,
            )
            val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )

            var startAngle = -90f
            data.forEach { slice ->
                val sweep = (slice.value / total * 360f).toFloat()
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )
                startAngle += sweep
            }
        }
    }
}
