package com.faturaapp.ui.comparacao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faturaapp.data.model.ResumoMensal

@Composable
fun ComparacaoScreen(viewModel: ComparacaoViewModel = viewModel()) {
    val periodosState by viewModel.periodosState.collectAsState()
    val selecionados by viewModel.selecionados.collectAsState()
    val comparacaoState by viewModel.comparacaoState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                            BarraDado("%02d/%d".format(it.mes_referencia, it.ano_referencia), it.total_gasto)
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    )
                    estado.comparacao.variacao_percentual_total?.let { variacao ->
                        val sinal = if (variacao >= 0) "+" else ""
                        Text(
                            text = "Variação do primeiro ao último mês selecionado: $sinal%.1f%%".format(variacao),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (variacao > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
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
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = "%02d/%d — Total: R$ %.2f".format(
                resumo.mes_referencia, resumo.ano_referencia, resumo.total_gasto
            ),
            style = MaterialTheme.typography.titleMedium,
        )
        resumo.por_categoria.forEach { categoria ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(categoria.categoria, style = MaterialTheme.typography.bodyMedium)
                Text("R$ %.2f".format(categoria.total), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
