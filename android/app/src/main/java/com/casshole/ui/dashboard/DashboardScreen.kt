package com.casshole.ui.dashboard

import android.content.Intent
import android.util.Log
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.platform.LocalContext
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
import com.casshole.data.export.ExportRow
import com.casshole.data.export.SummaryPdfExporter
import com.casshole.data.local.InvoicePeriod
import com.casshole.data.local.InvoiceWithTransactions
import com.casshole.data.local.SummaryAggregator
import com.casshole.data.local.entity.TransactionEntity
import androidx.compose.material3.HorizontalDivider
import com.casshole.ui.components.EditCategoryDialog
import com.casshole.ui.components.AdaptiveScreen
import com.casshole.ui.components.InstallmentBadge
import com.casshole.ui.components.formatCurrency
import com.casshole.ui.theme.CategoryIcon
import com.casshole.ui.theme.categoryVisual

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onSendInvoice: () -> Unit,
    viewModel: DashboardViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val summaryDisplayMode by viewModel.summaryDisplayMode.collectAsState()
    val amountsHidden by viewModel.amountsHidden.collectAsState()
    val retroactiveCount by viewModel.retroactiveCount.collectAsState()
    val availablePeriods by viewModel.availablePeriods.collectAsState()
    val periodFilter by viewModel.periodFilter.collectAsState()
    val emailFetchState by EmailFetchCoordinator.state.collectAsState()
    val context = LocalContext.current

    val allInvoices = (state as? DashboardState.Loaded)?.invoices.orEmpty()
    val periodInvoices = when (val filter = periodFilter) {
        is PeriodFilter.AllMonths -> allInvoices
        is PeriodFilter.Month -> SummaryAggregator.filterByMonth(allInvoices, filter.period.month, filter.period.year)
    }
    val periodLabel = when (val filter = periodFilter) {
        is PeriodFilter.AllMonths -> stringResource(R.string.dashboard_period_all)
        is PeriodFilter.Month -> filter.period.toString()
    }
    val exportFailedMessage = stringResource(R.string.export_error)
    val exportTitle = stringResource(R.string.dashboard_summary_title)
    val exportTotalLabel = stringResource(R.string.export_total_label)
    val exportFooter = stringResource(R.string.export_footer, java.time.LocalDate.now().toString())
    val exportFileName = stringResource(R.string.export_file_name, periodLabel.replace("/", "-"))
    val exportChooserTitle = stringResource(R.string.export_share_chooser)
    var exportError by remember { mutableStateOf<String?>(null) }

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
                    if (periodInvoices.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                exportError = try {
                                    // Valores reais, nunca mascarados: um PDF com
                                    // "R$ ••••" não serviria para nada, e a exportação
                                    // é sempre uma ação explícita do usuário.
                                    val uri = SummaryPdfExporter.export(
                                        context = context,
                                        fileName = exportFileName,
                                        title = exportTitle,
                                        subtitle = periodLabel,
                                        rows = SummaryAggregator.categoryTotals(periodInvoices).map {
                                            ExportRow(it.category, formatCurrency(it.total, hidden = false))
                                        },
                                        totalLabel = exportTotalLabel,
                                        totalValue = formatCurrency(
                                            periodInvoices.flatMap { it.transactions }.sumOf { it.amount },
                                            hidden = false,
                                        ),
                                        footer = exportFooter,
                                    )
                                    context.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            },
                                            exportChooserTitle,
                                        )
                                    )
                                    null
                                } catch (e: Exception) {
                                    Log.w("DashboardScreen", "Could not export the summary", e)
                                    exportFailedMessage
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.IosShare,
                                contentDescription = stringResource(R.string.action_export_summary),
                            )
                        }
                    }
                    IconButton(onClick = viewModel::toggleAmountsHidden) {
                        Icon(
                            imageVector = if (amountsHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (amountsHidden) R.string.action_show_amounts else R.string.action_hide_amounts
                            ),
                        )
                    }
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
                            invoices = periodInvoices,
                            nicknames = currentState.nicknames,
                            periodLabel = periodLabel,
                            availablePeriods = availablePeriods,
                            periodFilter = periodFilter,
                            onSelectPeriod = viewModel::selectPeriod,
                            exportError = exportError,
                            summaryDisplayMode = summaryDisplayMode,
                            amountsHidden = amountsHidden,
                            retroactiveCount = retroactiveCount,
                            onPrepareCategoryEdit = viewModel::prepareCategoryEdit,
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

