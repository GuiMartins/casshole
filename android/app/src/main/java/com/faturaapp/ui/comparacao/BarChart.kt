package com.faturaapp.ui.comparacao

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

data class BarraDado(val rotulo: String, val valor: Double)

@Composable
fun GraficoBarras(dados: List<BarraDado>, modifier: Modifier = Modifier) {
    if (dados.isEmpty()) return

    val valorMaximo = dados.maxOf { it.valor }.coerceAtLeast(0.01)
    val corBarra = MaterialTheme.colorScheme.primary
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val larguraTotal = size.width
                val alturaTotal = size.height
                val espacamento = 12.dp.toPx()
                val larguraBarra = (larguraTotal - espacamento * (dados.size + 1)) / dados.size

                dados.forEachIndexed { index, dado ->
                    val alturaBarra = (dado.valor / valorMaximo * (alturaTotal - 24.dp.toPx())).toFloat()
                    val x = espacamento + index * (larguraBarra + espacamento)
                    val y = alturaTotal - alturaBarra

                    drawRoundRect(
                        color = corBarra,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(larguraBarra, alturaBarra),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )

                    drawIntoCanvas {
                        val texto = "R$ %.0f".format(dado.valor)
                        val resultado = textMeasurer.measure(texto)
                        it.nativeCanvas.drawText(
                            texto,
                            x + larguraBarra / 2 - resultado.size.width / 2,
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
            dados.forEach { dado ->
                Text(
                    text = dado.rotulo,
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
