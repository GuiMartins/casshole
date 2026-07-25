package com.moneyhole.ui.theme

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

private val colorsByBank = mapOf(
    "nubank" to BankNubank,
    "itau" to BankItau,
    "mercadopago" to BankMercadoPago,
    "bradesco" to BankBradesco,
)

fun bankColor(bank: String): Color = colorsByBank[bank.lowercase()] ?: BankDefault

@Composable
fun BankBadge(bank: String, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    val color = bankColor(bank)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = bank.take(1).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
