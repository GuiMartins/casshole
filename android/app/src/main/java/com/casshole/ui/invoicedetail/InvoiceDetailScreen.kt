package com.casshole.ui.invoicedetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.casshole.R
import com.casshole.data.local.entity.TransactionEntity
import com.casshole.ui.components.EditCategoryDialog
import com.casshole.ui.components.AdaptiveScreen
import com.casshole.ui.components.InstallmentBadge
import com.casshole.ui.components.formatCurrency
import com.casshole.ui.theme.CategoryIcon
import com.casshole.ui.theme.categoryVisual

@Composable
fun InvoiceDetailScreen(viewModel: InvoiceDetailViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val categories by viewModel.availableCategories.collectAsState()
    val amountsHidden by viewModel.amountsHidden.collectAsState()
    val retroactiveCount by viewModel.retroactiveCount.collectAsState()

    var transactionBeingEdited by remember { mutableStateOf<TransactionEntity?>(null) }

    when (val currentState = state) {
        is InvoiceDetailState.Loading -> CircularProgressIndicator(
            modifier = Modifier.padding(24.dp)
        )
        is InvoiceDetailState.Error -> Text(
            text = currentState.message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(24.dp),
        )
        is InvoiceDetailState.Loaded -> InvoiceDetailContent(
            state = currentState,
            amountsHidden = amountsHidden,
            onTransactionClick = {
                viewModel.prepareCategoryEdit(it)
                transactionBeingEdited = it
            },
        )
    }

    transactionBeingEdited?.let { transaction ->
        EditCategoryDialog(
            transaction = transaction,
            categories = categories,
            retroactiveCount = retroactiveCount,
            onConfirm = { newCategory, applyToPast ->
                viewModel.updateCategory(transaction.id, newCategory, applyToPast)
                transactionBeingEdited = null
            },
            onCancel = { transactionBeingEdited = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceDetailContent(
    state: InvoiceDetailState.Loaded,
    amountsHidden: Boolean,
    onTransactionClick: (TransactionEntity) -> Unit,
) {
    val invoice = state.invoice
    val totalSpent = invoice.transactions.sumOf { it.amount }
    val cardSuffix = if (invoice.card.isNotBlank()) " (••••${invoice.card})" else ""
    val cardholders = invoice.transactions.map { it.cardholder }.filter { it.isNotBlank() }.distinct()
    val categories = invoice.transactions.map { it.category }.distinct().sorted()
    val cardsInLine = invoice.transactions.map { it.card }.filter { it.isNotBlank() }.distinct().sorted()
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    var searchText by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf<String?>(null) }
    var cardholderFilter by remember { mutableStateOf<String?>(null) }
    var cardFilter by remember { mutableStateOf<String?>(null) }

    val filtersActive = searchText.isNotBlank() || categoryFilter != null ||
        cardholderFilter != null || cardFilter != null

    val filteredTransactions = invoice.transactions.filter { transaction ->
        (searchText.isBlank() ||
            transaction.description.contains(searchText, ignoreCase = true) ||
            transaction.city.contains(searchText, ignoreCase = true)) &&
            (categoryFilter == null || transaction.category == categoryFilter) &&
            (cardholderFilter == null || transaction.cardholder == cardholderFilter) &&
            (cardFilter == null || transaction.card == cardFilter)
    }

    AdaptiveScreen {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Text(
                    text = state.cardNickname ?: "${invoice.bank.replaceFirstChar { it.uppercase() }}$cardSuffix",
                    style = MaterialTheme.typography.headlineSmall,
                )
                if (state.cardNickname != null) {
                    Text(
                        text = "${invoice.bank.replaceFirstChar { it.uppercase() }}$cardSuffix",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(R.string.invoice_detail_total_prefix, invoice.referenceMonth, invoice.referenceYear) +
                        formatCurrency(totalSpent, amountsHidden),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )

                Text(text = stringResource(R.string.invoice_detail_by_category), style = MaterialTheme.typography.titleSmall)
                state.byCategory.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryIcon(category = item.category, size = 26.dp)
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        Text(formatCurrency(item.total, amountsHidden), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text(stringResource(R.string.invoice_detail_search_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )

                if (categories.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = categoryFilter == category,
                                onClick = {
                                    categoryFilter = if (categoryFilter == category) null else category
                                },
                                label = { Text(category) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = categoryVisual(category).icon,
                                        contentDescription = null,
                                        tint = categoryVisual(category).color,
                                        modifier = Modifier.padding(2.dp),
                                    )
                                },
                            )
                        }
                    }
                }

                if (cardholders.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        cardholders.forEach { cardholder ->
                            FilterChip(
                                selected = cardholderFilter == cardholder,
                                onClick = {
                                    cardholderFilter = if (cardholderFilter == cardholder) null else cardholder
                                },
                                label = { Text(cardholder) },
                            )
                        }
                    }
                }

                if (cardsInLine.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        cardsInLine.forEach { card ->
                            FilterChip(
                                selected = cardFilter == card,
                                onClick = {
                                    cardFilter = if (cardFilter == card) null else card
                                },
                                label = { Text("••••$card") },
                            )
                        }
                    }
                }

                if (filtersActive) {
                    TextButton(
                        onClick = {
                            searchText = ""
                            categoryFilter = null
                            cardholderFilter = null
                            cardFilter = null
                        },
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        Text(stringResource(R.string.action_clear_filters))
                    }
                }

                val counterText = if (filtersActive) {
                    stringResource(R.string.invoice_detail_transactions_count_filtered, filteredTransactions.size, invoice.transactions.size)
                } else {
                    stringResource(R.string.invoice_detail_transactions_count, invoice.transactions.size)
                }
                Text(
                    text = counterText,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                )
            }

            if (cardholders.size > 1) {
                cardholders.forEach { cardholder ->
                    val cardholderTransactions = filteredTransactions.filter { it.cardholder == cardholder }
                    if (cardholderTransactions.isEmpty()) return@forEach
                    val isExpanded = filtersActive || (expanded[cardholder] ?: false)

                    item {
                        CardholderHeader(
                            cardholder = cardholder,
                            count = cardholderTransactions.size,
                            total = cardholderTransactions.sumOf { it.amount },
                            amountsHidden = amountsHidden,
                            expanded = isExpanded,
                            onClick = { expanded[cardholder] = !isExpanded },
                        )
                    }
                    if (isExpanded) {
                        items(cardholderTransactions) { transaction ->
                            TransactionRow(transaction, amountsHidden = amountsHidden, onClick = { onTransactionClick(transaction) })
                        }
                    }
                }
            } else {
                items(filteredTransactions) { transaction ->
                    TransactionRow(transaction, amountsHidden = amountsHidden, onClick = { onTransactionClick(transaction) })
                }
            }
        }
    }
}

@Composable
private fun CardholderHeader(
    cardholder: String,
    count: Int,
    total: Double,
    amountsHidden: Boolean,
    expanded: Boolean,
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
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp),
            )
            Text(
                text = "$cardholder ($count)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = formatCurrency(total, amountsHidden),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TransactionRow(transaction: TransactionEntity, amountsHidden: Boolean, onClick: () -> Unit) {
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
            CategoryIcon(category = transaction.category, size = 36.dp)
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
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = formatCurrency(transaction.amount, amountsHidden),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                val cityText = if (transaction.city.isNotBlank()) " • ${transaction.city}" else ""
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InstallmentBadge(
                        currentInstallment = transaction.currentInstallment,
                        totalInstallments = transaction.totalInstallments,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                    Text(
                        text = "${transaction.date} • ${transaction.category}$cityText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
