package com.faturaapp.ui.faturasporcartao

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faturaapp.data.local.FaturaComTransacoes
import com.faturaapp.data.local.FaturaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FaturasPorCartaoState {
    data object Loading : FaturasPorCartaoState()
    data class Carregado(val faturas: List<FaturaComTransacoes>) : FaturasPorCartaoState()
    data class Erro(val mensagem: String) : FaturasPorCartaoState()
}

class FaturasPorCartaoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FaturaRepository(application)

    private val _state = MutableStateFlow<FaturasPorCartaoState>(FaturasPorCartaoState.Loading)
    val state: StateFlow<FaturasPorCartaoState> = _state.asStateFlow()

    init {
        carregarFaturas()
    }

    fun carregarFaturas() {
        viewModelScope.launch {
            _state.value = FaturasPorCartaoState.Loading
            try {
                _state.value = FaturasPorCartaoState.Carregado(repository.listarFaturas())
            } catch (e: Exception) {
                _state.value = FaturasPorCartaoState.Erro(e.message ?: "Erro ao carregar faturas")
            }
        }
    }
}
