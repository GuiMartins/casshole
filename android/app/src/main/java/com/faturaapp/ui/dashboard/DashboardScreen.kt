package com.faturaapp.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faturaapp.data.model.Fatura

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onEnviarFatura: () -> Unit,
    onComparar: () -> Unit,
    onSenhas: () -> Unit,
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
                title = { Text("Fatura App") },
                actions = {
                    TextButton(onClick = onSenhas) {
                        Text("Senhas")
                    }
                    TextButton(onClick = onComparar) {
                        Text("Comparar")
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
                        ListaFaturas(estadoAtual.faturas)
                    }
                }
            }
        }
    }
}

@Composable
private fun ListaFaturas(faturas: List<Fatura>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(faturas) { fatura ->
            FaturaCard(fatura)
        }
    }
}

@Composable
private fun FaturaCard(fatura: Fatura) {
    val totalGasto = fatura.transacoes.sumOf { it.valor }
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            val sufixoCartao = if (fatura.cartao.isNotBlank()) " (••••${fatura.cartao})" else ""
            Text(
                text = "${fatura.banco.replaceFirstChar { it.uppercase() }}$sufixoCartao — ${fatura.mes_referencia}/${fatura.ano_referencia}",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Total: R$ %.2f".format(totalGasto),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "${fatura.transacoes.size} transações",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
