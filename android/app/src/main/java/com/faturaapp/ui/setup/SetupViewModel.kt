package com.faturaapp.ui.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faturaapp.data.PreferencesRepository
import com.faturaapp.data.network.ApiClientProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class ConnectionTestState {
    data object Idle : ConnectionTestState()
    data object Testing : ConnectionTestState()
    data object Success : ConnectionTestState()
    data class Failure(val mensagem: String) : ConnectionTestState()
}

class SetupViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application)

    private val _url = MutableStateFlow("")
    val url: StateFlow<String> = _url.asStateFlow()

    private val _testState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val testState: StateFlow<ConnectionTestState> = _testState.asStateFlow()

    private val _setupConcluido = MutableStateFlow(false)
    val setupConcluido: StateFlow<Boolean> = _setupConcluido.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.backendUrl.first()?.let { savedUrl ->
                _url.value = savedUrl
                if (testarConexaoSuspensa()) continuar()
            }
        }
    }

    fun onUrlChange(novaUrl: String) {
        _url.value = novaUrl
        _testState.value = ConnectionTestState.Idle
    }

    fun testarConexao() {
        viewModelScope.launch { testarConexaoSuspensa() }
    }

    private suspend fun testarConexaoSuspensa(): Boolean {
        val urlAtual = _url.value.trim()
        if (urlAtual.isBlank()) {
            _testState.value = ConnectionTestState.Failure("Informe o endereço do servidor")
            return false
        }

        _testState.value = ConnectionTestState.Testing
        return try {
            ApiClientProvider.getApi(urlAtual).listarFaturas()
            _testState.value = ConnectionTestState.Success
            preferencesRepository.setBackendUrl(urlAtual)
            true
        } catch (e: Exception) {
            _testState.value = ConnectionTestState.Failure(
                e.message ?: "Não foi possível conectar ao servidor"
            )
            false
        }
    }

    fun continuar() {
        if (_testState.value == ConnectionTestState.Success) {
            _setupConcluido.value = true
        }
    }
}
