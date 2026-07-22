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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faturaapp.data.model.SenhaPadrao
import kotlinx.coroutines.delay

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

        CampoSenhaComRevelacao(
            valor = novaSenha,
            onValueChange = { novaSenha = it },
            label = "Senha",
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
    val revelados = remember { mutableStateMapOf<Int, Boolean>() }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(senhas) { senha ->
            val revelado = revelados[senha.id] == true
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = if (revelado) senha.valor else "•".repeat(senha.valor.length),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        senha.descricao?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Row {
                        IconButton(onClick = { revelados[senha.id] = !revelado }) {
                            Icon(
                                imageVector = if (revelado) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (revelado) "Ocultar senha" else "Mostrar senha",
                            )
                        }
                        IconButton(onClick = { onRemover(senha.id) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remover senha")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mascara a senha mas revela o ultimo caractere digitado por um instante,
 * como em campos de senha de apps bancarios. Tambem oferece um icone de
 * olhinho pra mostrar/ocultar a senha inteira.
 */
private class RevelarUltimoCaractereTransformation(
    private val revelarUltimo: Boolean,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val texto = text.text
        val mascarado = when {
            texto.isEmpty() -> ""
            revelarUltimo -> "•".repeat(texto.length - 1) + texto.last()
            else -> "•".repeat(texto.length)
        }
        return TransformedText(AnnotatedString(mascarado), OffsetMapping.Identity)
    }
}

@Composable
private fun CampoSenhaComRevelacao(
    valor: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var mostrarTudo by remember { mutableStateOf(false) }
    var revelarUltimo by remember { mutableStateOf(false) }

    LaunchedEffect(valor) {
        if (valor.isNotEmpty()) {
            revelarUltimo = true
            delay(1000)
            revelarUltimo = false
        }
    }

    OutlinedTextField(
        value = valor,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (mostrarTudo) {
            VisualTransformation.None
        } else {
            RevelarUltimoCaractereTransformation(revelarUltimo)
        },
        trailingIcon = {
            IconButton(onClick = { mostrarTudo = !mostrarTudo }) {
                Icon(
                    imageVector = if (mostrarTudo) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (mostrarTudo) "Ocultar senha" else "Mostrar senha",
                )
            }
        },
        modifier = modifier,
    )
}
