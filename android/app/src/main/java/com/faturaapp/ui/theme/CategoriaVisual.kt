package com.faturaapp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class CategoriaVisual(val cor: Color, val icone: ImageVector)

private val visuaisPorCategoria = mapOf(
    "Streaming/Assinaturas" to CategoriaVisual(CategoriaStreaming, Icons.Filled.Subscriptions),
    "Compras" to CategoriaVisual(CategoriaCompras, Icons.Filled.ShoppingBag),
    "Alimentação" to CategoriaVisual(CategoriaAlimentacao, Icons.Filled.Restaurant),
    "Transporte" to CategoriaVisual(CategoriaTransporte, Icons.Filled.DirectionsCar),
    "Saúde" to CategoriaVisual(CategoriaSaude, Icons.Filled.LocalHospital),
    "Educação" to CategoriaVisual(CategoriaEducacao, Icons.Filled.School),
    "Contas/Serviços" to CategoriaVisual(CategoriaContas, Icons.Filled.Receipt),
)

private val visualPadrao = CategoriaVisual(CategoriaOutros, Icons.Filled.Category)

fun visualDaCategoria(categoria: String): CategoriaVisual =
    visuaisPorCategoria[categoria] ?: visualPadrao

@Composable
fun CategoriaIcone(
    categoria: String,
    modifier: Modifier = Modifier,
    tamanho: Dp = 32.dp,
) {
    val visual = visualDaCategoria(categoria)
    Box(
        modifier = modifier
            .size(tamanho)
            .clip(CircleShape)
            .background(visual.cor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = visual.icone,
            contentDescription = categoria,
            tint = visual.cor,
            modifier = Modifier.size(tamanho * 0.55f),
        )
    }
}
