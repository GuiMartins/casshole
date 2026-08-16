package com.casshole.ui.invoicesbycard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.casshole.R
import com.casshole.data.local.InvoiceWithTransactions
import com.casshole.ui.components.AdaptiveScreen
import com.casshole.ui.components.EditCardNicknameDialog
import com.casshole.ui.components.formatCurrency
import com.casshole.ui.theme.bankColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesByCardScreen(
    onOpenInvoice: (Long) -> Unit,
    viewModel: InvoicesByCardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val amountsHidden by viewModel.amountsHidden.collectAsState()

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
                        CardWallet(
                            invoices = currentState.invoices,
                            nicknames = currentState.nicknames,
                            amountsHidden = amountsHidden,
                            onOpenInvoice = onOpenInvoice,
                            onSetNickname = viewModel::setNickname,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Wallet view: one tile per physical card, invoices hidden until the card is
 * tapped. The flat "header + every invoice of every card" list buried the
 * cards themselves once a few months had been imported.
 *
 * Only one card stays open at a time - an accordion, so the wallet never
 * scrolls away from what was just opened.
 */
@Composable
private fun CardWallet(
    invoices: List<InvoiceWithTransactions>,
    nicknames: Map<Pair<String, String>, String>,
    amountsHidden: Boolean,
    onOpenInvoice: (Long) -> Unit,
    onSetNickname: (String, String, String) -> Unit,
) {
    val groups = invoices
        .groupBy { it.bank to it.card }
        .toList()
        .sortedBy { (key, _) -> "${key.first}${key.second}" }

    var expandedCard by remember { mutableStateOf<Pair<String, String>?>(null) }
    var cardBeingRenamed by remember { mutableStateOf<Pair<String, String>?>(null) }

    AdaptiveScreen {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        ) {
            items(
                count = groups.size,
                key = { index -> "${groups[index].first.first}-${groups[index].first.second}" },
            ) { index ->
                val (key, cardInvoices) = groups[index]
                val (bank, card) = key
                CardTile(
                    bank = bank,
                    card = card,
                    nickname = nicknames[key],
                    invoices = cardInvoices,
                    amountsHidden = amountsHidden,
                    expanded = expandedCard == key,
                    onToggle = { expandedCard = if (expandedCard == key) null else key },
                    onEditNickname = { cardBeingRenamed = key },
                    onOpenInvoice = onOpenInvoice,
                )
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
private fun CardTile(
    bank: String,
    card: String,
    nickname: String?,
    invoices: List<InvoiceWithTransactions>,
    amountsHidden: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEditNickname: () -> Unit,
    onOpenInvoice: (Long) -> Unit,
) {
    val sortedInvoices = invoices.sortedByDescending { it.referenceYear * 100 + it.referenceMonth }
    val latest = sortedInvoices.firstOrNull()
    val latestTotal = latest?.transactions?.sumOf { it.amount } ?: 0.0
    val bankLabel = bank.replaceFirstChar { it.uppercase() }
    val baseColor = bankColor(bank)
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "card-chevron")

    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(lerp(baseColor, Color.White, 0.12f), lerp(baseColor, Color.Black, 0.35f))
                    )
                )
                .clickable(onClick = onToggle)
                .padding(18.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = bankLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = onEditNickname,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.card_nickname_edit_cd),
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = nickname ?: bankLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (card.isNotBlank()) {
                    Text(
                        text = "•••• •••• •••• $card",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.invoices_by_card_invoices_count, invoices.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                        if (latest != null) {
                            Text(
                                text = stringResource(
                                    R.string.invoices_by_card_latest_prefix,
                                    latest.referenceMonth, latest.referenceYear,
                                ) + formatCurrency(latestTotal, amountsHidden),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ExpandMore,
                            contentDescription = stringResource(
                                if (expanded) R.string.invoices_by_card_collapse_cd else R.string.invoices_by_card_expand_cd
                            ),
                            tint = Color.White,
                            modifier = Modifier.rotate(chevronRotation),
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    sortedInvoices.forEachIndexed { index, invoice ->
                        if (index > 0) HorizontalDivider()
                        InvoiceMonthRow(
                            invoice = invoice,
                            amountsHidden = amountsHidden,
                            onClick = { onOpenInvoice(invoice.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceMonthRow(invoice: InvoiceWithTransactions, amountsHidden: Boolean, onClick: () -> Unit) {
    val totalSpent = invoice.transactions.sumOf { it.amount }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "%02d/%d".format(invoice.referenceMonth, invoice.referenceYear),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.invoices_by_card_transactions_count, invoice.transactions.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = formatCurrency(totalSpent, amountsHidden),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
