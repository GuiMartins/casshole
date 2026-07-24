package com.faturaapp.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faturaapp.data.local.FaturaComTransacoes
import com.faturaapp.data.local.FaturaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DashboardState {
    data object Loading : DashboardState()
    data class Carregado(val faturas: List<FaturaComTransacoes>) : DashboardState()
    data class Erro(val mensagem: String) : DashboardState()
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FaturaRepository(application)

    private val _state = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    init {
        carregarFaturas()
    }

    fun carregarFaturas() {
        viewModelScope.launch {
            _state.value = DashboardState.Loading
            try {
                val faturas = repository.listarFaturas()
                _state.value = DashboardState.Carregado(faturas)
            } catch (e: Exception) {
                _state.value = DashboardState.Erro(e.message ?: "Erro ao carregar faturas")
            }
        }
    }
}
