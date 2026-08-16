package com.casshole.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.casshole.R
import com.casshole.categorizer.AVAILABLE_CATEGORIES
import com.casshole.ui.components.AdaptiveScreen
import com.casshole.ui.theme.CategoryIcon

@Composable
fun CategoryOverridesScreen(viewModel: CategoryOverridesViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var itemBeingEdited by remember { mutableStateOf<CategoryOverrideItem?>(null) }

    AdaptiveScreen {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = stringResource(R.string.category_overrides_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.category_overrides_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
                )
            }

            when (val currentState = state) {
                is CategoryOverridesState.Loading -> item {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
                is CategoryOverridesState.Error -> item {
                    Text(currentState.message, color = MaterialTheme.colorScheme.error)
                }
                is CategoryOverridesState.Loaded -> {
                    if (currentState.overrides.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.category_overrides_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(currentState.overrides, key = { it.override.id }) { item ->
                            OverrideRow(
                                item = item,
                                onClick = { itemBeingEdited = item },
                                onRemove = viewModel::remove,
                            )
                        }
                    }
                }
            }
        }
    }

    itemBeingEdited?.let { item ->
        EditOverrideCategoryDialog(
            item = item,
            onConfirm = { category ->
                viewModel.updateCategory(item.override.description, category)
                itemBeingEdited = null
            },
            onCancel = { itemBeingEdited = null },
        )
    }
}

@Composable
private fun OverrideRow(
    item: CategoryOverrideItem,
    onClick: () -> Unit,
    onRemove: (Long) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            ) {
                CategoryIcon(category = item.override.category, size = 32.dp)
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = item.override.description,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.override.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.transactionCount > 0) {
                        Text(
                            text = stringResource(R.string.category_overrides_transactions_count, item.transactionCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            IconButton(onClick = { onRemove(item.override.id) }) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.category_overrides_remove_cd))
            }
        }
    }
}

@Composable
private fun EditOverrideCategoryDialog(
    item: CategoryOverrideItem,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var selected by remember(item.override.id) { mutableStateOf(item.override.category) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.edit_category_title, item.override.description)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                AVAILABLE_CATEGORIES.forEach { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = category },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = category == selected, onClick = { selected = category })
                        Text(category)
                    }
                }
                Text(
                    text = stringResource(R.string.category_overrides_edit_note, item.transactionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
