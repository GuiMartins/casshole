package com.faturaapp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val coresPorBanco = mapOf(
    "nubank" to BancoNubank,
    "itau" to BancoItau,
    "mercadopago" to BancoMercadoPago,
    "bradesco" to BancoBradesco,
)

fun corDoBanco(banco: String): Color = coresPorBanco[banco.lowercase()] ?: BancoPadrao

@Composable
fun BancoBadge(banco: String, modifier: Modifier = Modifier, tamanho: Dp = 36.dp) {
    val cor = corDoBanco(banco)
    Box(
        modifier = modifier
            .size(tamanho)
            .clip(CircleShape)
            .background(cor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = banco.take(1).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
