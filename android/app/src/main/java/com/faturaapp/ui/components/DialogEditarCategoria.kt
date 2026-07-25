package com.faturaapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.faturaapp.data.local.entity.TransacaoEntity

@Composable
fun DialogEditarCategoria(
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
