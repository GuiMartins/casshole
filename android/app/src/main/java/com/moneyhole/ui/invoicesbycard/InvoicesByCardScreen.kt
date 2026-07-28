package com.moneyhole.ui.invoicesbycard

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.moneyhole.R
import com.moneyhole.data.local.InvoiceWithTransactions
import com.moneyhole.ui.components.AdaptiveScreen
import com.moneyhole.ui.components.EditCardNicknameDialog
import com.moneyhole.ui.theme.BankBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesByCardScreen(
    onOpenInvoice: (Long) -> Unit,
    viewModel: InvoicesByCardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    val onResumeAction by rememberUpdatedState(viewModel::loadInvoices)
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
                title = { Text(text = stringResource(R.string.nav_invoices_by_card), fontWeight = FontWeight.Bold) },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when (val currentState = state) {
                is InvoicesByCardState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                is InvoicesByCardState.Error -> Text(
                    text = currentState.message,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
                is InvoicesByCardState.Loaded -> {
                    if (currentState.invoices.isEmpty()) {
                        Text(
                            text = stringResource(R.string.empty_no_invoices),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else {
                        InvoiceListByCard(
                            invoices = currentState.invoices,
                            nicknames = currentState.nicknames,
                            onOpenInvoice = onOpenInvoice,
                            onSetNickname = viewModel::setNickname,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceListByCard(
    invoices: List<InvoiceWithTransactions>,
    nicknames: Map<Pair<String, String>, String>,
    onOpenInvoice: (Long) -> Unit,
    onSetNickname: (String, String, String) -> Unit,
) {
    val groups = invoices
        .groupBy { it.bank to it.card }
        .toList()
        .sortedBy { (key, _) -> "${key.first}${key.second}" }

    var cardBeingRenamed by remember { mutableStateOf<Pair<String, String>?>(null) }

    AdaptiveScreen {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        ) {
            groups.forEach { (key, groupInvoices) ->
                val (bank, card) = key
                item(key = "header-$bank-$card") {
                    CardGroupHeader(
                        bank = bank,
                        card = card,
                        nickname = nicknames[key],
                        onEditNickname = { cardBeingRenamed = key },
                    )
                }
                items(
                    groupInvoices.sortedByDescending { it.referenceYear * 100 + it.referenceMonth },
                    key = { it.id },
                ) { invoice ->
                    InvoiceMonthRow(invoice, onClick = { onOpenInvoice(invoice.id) })
                }
            }
        }
    }

    cardBeingRenamed?.let { (bank, card) ->
        EditCardNicknameDialog(
            currentNickname = nicknames[bank to card] ?: "",
            onConfirm = { nickname ->
                onSetNickname(bank, card, nickname)
                cardBeingRenamed = null
            },
            onCancel = { cardBeingRenamed = null },
        )
    }
}

@Composable
private fun CardGroupHeader(
    bank: String,
    card: String,
    nickname: String?,
    onEditNickname: () -> Unit,
) {
    val cardSuffix = if (card.isNotBlank()) " (••••$card)" else ""
    val bankLine = "${bank.replaceFirstChar { it.uppercase() }}$cardSuffix"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 8.dp),
    ) {
        BankBadge(bank = bank, size = 28.dp)
        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
            Text(
                text = nickname ?: bankLine,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            if (nickname != null) {
                Text(
                    text = bankLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = onEditNickname) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = stringResource(R.string.card_nickname_edit_cd),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InvoiceMonthRow(invoice: InvoiceWithTransactions, onClick: () -> Unit) {
    val totalSpent = invoice.transactions.sumOf { it.amount }
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
                text = "${invoice.referenceMonth}/${invoice.referenceYear}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "R$ %.2f".format(totalSpent),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.invoices_by_card_transactions_count, invoice.transactions.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
