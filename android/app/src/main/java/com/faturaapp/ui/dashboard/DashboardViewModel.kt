package com.faturaapp.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faturaapp.data.local.InvoiceWithTransactions
import com.faturaapp.data.local.InvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DashboardState {
    data object Loading : DashboardState()
    data class Loaded(val invoices: List<InvoiceWithTransactions>) : DashboardState()
    data class Error(val message: String) : DashboardState()
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InvoiceRepository(application)

    private val _state = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        loadInvoices()
    }

    fun loadInvoices() {
        viewModelScope.launch {
            _state.value = DashboardState.Loading
            try {
                val invoices = repository.listInvoices()
                _state.value = DashboardState.Loaded(invoices)
            } catch (e: Exception) {
                _state.value = DashboardState.Error(e.message ?: "Erro ao carregar faturas")
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
                _state.value = DashboardState.Loaded(repository.listInvoices())
            } catch (e: Exception) {
                _state.value = DashboardState.Error(e.message ?: "Erro ao atualizar categoria")
            }
        }
    }
}
