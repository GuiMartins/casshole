package com.moneyhole.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Pets
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

data class CategoryVisual(val color: Color, val icon: ImageVector)

private val visualsByCategory = mapOf(
    "Streaming/Assinaturas" to CategoryVisual(CategoryStreaming, Icons.Filled.Subscriptions),
    "Compras" to CategoryVisual(CategoryShopping, Icons.Filled.ShoppingBag),
    "Alimentação" to CategoryVisual(CategoryFood, Icons.Filled.Restaurant),
    "Transporte" to CategoryVisual(CategoryTransport, Icons.Filled.DirectionsCar),
    "Saúde" to CategoryVisual(CategoryHealth, Icons.Filled.LocalHospital),
    "Educação" to CategoryVisual(CategoryEducation, Icons.Filled.School),
    "Contas/Serviços" to CategoryVisual(CategoryBills, Icons.Filled.Receipt),
    "Pets" to CategoryVisual(CategoryPets, Icons.Filled.Pets),
)

private val defaultVisual = CategoryVisual(CategoryOther, Icons.Filled.Category)

fun categoryVisual(category: String): CategoryVisual =
    visualsByCategory[category] ?: defaultVisual

@Composable
fun CategoryIcon(
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
) {
    val visual = categoryVisual(category)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(visual.color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = visual.icon,
            contentDescription = category,
            tint = visual.color,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}
