package com.casshole.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.casshole.R
import com.casshole.data.local.entity.TransactionEntity

/**
 * [retroactiveCount] is how many stored transactions share this description
 * (the edited one included). With more than one, the dialog offers to apply
 * the correction to all of them, not just the transaction that was tapped -
 * the same merchant is usually miscategorized in every past invoice too.
 */
@Composable
fun EditCategoryDialog(
    transaction: TransactionEntity,
    categories: List<String>,
    retroactiveCount: Int = 1,
    onConfirm: (category: String, applyToPast: Boolean) -> Unit,
    onCancel: () -> Unit,
) {
    var selected by remember(transaction.id) { mutableStateOf(transaction.category) }
    var applyToPast by remember(transaction.id) { mutableStateOf(true) }
    val canApplyToPast = retroactiveCount > 1

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.edit_category_title, transaction.description)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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

                if (canApplyToPast) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { applyToPast = !applyToPast },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = applyToPast,
                            onCheckedChange = { applyToPast = it },
                        )
                        Text(
                            text = stringResource(R.string.edit_category_apply_to_past, retroactiveCount),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.edit_category_future_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected, canApplyToPast && applyToPast) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
