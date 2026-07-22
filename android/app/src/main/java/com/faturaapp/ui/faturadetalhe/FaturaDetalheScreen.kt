package com.faturaapp.ui.faturadetalhe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faturaapp.data.model.Transacao

@Composable
fun FaturaDetalheScreen(viewModel: FaturaDetalheViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    when (val estado = state) {
        is FaturaDetalheState.Carregando -> CircularProgressIndicator(
            modifier = Modifier.padding(24.dp)
        )
        is FaturaDetalheState.Erro -> Text(
            text = estado.mensagem,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(24.dp),
        )
        is FaturaDetalheState.Carregado -> ConteudoFaturaDetalhe(estado)
    }
}

@Composable
private fun ConteudoFaturaDetalhe(estado: FaturaDetalheState.Carregado) {
    val fatura = estado.fatura
    val totalGasto = fatura.transacoes.sumOf { it.valor }
    val sufixoCartao = if (fatura.cartao.isNotBlank()) " (••••${fatura.cartao})" else ""

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        item {
            Text(
                text = "${fatura.banco.replaceFirstChar { it.uppercase() }}$sufixoCartao",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "${fatura.mes_referencia}/${fatura.ano_referencia} — Total: R$ %.2f".format(totalGasto),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            Text(text = "Por categoria", style = MaterialTheme.typography.titleSmall)
            estado.porCategoria.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(item.categoria, style = MaterialTheme.typography.bodyMedium)
                    Text("R$ %.2f".format(item.total), style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            Text(
                text = "Transações (${fatura.transacoes.size})",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        items(fatura.transacoes) { transacao ->
            TransacaoRow(transacao)
        }
    }
}

@Composable
private fun TransacaoRow(transacao: Transacao) {
    Card(modifier = Modifier.padding(bottom = 8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = transacao.descricao,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "R$ %.2f".format(transacao.valor),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            val parcelaTexto = if (transacao.parcela_atual != null && transacao.parcela_total != null) {
                " • Parcela ${transacao.parcela_atual}/${transacao.parcela_total}"
            } else ""
            Text(
                text = "${transacao.data} • ${transacao.categoria}$parcelaTexto",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
