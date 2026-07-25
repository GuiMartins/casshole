package com.faturaapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class TelaPrincipal {
    INICIO,
    FATURAS_POR_CARTAO,
    COMPARAR,
    CONFIGURACOES,
}

@Composable
fun BarraNavegacaoFlutuante(
    telaAtual: TelaPrincipal,
    onIrParaInicio: () -> Unit,
    onVerPorCartao: () -> Unit,
    onComparar: () -> Unit,
    onConfiguracoes: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(6.dp),
        ) {
            ItemNavegacao(
                icone = Icons.Filled.Home,
                descricao = "Início",
                selecionado = telaAtual == TelaPrincipal.INICIO,
                onClick = onIrParaInicio,
            )
            ItemNavegacao(
                icone = Icons.Filled.CreditCard,
                descricao = "Faturas por cartão",
                selecionado = telaAtual == TelaPrincipal.FATURAS_POR_CARTAO,
                onClick = onVerPorCartao,
            )
            ItemNavegacao(
                icone = Icons.Filled.BarChart,
                descricao = "Comparar",
                selecionado = telaAtual == TelaPrincipal.COMPARAR,
                onClick = onComparar,
            )
            ItemNavegacao(
                icone = Icons.Filled.Settings,
                descricao = "Configurações",
                selecionado = telaAtual == TelaPrincipal.CONFIGURACOES,
                onClick = onConfiguracoes,
            )
        }
    }
}

@Composable
private fun ItemNavegacao(
    icone: ImageVector,
    descricao: String,
    selecionado: Boolean,
    onClick: () -> Unit,
) {
    val corFundo = if (selecionado) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val corIcone = if (selecionado) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .padding(2.dp)
            .clip(CircleShape)
            .background(corFundo)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Icon(imageVector = icone, contentDescription = descricao, tint = corIcone)
    }
}
