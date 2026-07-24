package com.faturaapp.ui.faturadetalhe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.faturaapp.data.local.entity.TransacaoEntity
import com.faturaapp.ui.theme.CategoriaIcone
import com.faturaapp.ui.theme.visualDaCategoria

@Composable
fun FaturaDetalheScreen(viewModel: FaturaDetalheViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val categorias by viewModel.categoriasDisponiveis.collectAsState()

    var transacaoEmEdicao by remember { mutableStateOf<TransacaoEntity?>(null) }

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
    transacao: TransacaoEntity,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConteudoFaturaDetalhe(
    estado: FaturaDetalheState.Carregado,
    onTransacaoClick: (TransacaoEntity) -> Unit,
) {
    val fatura = estado.fatura
    val totalGasto = fatura.transacoes.sumOf { it.valor }
    val sufixoCartao = if (fatura.cartao.isNotBlank()) " (••••${fatura.cartao})" else ""
    val titulares = fatura.transacoes.map { it.titular }.filter { it.isNotBlank() }.distinct()
    val categorias = fatura.transacoes.map { it.categoria }.distinct().sorted()
    val cartoesDaLinha = fatura.transacoes.map { it.cartao }.filter { it.isNotBlank() }.distinct().sorted()
    val expandido = remember { mutableStateMapOf<String, Boolean>() }

    var textoBusca by remember { mutableStateOf("") }
    var categoriaFiltro by remember { mutableStateOf<String?>(null) }
    var titularFiltro by remember { mutableStateOf<String?>(null) }
    var cartaoFiltro by remember { mutableStateOf<String?>(null) }

    val filtrosAtivos = textoBusca.isNotBlank() || categoriaFiltro != null ||
        titularFiltro != null || cartaoFiltro != null

    val transacoesFiltradas = fatura.transacoes.filter { transacao ->
        (textoBusca.isBlank() ||
            transacao.descricao.contains(textoBusca, ignoreCase = true) ||
            transacao.cidade.contains(textoBusca, ignoreCase = true)) &&
            (categoriaFiltro == null || transacao.categoria == categoriaFiltro) &&
            (titularFiltro == null || transacao.titular == titularFiltro) &&
            (cartaoFiltro == null || transacao.cartao == cartaoFiltro)
    }

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
                text = "${fatura.mesReferencia}/${fatura.anoReferencia} — Total: R$ %.2f".format(totalGasto),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            Text(text = "Por categoria", style = MaterialTheme.typography.titleSmall)
            estado.porCategoria.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoriaIcone(categoria = item.categoria, tamanho = 26.dp)
                        Text(
                            text = item.categoria,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Text("R$ %.2f".format(item.total), style = MaterialTheme.typography.bodyMedium)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            OutlinedTextField(
                value = textoBusca,
                onValueChange = { textoBusca = it },
                label = { Text("Buscar por descrição ou cidade") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )

            if (categorias.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categorias.forEach { categoria ->
                        FilterChip(
                            selected = categoriaFiltro == categoria,
                            onClick = {
                                categoriaFiltro = if (categoriaFiltro == categoria) null else categoria
                            },
                            label = { Text(categoria) },
                            leadingIcon = {
                                Icon(
                                    imageVector = visualDaCategoria(categoria).icone,
                                    contentDescription = null,
                                    tint = visualDaCategoria(categoria).cor,
                                    modifier = Modifier.padding(2.dp),
                                )
                            },
                        )
                    }
                }
            }

            if (titulares.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    titulares.forEach { titular ->
                        FilterChip(
                            selected = titularFiltro == titular,
                            onClick = {
                                titularFiltro = if (titularFiltro == titular) null else titular
                            },
                            label = { Text(titular) },
                        )
                    }
                }
            }

            if (cartoesDaLinha.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    cartoesDaLinha.forEach { cartao ->
                        FilterChip(
                            selected = cartaoFiltro == cartao,
                            onClick = {
                                cartaoFiltro = if (cartaoFiltro == cartao) null else cartao
                            },
                            label = { Text("••••$cartao") },
                        )
                    }
                }
            }

            if (filtrosAtivos) {
                TextButton(
                    onClick = {
                        textoBusca = ""
                        categoriaFiltro = null
                        titularFiltro = null
                        cartaoFiltro = null
                    },
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    Text("Limpar filtros")
                }
            }

            val textoContador = if (filtrosAtivos) {
                "Transações (${transacoesFiltradas.size} de ${fatura.transacoes.size})"
            } else {
                "Transações (${fatura.transacoes.size})"
            }
            Text(
                text = textoContador,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
        }

        if (titulares.size > 1) {
            titulares.forEach { titular ->
                val transacoesDoTitular = transacoesFiltradas.filter { it.titular == titular }
                if (transacoesDoTitular.isEmpty()) return@forEach
                val estaExpandido = filtrosAtivos || (expandido[titular] ?: false)

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
            items(transacoesFiltradas) { transacao ->
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
            Icon(
                imageVector = if (expandido) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp),
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
private fun TransacaoRow(transacao: TransacaoEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            CategoriaIcone(categoria = transacao.categoria, tamanho = 36.dp)
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
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
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                val parcelaTexto = if (transacao.parcelaAtual != null && transacao.parcelaTotal != null) {
                    " • Parcela ${transacao.parcelaAtual}/${transacao.parcelaTotal}"
                } else ""
                val cidadeTexto = if (transacao.cidade.isNotBlank()) " • ${transacao.cidade}" else ""
                Text(
                    text = "${transacao.data} • ${transacao.categoria}$parcelaTexto$cidadeTexto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