@Composable
private fun GeneralSummaryContent(
    invoices: List<InvoiceWithTransactions>,
    nicknames: Map<Pair<String, String>, String>,
    periodLabel: String,
    availablePeriods: List<InvoicePeriod>,
    periodFilter: PeriodFilter,
    onSelectPeriod: (PeriodFilter) -> Unit,
    exportError: String?,
    summaryDisplayMode: String,
    amountsHidden: Boolean,
    retroactiveCount: Int,
    onPrepareCategoryEdit: (TransactionEntity) -> Unit,
    onUpdateCategory: (Long, String, Boolean) -> Unit,
) {
    val currentMonthInvoices = invoices

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var transactionBeingEdited by remember { mutableStateOf<TransactionEntity?>(null) }

    AdaptiveScreen {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (exportError != null) {
                Text(
                    text = exportError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (currentMonthInvoices.isEmpty()) {
                Text(
                    text = stringResource(R.string.dashboard_no_invoices_for_month, periodLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                GeneralSummaryCard(
                    currentMonthInvoices = currentMonthInvoices,
                    periodLabel = periodLabel,
                    availablePeriods = availablePeriods,
                    periodFilter = periodFilter,
                    onSelectPeriod = onSelectPeriod,
                    summaryDisplayMode = summaryDisplayMode,
                    amountsHidden = amountsHidden,
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
            amountsHidden = amountsHidden,
            onDismiss = { selectedCategory = null },
            onTransactionClick = {
                onPrepareCategoryEdit(it)
                transactionBeingEdited = it
            },
        )
    }

    transactionBeingEdited?.let { transaction ->
        EditCategoryDialog(
            transaction = transaction,
            categories = AVAILABLE_CATEGORIES,
            retroactiveCount = retroactiveCount,
            onConfirm = { newCategory, applyToPast ->
                onUpdateCategory(transaction.id, newCategory, applyToPast)
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
    amountsHidden: Boolean,
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
                    text = stringResource(R.string.dashboard_transactions_and_total_prefix, transactions.size) +
                        formatCurrency(total, amountsHidden),
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
                            amountsHidden = amountsHidden,
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
    amountsHidden: Boolean,
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
                text = formatCurrency(transaction.amount, amountsHidden),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            InstallmentBadge(
                currentInstallment = transaction.currentInstallment,
                totalInstallments = transaction.totalInstallments,
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(
                text = "${transaction.date} • $bankLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun GeneralSummaryCard(
    currentMonthInvoices: List<InvoiceWithTransactions>,
    periodLabel: String,
    availablePeriods: List<InvoicePeriod>,
    periodFilter: PeriodFilter,
    onSelectPeriod: (PeriodFilter) -> Unit,
    summaryDisplayMode: String,
    amountsHidden: Boolean,
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
            PeriodSelector(
                periodLabel = periodLabel,
                availablePeriods = availablePeriods,
                periodFilter = periodFilter,
                onSelectPeriod = onSelectPeriod,
            )
            Text(
                text = formatCurrency(totalAmount, amountsHidden),
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
                        amountsHidden = amountsHidden,
                        onClick = { onCategoryClick(category) },
                    )
                }
            }
        }
    }
}

/**
 * Month navigation for the summary: arrows step through the periods that have
 * invoices, and the label opens the full list (plus an "all months" option).
 * Only periods with data are offered - stepping into empty months would just
 * be a way to reach a blank screen.
 */
@Composable
private fun PeriodSelector(
    periodLabel: String,
    availablePeriods: List<InvoicePeriod>,
    periodFilter: PeriodFilter,
    onSelectPeriod: (PeriodFilter) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val currentIndex = (periodFilter as? PeriodFilter.Month)?.let { availablePeriods.indexOf(it.period) } ?: -1
    val hasPrevious = currentIndex > 0
    val hasNext = currentIndex >= 0 && currentIndex < availablePeriods.lastIndex

    Row(
        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onSelectPeriod(PeriodFilter.Month(availablePeriods[currentIndex - 1])) },
            enabled = hasPrevious,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = stringResource(R.string.dashboard_period_previous_cd),
            )
        }

        Box {
            TextButton(onClick = { menuOpen = true }) {
                Text(text = periodLabel, style = MaterialTheme.typography.bodyMedium)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.dashboard_period_all)) },
                    onClick = {
                        onSelectPeriod(PeriodFilter.AllMonths)
                        menuOpen = false
                    },
                )
                // Newest first: the recent months are the ones usually looked up.
                availablePeriods.reversed().forEach { period ->
                    DropdownMenuItem(
                        text = { Text(period.toString()) },
                        onClick = {
                            onSelectPeriod(PeriodFilter.Month(period))
                            menuOpen = false
                        },
                    )
                }
            }
        }

        IconButton(
            onClick = { onSelectPeriod(PeriodFilter.Month(availablePeriods[currentIndex + 1])) },
            enabled = hasNext,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.dashboard_period_next_cd),
            )
        }
    }
}

@Composable
private fun CategorySummaryRow(category: String, total: Double, amountsHidden: Boolean, onClick: () -> Unit) {
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
                text = formatCurrency(total, amountsHidden),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
        }
    }
}
