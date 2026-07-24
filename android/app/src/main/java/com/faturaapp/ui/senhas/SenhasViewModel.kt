package com.faturaapp.ui.senhas

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faturaapp.data.local.FaturaRepository
import com.faturaapp.data.local.entity.SenhaPadraoEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SenhasState {
    data object Carregando : SenhasState()
    data class Carregado(val senhas: List<SenhaPadraoEntity>) : SenhasState()
    data class Erro(val mensagem: String) : SenhasState()
}

class SenhasViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FaturaRepository(application)

    private val _state = MutableStateFlow<SenhasState>(SenhasState.Carregando)
    val state: StateFlow<SenhasState> = _state.asStateFlow()

    private val _erroAcao = MutableStateFlow<String?>(null)
    val erroAcao: StateFlow<String?> = _erroAcao.asStateFlow()

    init {
        carregar()
    }

    fun carregar() {
        viewModelScope.launch {
            _state.value = SenhasState.Carregando
            try {
                val senhas = repository.listarSenhasPadrao()
                _state.value = SenhasState.Carregado(senhas)
            } catch (e: Exception) {
                _state.value = SenhasState.Erro(e.message ?: "Erro ao carregar senhas")
            }
        }
    }

    fun adicionar(valor: String) {
        if (valor.isBlank()) {
            _erroAcao.value = "Informe uma senha"
            return
        }
        viewModelScope.launch {
            try {
                repository.adicionarSenhaPadrao(valor.trim())
                _erroAcao.value = null
                carregar()
            } catch (e: SQLiteConstraintException) {
                _erroAcao.value = "Esta senha já está cadastrada"
            } catch (e: Exception) {
                _erroAcao.value = e.message ?: "Erro ao salvar senha"
            }
        }
    }

    fun remover(id: Long) {
        viewModelScope.launch {
            try {
                repository.removerSenhaPadrao(id)
                carregar()
            } catch (e: Exception) {
                _erroAcao.value = e.message ?: "Erro ao remover senha"
            }
        }
    }
}
