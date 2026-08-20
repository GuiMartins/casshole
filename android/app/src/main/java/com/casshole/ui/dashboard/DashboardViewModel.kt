package com.casshole.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.casshole.R
import com.casshole.data.PreferencesRepository
import com.casshole.data.local.InvoicePeriod
import com.casshole.data.local.InvoiceWithTransactions
import com.casshole.data.local.InvoiceRepository
import com.casshole.data.local.SummaryAggregator
import com.casshole.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Which reference period the summary is scoped to. */
sealed class PeriodFilter {
    data object AllMonths : PeriodFilter()
    data class Month(val period: InvoicePeriod) : PeriodFilter()
}

sealed class DashboardState {
    data object Loading : DashboardState()
    data class Loaded(
        val invoices: List<InvoiceWithTransactions>,
        val nicknames: Map<Pair<String, String>, String>,
    ) : DashboardState()
    data class Error(val message: String) : DashboardState()
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InvoiceRepository(application)
    private val preferencesRepository = PreferencesRepository(application)

    private val _state = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    val summaryDisplayMode: StateFlow<String> = preferencesRepository.summaryDisplayMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PreferencesRepository.SUMMARY_DISPLAY_NUMBERS,
    )

    val amountsHidden: StateFlow<Boolean> = preferencesRepository.amountsHidden.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    private val _availablePeriods = MutableStateFlow<List<InvoicePeriod>>(emptyList())
    val availablePeriods: StateFlow<List<InvoicePeriod>> = _availablePeriods.asStateFlow()

    private val _periodFilter = MutableStateFlow<PeriodFilter>(PeriodFilter.AllMonths)
    val periodFilter: StateFlow<PeriodFilter> = _periodFilter.asStateFlow()

    /** Until the user picks a period, every reload re-derives the default from the data. */
    private var periodPickedByUser = false

    private val _retroactiveCount = MutableStateFlow(1)
    val retroactiveCount: StateFlow<Int> = _retroactiveCount.asStateFlow()

    init {
        loadInvoices()
    }

    fun toggleAmountsHidden() {
        viewModelScope.launch {
            preferencesRepository.setAmountsHidden(!amountsHidden.value)
        }
    }

    fun loadInvoices() {
        viewModelScope.launch {
            _state.value = DashboardState.Loading
            try {
                val invoices = repository.listInvoices()
                val nicknames = repository.listCardNicknames().associate { (it.bank to it.card) to it.nickname }
                updateAvailablePeriods(invoices)
                _state.value = DashboardState.Loaded(invoices, nicknames)
            } catch (e: Exception) {
                _state.value = DashboardState.Error(e.message ?: getApplication<Application>().getString(R.string.error_load_invoices))
            }
        }
    }

    /**
     * Counts how many stored transactions share this description before the
     * edit dialog opens, so it can offer to fix all of them at once.
     */
    fun prepareCategoryEdit(transaction: TransactionEntity) {
        _retroactiveCount.value = 1
        viewModelScope.launch {
            _retroactiveCount.value = repository.countTransactionsWithDescription(transaction.description)
        }
    }

    fun selectPeriod(filter: PeriodFilter) {
        periodPickedByUser = true
        _periodFilter.value = filter
    }

    /**
     * Keeps whatever the user picked as long as that period still exists,
     * otherwise falls back to the newest period with an invoice. The calendar
     * month is deliberately not the default: invoices are filed by reference
     * month and arrive late, so "now" is often an empty screen.
     */
    private fun updateAvailablePeriods(invoices: List<InvoiceWithTransactions>) {
        val periods = SummaryAggregator.availablePeriods(invoices)
        _availablePeriods.value = periods

        val current = _periodFilter.value
        val keepCurrent = periodPickedByUser && (
            current is PeriodFilter.AllMonths ||
                (current is PeriodFilter.Month && current.period in periods)
            )
        if (!keepCurrent || periods.isEmpty()) {
            _periodFilter.value = periods.lastOrNull()?.let { PeriodFilter.Month(it) } ?: PeriodFilter.AllMonths
        }
    }

    fun updateCategory(transactionId: Long, newCategory: String, applyToPast: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateCategory(transactionId, newCategory, applyToPast)
                // Update without going through Loading, so it doesn't close
                // any open dialogs (the summary and category edit both live
                // inside the Loaded branch).
                val nicknames = repository.listCardNicknames().associate { (it.bank to it.card) to it.nickname }
                _state.value = DashboardState.Loaded(repository.listInvoices(), nicknames)
            } catch (e: Exception) {
                _state.value = DashboardState.Error(e.message ?: getApplication<Application>().getString(R.string.error_update_category))
            }
        }
    }
}
