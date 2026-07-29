package com.casshole.ui.comparison

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.casshole.R
import com.casshole.data.PreferencesRepository
import com.casshole.data.local.MonthlyComparison
import com.casshole.data.local.InvoiceRepository
import com.casshole.data.local.SummaryAggregator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MonthYear(val month: Int, val year: Int) : Comparable<MonthYear> {
    override fun compareTo(other: MonthYear): Int =
        compareValuesBy(this, other, { it.year }, { it.month })

    override fun toString(): String = "%02d/%d".format(month, year)
}

sealed class PeriodsState {
    data object Loading : PeriodsState()
    data class Available(val periods: List<MonthYear>) : PeriodsState()
    data class Error(val message: String) : PeriodsState()
}

sealed class ComparisonUiState {
    data object Idle : ComparisonUiState()
    data object Comparing : ComparisonUiState()
    data class Result(val comparison: MonthlyComparison) : ComparisonUiState()
    data class Error(val message: String) : ComparisonUiState()
}

class ComparisonViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InvoiceRepository(application)
    private val preferencesRepository = PreferencesRepository(application)

    val amountsHidden: StateFlow<Boolean> = preferencesRepository.amountsHidden.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    private val _periodsState = MutableStateFlow<PeriodsState>(PeriodsState.Loading)
    val periodsState: StateFlow<PeriodsState> = _periodsState.asStateFlow()

    private val _selected = MutableStateFlow<Set<MonthYear>>(emptySet())
    val selected: StateFlow<Set<MonthYear>> = _selected.asStateFlow()

    private val _comparisonState = MutableStateFlow<ComparisonUiState>(ComparisonUiState.Idle)
    val comparisonState: StateFlow<ComparisonUiState> = _comparisonState.asStateFlow()

    init {
        loadAvailablePeriods()
    }

    fun loadAvailablePeriods() {
        viewModelScope.launch {
            _periodsState.value = PeriodsState.Loading
            try {
                val invoices = repository.listInvoices()
                val periods = invoices
                    .map { MonthYear(it.referenceMonth, it.referenceYear) }
                    .distinct()
                    .sorted()
                _periodsState.value = PeriodsState.Available(periods)
                _selected.value = periods.takeLast(3).toSet()
            } catch (e: Exception) {
                _periodsState.value = PeriodsState.Error(e.message ?: getApplication<Application>().getString(R.string.error_load_periods))
            }
        }
    }

    fun toggleSelection(period: MonthYear) {
        _selected.value = if (period in _selected.value) {
            _selected.value - period
        } else {
            _selected.value + period
        }
    }

    fun compare() {
        val periods = _selected.value.sorted()
        if (periods.isEmpty()) {
            _comparisonState.value = ComparisonUiState.Error(getApplication<Application>().getString(R.string.error_select_month))
            return
        }

        viewModelScope.launch {
            _comparisonState.value = ComparisonUiState.Comparing
            try {
                val transactionsByPeriod = periods.map { period ->
                    Triple(period.month, period.year, repository.transactionsByPeriod(period.month, period.year))
                }
                val result = SummaryAggregator.compareMonths(transactionsByPeriod)
                _comparisonState.value = ComparisonUiState.Result(result)
            } catch (e: Exception) {
                _comparisonState.value = ComparisonUiState.Error(e.message ?: getApplication<Application>().getString(R.string.error_compare_months))
            }
        }
    }
}
