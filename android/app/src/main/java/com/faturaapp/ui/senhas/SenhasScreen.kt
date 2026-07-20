package com.faturaapp.ui.senhas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faturaapp.data.model.SenhaPadrao

@Composable
fun SenhasScreen(viewModel: SenhasViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val erroAcao by viewModel.erroAcao.collectAsState()

    var novaSenha by remember { mutableStateOf("") }
    var novaDescricao by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Senhas padrão de PDF",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Cadastre senhas (ex: dígitos do CPF) para o app tentar abrir " +
                "faturas protegidas automaticamente, sem precisar digitar toda vez.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )

        OutlinedTextField(
            value = novaSenha,
            onValueChange = { novaSenha = it },
            label = { Text("Senha") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = novaDescricao,
            onValueChange = { novaDescricao = it },
            label = { Text("Descrição (opcional, ex: CPF 5 dígitos)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )

        erroAcao?.let { mensagem ->
            Text(
                text = mensagem,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Button(
            onClick = {
                viewModel.adicionar(novaSenha, novaDescricao)
                novaSenha = ""
                novaDescricao = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
        ) {
            Text("Adicionar senha")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        when (val estado = state) {
            is SenhasState.Carregando -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            is SenhasState.Erro -> Text(estado.mensagem, color = MaterialTheme.colorScheme.error)
            is SenhasState.Carregado -> {
                if (estado.senhas.isEmpty()) {
                    Text("Nenhuma senha cadastrada ainda")
                } else {
                    ListaSenhas(estado.senhas, onRemover = viewModel::remover)
                }
            }
        }
    }
}

@Composable
private fun ListaSenhas(senhas: List<SenhaPadrao>, onRemover: (Int) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(senhas) { senha ->
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("•".repeat(senha.valor.length), style = MaterialTheme.typography.titleMedium)
                        senha.descricao?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    IconButton(onClick = { onRemover(senha.id) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remover senha")
                    }
                }
            }
        }
    }
}
