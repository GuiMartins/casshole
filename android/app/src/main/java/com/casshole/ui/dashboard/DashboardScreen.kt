package com.casshole.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.casshole.R
import com.casshole.categorizer.AVAILABLE_CATEGORIES
import com.casshole.data.PreferencesRepository
import com.casshole.data.email.EmailFetchCoordinator
import com.casshole.data.email.EmailFetchState
import com.casshole.data.local.InvoiceWithTransactions
import com.casshole.data.local.SummaryAggregator
import com.casshole.data.local.entity.TransactionEntity
import androidx.compose.material3.HorizontalDivider
import com.casshole.ui.components.EditCategoryDialog
import com.casshole.ui.components.AdaptiveScreen
import com.casshole.ui.theme.CategoryIcon
import com.casshole.ui.theme.categoryVisual
import java.time.YearMonth

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onSendInvoice: () -> Unit,
    viewModel: DashboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val summaryDisplayMode by viewModel.summaryDisplayMode.collectAsState()
    val emailFetchState by EmailFetchCoordinator.state.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    val onResumeAction by rememberUpdatedState(viewModel::loadInvoices)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onResumeAction()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-fetch runs in the background from app start - refresh as soon as it lands
    // new invoices, instead of waiting for the user to background/foreground the app.
    LaunchedEffect(emailFetchState) {
        if (emailFetchState is EmailFetchState.Done) viewModel.loadInvoices()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    IconButton(onClick = onSendInvoice) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(R.string.action_send_invoice))
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (emailFetchState is EmailFetchState.Fetching) {
                EmailFetchBanner(state = emailFetchState as EmailFetchState.Fetching)
            }
            Box(modifier = Modifier.fillMaxSize()) {
            when (val currentState = state) {
                is DashboardState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                is DashboardState.Error -> Text(
                    text = currentState.message,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
                is DashboardState.Loaded -> {
                    if (currentState.invoices.isEmpty()) {
                        Text(
                            text = stringResource(R.string.empty_no_invoices),
                            modifier = Modifier.align(Alignment.Center),
                        )
                    } else {
                        GeneralSummaryContent(
                            invoices = currentState.invoices,
                            nicknames = currentState.nicknames,
                            summaryDisplayMode = summaryDisplayMode,
                            onUpdateCategory = viewModel::updateCategory,
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun EmailFetchBanner(state: EmailFetchState.Fetching) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(
            text = if (state.total > 0) {
                stringResource(R.string.email_fetch_progress, state.processed, state.total)
            } else {
                stringResource(R.string.email_fetch_banner_starting)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Month/year label for the current-month filter, e.g. "07/2026". */
private fun currentMonthLabel(): String {
    val now = YearMonth.now()
    return "%02d/%d".format(now.monthValue, now.year)
}

@Composable
private fun GeneralSummaryContent(
    invoices: List<InvoiceWithTransactions>,
    nicknames: Map<Pair<String, String>, String>,
    summaryDisplayMode: String,
    onUpdateCategory: (Long, String) -> Unit,
) {
    val now = remember { YearMonth.now() }
    val currentMonthInvoices = SummaryAggregator.filterByMonth(invoices, now.monthValue, now.year)

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var transactionBeingEdited by remember { mutableStateOf<TransactionEntity?>(null) }

    AdaptiveScreen {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (currentMonthInvoices.isEmpty()) {
                Text(
                    text = stringResource(R.string.dashboard_no_invoices_for_month, currentMonthLabel()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                GeneralSummaryCard(
                    currentMonthInvoices = currentMonthInvoices,
                    summaryDisplayMode = summaryDisplayMode,
                    onCategoryClick = { selectedCategory = it },
                )
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    selectedCategory?.let { category ->
        val categoryTransactions = currentMonthInvoices
            .flatMap { invoice -> invoice.transactions.map { it to invoice } }
            .filter { (transaction, _) -> transaction.category == category }
            .sortedByDescending { (transaction, _) -> transaction.amount }

        CategoryTransactionsDialog(
            category = category,
            transactions = categoryTransactions,
            nicknames = nicknames,
            onDismiss = { selectedCategory = null },
            onTransactionClick = { transactionBeingEdited = it },
        )
    }

    transactionBeingEdited?.let { transaction ->
        EditCategoryDialog(
            transaction = transaction,
            categories = AVAILABLE_CATEGORIES,
            onConfirm = { newCategory ->
                onUpdateCategory(transaction.id, newCategory)
                transactionBeingEdited = null
            },
            onCancel = { transactionBeingEdited = null },
        )
    }
}

@Composable
private fun CategoryTransactionsDialog(
    category: String,
    transactions: List<Pair<TransactionEntity, InvoiceWithTransactions>>,
    nicknames: Map<Pair<String, String>, String>,
    onDismiss: () -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
) {
    val total = transactions.sumOf { (transaction, _) -> transaction.amount }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(category = category, size = 28.dp)
                Text(category, modifier = Modifier.padding(start = 8.dp))
            }
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.dashboard_transactions_and_total, transactions.size, total),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(transactions) { (transaction, invoice) ->
                        TransactionSummaryRow(
                            transaction = transaction,
                            invoice = invoice,
                            nickname = nicknames[invoice.bank to invoice.card],
                            onClick = { onTransactionClick(transaction) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

@Composable
private fun TransactionSummaryRow(
    transaction: TransactionEntity,
    invoice: InvoiceWithTransactions,
    nickname: String?,
    onClick: () -> Unit,
) {
    val cardSuffix = if (invoice.card.isNotBlank()) " ••••${invoice.card}" else ""
    val bankLabel = nickname ?: "${invoice.bank.replaceFirstChar { it.uppercase() }}$cardSuffix"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = transaction.description,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "R$ %.2f".format(transaction.amount),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            text = "${transaction.date} • $bankLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider()
}

@Composable
private fun GeneralSummaryCard(
    currentMonthInvoices: List<InvoiceWithTransactions>,
    summaryDisplayMode: String,
    onCategoryClick: (String) -> Unit,
) {
    val transactions = currentMonthInvoices.flatMap { it.transactions }
    val totalAmount = transactions.sumOf { it.amount }
    val byCategory = transactions
        .groupBy { it.category }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
        .toList()
        .sortedByDescending { (_, total) -> total }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.dashboard_summary_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.dashboard_current_month_label, currentMonthLabel()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = "R$ %.2f".format(totalAmount),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )

            if (byCategory.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                if (summaryDisplayMode == PreferencesRepository.SUMMARY_DISPLAY_PIE_CHART) {
                    PieChart(
                        data = byCategory.map { (category, total) ->
                            PieSlice(category, total, categoryVisual(category).color)
                        },
                    )
                }

                byCategory.forEach { (category, total) ->
                    CategorySummaryRow(
                        category = category,
                        total = total,
                        onClick = { onCategoryClick(category) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategorySummaryRow(category: String, total: Double, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            ) {
                CategoryIcon(category = category, size = 28.dp)
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Text(
                text = "R$ %.2f".format(total),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
        }
    }
}
