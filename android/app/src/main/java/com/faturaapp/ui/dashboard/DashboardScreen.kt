package com.faturaapp.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faturaapp.categorizer.CATEGORIAS_DISPONIVEIS
import com.faturaapp.data.local.FaturaComTransacoes
import com.faturaapp.data.local.entity.TransacaoEntity
import androidx.compose.material3.HorizontalDivider
import com.faturaapp.ui.components.TelaAdaptavel
import com.faturaapp.ui.theme.CategoriaIcone

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onEnviarFatura: () -> Unit,
    viewModel: DashboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    val onResumeAction by rememberUpdatedState(viewModel::carregarFaturas)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onResumeAction()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Fatura App",
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onEnviarFatura) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Enviar fatura")
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val estadoAtual = state) {
                is DashboardState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                is DashboardState.Erro -> Text(
                    text = estadoAtual.mensagem,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
                is DashboardState.Carregado -> {
                    if (estadoAtual.faturas.isEmpty()) {
                        Text(
                            text = "Nenhuma fatura enviada ainda",
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else {
                        ResumoGeralConteudo(
                            faturas = estadoAtual.faturas,
                            onAtualizarCategoria = viewModel::atualizarCategoria,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumoGeralConteudo(
    faturas: List<FaturaComTransacoes>,
    onAtualizarCategoria: (Long, String) -> Unit,
) {
    val faturasMaisRecentes = faturas
        .groupBy { it.banco to it.cartao }
        .values
        .map { faturasDoGrupo -> faturasDoGrupo.maxBy { it.anoReferencia * 100 + it.mesReferencia } }

    var categoriaSelecionada by remember { mutableStateOf<String?>(null) }
    var transacaoEmEdicao by remember { mutableStateOf<TransacaoEntity?>(null) }

    TelaAdaptavel {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            ResumoGeralCard(faturasMaisRecentes, onCategoriaClick = { categoriaSelecionada = it })
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    categoriaSelecionada?.let { categoria ->
        val transacoesDaCategoria = faturasMaisRecentes
            .flatMap { fatura -> fatura.transacoes.map { it to fatura } }
            .filter { (transacao, _) -> transacao.categoria == categoria }
            .sortedByDescending { (transacao, _) -> transacao.valor }

        DialogTransacoesCategoria(
            categoria = categoria,
            transacoes = transacoesDaCategoria,
            onDismiss = { categoriaSelecionada = null },
            onTransacaoClick = { transacaoEmEdicao = it },
        )
    }

    transacaoEmEdicao?.let { transacao ->
        DialogEditarCategoria(
            transacao = transacao,
            categorias = CATEGORIAS_DISPONIVEIS,
            onConfirmar = { novaCategoria ->
                onAtualizarCategoria(transacao.id, novaCategoria)
                transacaoEmEdicao = null
            },
            onCancelar = { transacaoEmEdicao = null },
        )
    }
}

@Composable
private fun DialogTransacoesCategoria(
    categoria: String,
    transacoes: List<Pair<TransacaoEntity, FaturaComTransacoes>>,
    onDismiss: () -> Unit,
    onTransacaoClick: (TransacaoEntity) -> Unit,
) {
    val total = transacoes.sumOf { (transacao, _) -> transacao.valor }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoriaIcone(categoria = categoria, tamanho = 28.dp)
                Text(categoria, modifier = Modifier.padding(start = 8.dp))
            }
        },
        text = {
            Column {
                Text(
                    text = "${transacoes.size} transações • R$ %.2f".format(total),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(transacoes) { (transacao, fatura) ->
                        TransacaoResumoRow(
                            transacao = transacao,
                            fatura = fatura,
                            onClick = { onTransacaoClick(transacao) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        },
    )
}

@Composable
private fun TransacaoResumoRow(transacao: TransacaoEntity, fatura: FaturaComTransacoes, onClick: () -> Unit) {
    val sufixoCartao = if (fatura.cartao.isNotBlank()) " ••••${fatura.cartao}" else ""
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = transacao.descricao,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "R$ %.2f".format(transacao.valor),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            text = "${transacao.data} • ${fatura.banco.replaceFirstChar { it.uppercase() }}$sufixoCartao",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider()
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

@Composable
private fun ResumoGeralCard(faturasMaisRecentes: List<FaturaComTransacoes>, onCategoriaClick: (String) -> Unit) {
    val transacoes = faturasMaisRecentes.flatMap { it.transacoes }
    val totalGeral = transacoes.sumOf { it.valor }
    val porCategoria = transacoes
        .groupBy { it.categoria }
        .mapValues { (_, itens) -> itens.sumOf { it.valor } }
        .toList()
        .sortedByDescending { (_, total) -> total }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Resumo geral",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Mês mais recente de cada cartão",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = "R$ %.2f".format(totalGeral),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            if (porCategoria.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                porCategoria.forEach { (categoria, total) ->
                    CategoriaResumoRow(
                        categoria = categoria,
                        total = total,
                        onClick = { onCategoriaClick(categoria) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoriaResumoRow(categoria: String, total: Double, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            ) {
                CategoriaIcone(categoria = categoria, tamanho = 28.dp)
                Text(
                    text = categoria,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Text(
                text = "R$ %.2f".format(total),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
        }
    }
}
