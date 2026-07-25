package com.faturaapp.ui.comparacao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import com.faturaapp.data.local.ResumoMensal
import com.faturaapp.ui.theme.CategoriaIcone

@Composable
fun ComparacaoScreen(viewModel: ComparacaoViewModel = viewModel()) {
    val periodosState by viewModel.periodosState.collectAsState()
    val selecionados by viewModel.selecionados.collectAsState()
    val comparacaoState by viewModel.comparacaoState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            Text(
                text = "Comparar meses",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        when (val estado = periodosState) {
            is PeriodosState.Carregando -> item {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
            is PeriodosState.Erro -> item {
                Text(estado.mensagem, color = MaterialTheme.colorScheme.error)
            }
            is PeriodosState.Disponivel -> {
                if (estado.periodos.isEmpty()) {
                    item { Text("Nenhuma fatura enviada ainda") }
                } else {
                    items(estado.periodos) { periodo ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = periodo in selecionados,
                                onCheckedChange = { viewModel.alternarSelecao(periodo) },
                            )
                            Text(periodo.toString())
                        }
                    }
                    item {
                        Button(
                            onClick = viewModel::comparar,
                            enabled = comparacaoState != ComparacaoUiState.Comparando,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                        ) {
                            Text("Comparar")
                        }
                    }
                }
            }
        }

        when (val estado = comparacaoState) {
            is ComparacaoUiState.Comparando -> item {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
            is ComparacaoUiState.Erro -> item {
                Text(estado.mensagem, color = MaterialTheme.colorScheme.error)
            }
            is ComparacaoUiState.Resultado -> {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    GraficoBarras(
                        dados = estado.comparacao.meses.map {
                            BarraDado("%02d/%d".format(it.mesReferencia, it.anoReferencia), it.totalGasto)
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    )
                    estado.comparacao.variacaoPercentualTotal?.let { variacao ->
                        val sinal = if (variacao >= 0) "+" else ""
                        Text(
                            text = "Variação do primeiro ao último mês selecionado: $sinal%.1f%%".format(variacao),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (variacao > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(bottom = 16.dp),
                        )
                    }
                }
                items(estado.comparacao.meses) { mes ->
                    ResumoMensalCard(mes)
                }
            }
            is ComparacaoUiState.Idle -> {}
        }
    }
}

@Composable
private fun ResumoMensalCard(resumo: ResumoMensal) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "%02d/%d — Total: R$ %.2f".format(
                    resumo.mesReferencia, resumo.anoReferencia, resumo.totalGasto
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            resumo.porCategoria.forEach { categoria ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        CategoriaIcone(categoria = categoria.categoria, tamanho = 26.dp)
                        Text(
                            text = categoria.categoria,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Text("R$ %.2f".format(categoria.total), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
