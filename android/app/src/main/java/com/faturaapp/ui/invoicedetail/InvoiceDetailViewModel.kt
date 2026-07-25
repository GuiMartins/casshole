package com.faturaapp.ui.invoicedetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.faturaapp.categorizer.AVAILABLE_CATEGORIES
import com.faturaapp.data.local.InvoiceWithTransactions
import com.faturaapp.data.local.InvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoryTotal(val category: String, val total: Double)

sealed class InvoiceDetailState {
    data object Loading : InvoiceDetailState()
    data class Loaded(val invoice: InvoiceWithTransactions, val byCategory: List<CategoryTotal>) : InvoiceDetailState()
    data class Error(val message: String) : InvoiceDetailState()
}

class InvoiceDetailViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val repository = InvoiceRepository(application)

    private val _state = MutableStateFlow<InvoiceDetailState>(InvoiceDetailState.Loading)
    val state: StateFlow<InvoiceDetailState> = _state.asStateFlow()

    val availableCategories: StateFlow<List<String>> = MutableStateFlow(AVAILABLE_CATEGORIES)

    private val _editError = MutableStateFlow<String?>(null)
    val editError: StateFlow<String?> = _editError.asStateFlow()

    init {
        load()
    }

    fun load() {
        val invoiceId = savedStateHandle.get<Long>("invoiceId") ?: run {
            _state.value = InvoiceDetailState.Error("ID da fatura inválido")
            return
        }

        viewModelScope.launch {
            _state.value = InvoiceDetailState.Loading
            try {
                val invoice = repository.getInvoice(invoiceId)
                    ?: throw IllegalStateException("Fatura não encontrada")
                val byCategory = invoice.transactions
                    .groupBy { it.category }
                    .map { (category, transactions) -> CategoryTotal(category, transactions.sumOf { it.amount }) }
                    .sortedByDescending { it.total }
                _state.value = InvoiceDetailState.Loaded(invoice, byCategory)
            } catch (e: Exception) {
                _state.value = InvoiceDetailState.Error(e.message ?: "Erro ao carregar fatura")
            }
        }
    }

    fun updateCategory(transactionId: Long, newCategory: String) {
        viewModelScope.launch {
            try {
                repository.updateCategory(transactionId, newCategory)
                _editError.value = null
                load()
            } catch (e: Exception) {
                _editError.value = e.message ?: "Erro ao atualizar categoria"
            }
        }
    }
}
