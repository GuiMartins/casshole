package com.casshole.ui.invoicedetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.casshole.R
import com.casshole.categorizer.AVAILABLE_CATEGORIES
import com.casshole.data.PreferencesRepository
import com.casshole.data.local.InvoiceWithTransactions
import com.casshole.data.local.InvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryTotal(val category: String, val total: Double)

sealed class InvoiceDetailState {
    data object Loading : InvoiceDetailState()
    data class Loaded(
        val invoice: InvoiceWithTransactions,
        val byCategory: List<CategoryTotal>,
        val cardNickname: String?,
    ) : InvoiceDetailState()
    data class Error(val message: String) : InvoiceDetailState()
}

class InvoiceDetailViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val repository = InvoiceRepository(application)
    private val preferencesRepository = PreferencesRepository(application)

    private val _state = MutableStateFlow<InvoiceDetailState>(InvoiceDetailState.Loading)
    val state: StateFlow<InvoiceDetailState> = _state.asStateFlow()

    val availableCategories: StateFlow<List<String>> = MutableStateFlow(AVAILABLE_CATEGORIES)

    val amountsHidden: StateFlow<Boolean> = preferencesRepository.amountsHidden.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    private val _editError = MutableStateFlow<String?>(null)
    val editError: StateFlow<String?> = _editError.asStateFlow()

    init {
        load()
    }

    fun load() {
        val invoiceId = savedStateHandle.get<Long>("invoiceId") ?: run {
            _state.value = InvoiceDetailState.Error(getApplication<Application>().getString(R.string.error_invalid_invoice_id))
            return
        }

        viewModelScope.launch {
            _state.value = InvoiceDetailState.Loading
            try {
                val invoice = repository.getInvoice(invoiceId)
                    ?: throw IllegalStateException(getApplication<Application>().getString(R.string.error_invoice_not_found))
                val byCategory = invoice.transactions
                    .groupBy { it.category }
                    .map { (category, transactions) -> CategoryTotal(category, transactions.sumOf { it.amount }) }
                    .sortedByDescending { it.total }
                val cardNickname = repository.listCardNicknames()
                    .find { it.bank == invoice.bank && it.card == invoice.card }?.nickname
                _state.value = InvoiceDetailState.Loaded(invoice, byCategory, cardNickname)
            } catch (e: Exception) {
                _state.value = InvoiceDetailState.Error(e.message ?: getApplication<Application>().getString(R.string.error_load_invoice))
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
                _editError.value = e.message ?: getApplication<Application>().getString(R.string.error_update_category)
            }
        }
    }
}
