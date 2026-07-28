package com.moneyhole.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneyhole.R
import com.moneyhole.data.PreferencesRepository
import com.moneyhole.data.local.InvoiceWithTransactions
import com.moneyhole.data.local.InvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    init {
        loadInvoices()
    }

    fun loadInvoices() {
        viewModelScope.launch {
            _state.value = DashboardState.Loading
            try {
                val invoices = repository.listInvoices()
                val nicknames = repository.listCardNicknames().associate { (it.bank to it.card) to it.nickname }
                _state.value = DashboardState.Loaded(invoices, nicknames)
            } catch (e: Exception) {
                _state.value = DashboardState.Error(e.message ?: getApplication<Application>().getString(R.string.error_load_invoices))
            }
        }
    }

    fun updateCategory(transactionId: Long, newCategory: String) {
        viewModelScope.launch {
            try {
                repository.updateCategory(transactionId, newCategory)
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
