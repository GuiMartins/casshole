package com.moneyhole.ui.components

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
import com.moneyhole.data.local.entity.TransactionEntity

@Composable
fun EditCategoryDialog(
    transaction: TransactionEntity,
    categories: List<String>,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var selected by remember(transaction.id) { mutableStateOf(transaction.category) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Categoria de \"${transaction.description}\"") },
        text = {
            Column {
                categories.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = category },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = category == selected,
                            onClick = { selected = category },
                        )
                        Text(category)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancelar")
            }
        },
    )
}
