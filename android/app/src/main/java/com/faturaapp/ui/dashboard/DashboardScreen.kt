package com.faturaapp.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faturaapp.data.local.FaturaComTransacoes
import androidx.compose.material3.HorizontalDivider
import com.faturaapp.ui.theme.BancoBadge
import com.faturaapp.ui.theme.CategoriaIcone

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onEnviarFatura: () -> Unit,
    onComparar: () -> Unit,
    onConfiguracoes: () -> Unit,
    onAbrirFatura: (Long) -> Unit,
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
                    IconButton(onClick = onComparar) {
                        Icon(imageVector = Icons.Filled.BarChart, contentDescription = "Comparar")
                    }
                    IconButton(onClick = onConfiguracoes) {
                        Icon(imageVector = Icons.Filled.Settings, contentDescription = "Configurações")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onEnviarFatura) {
                Icon(Icons.Filled.Add, contentDescription = "Enviar fatura")
            }
        }
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
                        ListaFaturasPorCartao(estadoAtual.faturas, onAbrirFatura)
                    }
                }
            }
        }
    }
}

@Composable
private fun ListaFaturasPorCartao(faturas: List<FaturaComTransacoes>, onAbrirFatura: (Long) -> Unit) {
    val grupos = faturas
        .groupBy { it.banco to it.cartao }
        .toList()
        .sortedBy { (chave, _) -> "${chave.first}${chave.second}" }

    val faturasMaisRecentes = grupos.map { (_, faturasDoGrupo) ->
        faturasDoGrupo.maxBy { it.anoReferencia * 100 + it.mesReferencia }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item(key = "resumo-geral") {
            ResumoGeralCard(faturasMaisRecentes)
        }
        grupos.forEach { (chave, faturasDoGrupo) ->
            val (banco, cartao) = chave
            item(key = "header-$banco-$cartao") {
                val sufixoCartao = if (cartao.isNotBlank()) " (••••$cartao)" else ""
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                ) {
                    BancoBadge(banco = banco, tamanho = 28.dp)
                    Text(
                        text = "${banco.replaceFirstChar { it.uppercase() }}$sufixoCartao",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
            items(
                faturasDoGrupo.sortedByDescending { it.anoReferencia * 100 + it.mesReferencia },
                key = { it.id },
            ) { fatura ->
                FaturaMesRow(fatura, onClick = { onAbrirFatura(fatura.id) })
            }
        }
    }
}

@Composable
private fun ResumoGeralCard(faturasMaisRecentes: List<FaturaComTransacoes>) {
    val transacoes = faturasMaisRecentes.flatMap { it.transacoes }
    val totalGeral = transacoes.sumOf { it.valor }
    val porCategoria = transacoes
        .groupBy { it.categoria }
        .mapValues { (_, itens) -> itens.sumOf { it.valor } }
        .toList()
        .sortedByDescending { (_, total) -> total }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoriaIcone(categoria = categoria, tamanho = 28.dp)
                            Text(
                                text = categoria,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                        Text("R$ %.2f".format(total), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun FaturaMesRow(fatura: FaturaComTransacoes, onClick: () -> Unit) {
    val totalGasto = fatura.transacoes.sumOf { it.valor }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${fatura.mesReferencia}/${fatura.anoReferencia}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "R$ %.2f".format(totalGasto),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${fatura.transacoes.size} transações",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
