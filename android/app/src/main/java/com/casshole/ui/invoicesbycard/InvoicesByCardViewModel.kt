package com.casshole.ui.invoicesbycard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.casshole.R
import com.casshole.data.PreferencesRepository
import com.casshole.data.local.InvoiceWithTransactions
import com.casshole.data.local.InvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class InvoicesByCardState {
    data object Loading : InvoicesByCardState()
    data class Loaded(
        val invoices: List<InvoiceWithTransactions>,
        val nicknames: Map<Pair<String, String>, String>,
    ) : InvoicesByCardState()
    data class Error(val message: String) : InvoicesByCardState()
}

class InvoicesByCardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InvoiceRepository(application)
    private val preferencesRepository = PreferencesRepository(application)

    private val _state = MutableStateFlow<InvoicesByCardState>(InvoicesByCardState.Loading)
    val state: StateFlow<InvoicesByCardState> = _state.asStateFlow()

    val amountsHidden: StateFlow<Boolean> = preferencesRepository.amountsHidden.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    init {
        loadInvoices()
    }

    fun loadInvoices() {
        viewModelScope.launch {
            _state.value = InvoicesByCardState.Loading
            try {
                val invoices = repository.listInvoices()
                val nicknames = repository.listCardNicknames().associate { (it.bank to it.card) to it.nickname }
                _state.value = InvoicesByCardState.Loaded(invoices, nicknames)
            } catch (e: Exception) {
                _state.value = InvoicesByCardState.Error(e.message ?: getApplication<Application>().getString(R.string.error_load_invoices))
            }
        }
    }

    fun setNickname(bank: String, card: String, nickname: String) {
        viewModelScope.launch {
            repository.setCardNickname(bank, card, nickname)
            loadInvoices()
        }
    }
}
