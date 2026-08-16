package com.casshole.ui.comparison

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** [projected] bars come from [com.casshole.data.local.InstallmentProjector], not from a parsed invoice. */
data class BarData(val label: String, val value: Double, val projected: Boolean = false)

@Composable
fun BarChart(data: List<BarData>, amountsHidden: Boolean = false, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return

    val maxValue = data.maxOf { it.value }.coerceAtLeast(0.01)
    val barColor = MaterialTheme.colorScheme.primary
    // Projected bars are drawn hollow, so an estimate never reads as a charge
    // that actually landed on an invoice.
    val projectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val averageColor = MaterialTheme.colorScheme.outline
    val labelStyle = TextStyle(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium,
    )
    val average = data.map { it.value }.average()
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val totalWidth = size.width
                val totalHeight = size.height
                val spacing = 12.dp.toPx()
                val labelRoom = 20.dp.toPx()
                val barWidth = (totalWidth - spacing * (data.size + 1)) / data.size

                if (data.size > 1) {
                    val averageY = totalHeight - (average / maxValue * (totalHeight - labelRoom)).toFloat()
                    drawLine(
                        color = averageColor,
                        start = Offset(0f, averageY),
                        end = Offset(totalWidth, averageY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
                    )
                }

                data.forEachIndexed { index, item ->
                    val barHeight = (item.value / maxValue * (totalHeight - labelRoom)).toFloat()
                    val x = spacing + index * (barWidth + spacing)
                    val y = totalHeight - barHeight
                    val corner = CornerRadius(4.dp.toPx())

                    if (item.projected) {
                        drawRoundRect(
                            color = projectedColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = corner,
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))),
                        )
                    } else {
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = corner,
                        )
                    }

                    // Skip the value label when bars get too narrow to hold it -
                    // overlapping numbers are worse than no number.
                    val text = if (amountsHidden) "R$ ••••" else "R$ %.0f".format(item.value)
                    val measured = textMeasurer.measure(text, labelStyle)
                    if (measured.size.width <= barWidth + spacing) {
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                x + barWidth / 2 - measured.size.width / 2,
                                (y - measured.size.height - 2.dp.toPx()).coerceAtLeast(0f),
                            ),
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            data.forEach { item ->
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.projected) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .padding(top = 4.dp),
                )
            }
        }
    }
}
