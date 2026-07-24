package com.faturaapp.ui.categorias

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faturaapp.data.local.FaturaRepository
import com.faturaapp.data.local.entity.CategoriaOverrideEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CategoriaOverridesState {
    data object Carregando : CategoriaOverridesState()
    data class Carregado(val overrides: List<CategoriaOverrideEntity>) : CategoriaOverridesState()
    data class Erro(val mensagem: String) : CategoriaOverridesState()
}

class CategoriaOverridesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FaturaRepository(application)

    private val _state = MutableStateFlow<CategoriaOverridesState>(CategoriaOverridesState.Carregando)
    val state: StateFlow<CategoriaOverridesState> = _state.asStateFlow()

    init {
        carregar()
    }

    fun carregar() {
        viewModelScope.launch {
            _state.value = CategoriaOverridesState.Carregando
            try {
                _state.value = CategoriaOverridesState.Carregado(repository.listarCategoriaOverrides())
            } catch (e: Exception) {
                _state.value = CategoriaOverridesState.Erro(e.message ?: "Erro ao carregar categorizações")
            }
        }
    }

    fun remover(id: Long) {
        viewModelScope.launch {
            repository.removerCategoriaOverride(id)
            carregar()
        }
    }
}
