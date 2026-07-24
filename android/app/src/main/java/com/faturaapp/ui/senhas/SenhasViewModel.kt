package com.faturaapp.ui.senhas

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faturaapp.data.PreferencesRepository
import com.faturaapp.data.model.SenhaPadrao
import com.faturaapp.data.model.SenhaPadraoCreate
import com.faturaapp.data.network.ApiClientProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed class SenhasState {
    data object Carregando : SenhasState()
    data class Carregado(val senhas: List<SenhaPadrao>) : SenhasState()
    data class Erro(val mensagem: String) : SenhasState()
}

class SenhasViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application)

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
                val backendUrl = backendUrlOuFalha() ?: return@launch
                val senhas = ApiClientProvider.getApi(backendUrl).listarSenhasPadrao()
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
                val backendUrl = backendUrlOuFalha() ?: return@launch
                ApiClientProvider.getApi(backendUrl).criarSenhaPadrao(SenhaPadraoCreate(valor.trim()))
                _erroAcao.value = null
                carregar()
            } catch (e: HttpException) {
                _erroAcao.value = if (e.code() == 409) "Esta senha já está cadastrada" else "Erro ao salvar (${e.code()})"
            } catch (e: Exception) {
                _erroAcao.value = e.message ?: "Erro ao salvar senha"
            }
        }
    }

    fun remover(id: Int) {
        viewModelScope.launch {
            try {
                val backendUrl = backendUrlOuFalha() ?: return@launch
                ApiClientProvider.getApi(backendUrl).removerSenhaPadrao(id)
                carregar()
            } catch (e: Exception) {
                _erroAcao.value = e.message ?: "Erro ao remover senha"
            }
        }
    }

    private suspend fun backendUrlOuFalha(): String? {
        val backendUrl = preferencesRepository.backendUrl.first()
        if (backendUrl.isNullOrBlank()) {
            _state.value = SenhasState.Erro("Backend não configurado")
            return null
        }
        return backendUrl
    }
}
