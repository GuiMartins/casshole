package com.faturaapp.ui.faturadetalhe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faturaapp.data.model.Transacao

@Composable
fun FaturaDetalheScreen(viewModel: FaturaDetalheViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val categorias by viewModel.categoriasDisponiveis.collectAsState()

    var transacaoEmEdicao by remember { mutableStateOf<Transacao?>(null) }

    when (val estado = state) {
        is FaturaDetalheState.Carregando -> CircularProgressIndicator(
            modifier = Modifier.padding(24.dp)
        )
        is FaturaDetalheState.Erro -> Text(
            text = estado.mensagem,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(24.dp),
        )
        is FaturaDetalheState.Carregado -> ConteudoFaturaDetalhe(
            estado = estado,
            onTransacaoClick = { transacaoEmEdicao = it },
        )
    }

    transacaoEmEdicao?.let { transacao ->
        DialogEditarCategoria(
            transacao = transacao,
            categorias = categorias,
            onConfirmar = { novaCategoria ->
                viewModel.atualizarCategoria(transacao.id, novaCategoria)
                transacaoEmEdicao = null
            },
            onCancelar = { transacaoEmEdicao = null },
        )
    }
}

@Composable
private fun DialogEditarCategoria(
    transacao: Transacao,
    categorias: List<String>,
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit,
) {
    var selecionada by remember(transacao.id) { mutableStateOf(transacao.categoria) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Categoria de \"${transacao.descricao}\"") },
        text = {
            Column {
                categorias.forEach { categoria ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selecionada = categoria },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = categoria == selecionada,
                            onClick = { selecionada = categoria },
                        )
                        Text(categoria)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmar(selecionada) }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun ConteudoFaturaDetalhe(
    estado: FaturaDetalheState.Carregado,
    onTransacaoClick: (Transacao) -> Unit,
) {
    val fatura = estado.fatura
    val totalGasto = fatura.transacoes.sumOf { it.valor }
    val sufixoCartao = if (fatura.cartao.isNotBlank()) " (••••${fatura.cartao})" else ""
    val titulares = fatura.transacoes.map { it.titular }.filter { it.isNotBlank() }.distinct()
    val expandido = remember { mutableStateMapOf<String, Boolean>() }

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

        if (titulares.size > 1) {
            titulares.forEach { titular ->
                val transacoesDoTitular = fatura.transacoes.filter { it.titular == titular }
                val estaExpandido = expandido[titular] ?: false

                item {
                    TitularHeader(
                        titular = titular,
                        quantidade = transacoesDoTitular.size,
                        total = transacoesDoTitular.sumOf { it.valor },
                        expandido = estaExpandido,
                        onClick = { expandido[titular] = !estaExpandido },
                    )
                }
                if (estaExpandido) {
                    items(transacoesDoTitular) { transacao ->
                        TransacaoRow(transacao, onClick = { onTransacaoClick(transacao) })
                    }
                }
            }
        } else {
            items(fatura.transacoes) { transacao ->
                TransacaoRow(transacao, onClick = { onTransacaoClick(transacao) })
            }
        }
    }
}

@Composable
private fun TitularHeader(
    titular: String,
    quantidade: Int,
    total: Double,
    expandido: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (expandido) "▾" else "▸",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = "$titular ($quantidade)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = "R$ %.2f".format(total),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TransacaoRow(transacao: Transacao, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .clickable(onClick = onClick),
    ) {
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
            val cidadeTexto = if (transacao.cidade.isNotBlank()) " • ${transacao.cidade}" else ""
            Text(
                text = "${transacao.data} • ${transacao.categoria}$parcelaTexto$cidadeTexto",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
