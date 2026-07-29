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
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BarData(val label: String, val value: Double)

@Composable
fun BarChart(data: List<BarData>, amountsHidden: Boolean = false, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return

    val maxValue = data.maxOf { it.value }.coerceAtLeast(0.01)
    val barColor = MaterialTheme.colorScheme.primary
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
                val barWidth = (totalWidth - spacing * (data.size + 1)) / data.size

                data.forEachIndexed { index, item ->
                    val barHeight = (item.value / maxValue * (totalHeight - 24.dp.toPx())).toFloat()
                    val x = spacing + index * (barWidth + spacing)
                    val y = totalHeight - barHeight

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )

                    drawIntoCanvas {
                        val text = if (amountsHidden) "R$ ••••" else "R$ %.0f".format(item.value)
                        val result = textMeasurer.measure(text)
                        it.nativeCanvas.drawText(
                            text,
                            x + barWidth / 2 - result.size.width / 2,
                            y - 4.dp.toPx(),
                            android.graphics.Paint().apply {
                                textSize = 10.sp.toPx()
                                color = android.graphics.Color.GRAY
                            },
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .padding(top = 4.dp),
                )
            }
        }
    }
}
