package com.faturaapp.ui.upload

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faturaapp.data.local.FaturaComTransacoes
import com.faturaapp.data.local.FaturaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ArquivoSelecionado(val uri: Uri, val nome: String)

sealed class UploadState {
    data object Idle : UploadState()
    data object Enviando : UploadState()
    data class Sucesso(val fatura: FaturaComTransacoes) : UploadState()
    data class Erro(val mensagem: String) : UploadState()
}

class UploadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FaturaRepository(application)

    private val _arquivoSelecionado = MutableStateFlow<ArquivoSelecionado?>(null)
    val arquivoSelecionado: StateFlow<ArquivoSelecionado?> = _arquivoSelecionado.asStateFlow()

    private val _senha = MutableStateFlow("")
    val senha: StateFlow<String> = _senha.asStateFlow()

    private val _temSenhasCadastradas = MutableStateFlow(false)
    val temSenhasCadastradas: StateFlow<Boolean> = _temSenhasCadastradas.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    init {
        viewModelScope.launch {
            _temSenhasCadastradas.value = repository.temSenhasCadastradas()
        }
    }

    fun onSenhaChange(novaSenha: String) {
        _senha.value = novaSenha
    }

    fun selecionarArquivo(uri: Uri) {
        val nome = resolverNomeArquivo(uri) ?: "fatura.pdf"
        _arquivoSelecionado.value = ArquivoSelecionado(uri, nome)
        _uploadState.value = UploadState.Idle
    }

    private fun resolverNomeArquivo(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }

    fun enviarFatura() {
        val arquivo = _arquivoSelecionado.value ?: run {
            _uploadState.value = UploadState.Erro("Selecione um arquivo PDF primeiro")
            return
        }

        viewModelScope.launch {
            _uploadState.value = UploadState.Enviando
            try {
                val bytes = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openInputStream(arquivo.uri)?.use { it.readBytes() }
                } ?: throw IllegalStateException("Não foi possível ler o arquivo selecionado")

                val senhaDigitada = _senha.value.trim().ifBlank { null }
                val fatura = repository.processarEArmazenar(bytes, senhaDigitada)
                _uploadState.value = UploadState.Sucesso(fatura)
            } catch (e: Exception) {
                _uploadState.value = UploadState.Erro(e.message ?: "Erro ao enviar a fatura")
            }
        }
    }

    fun limpar() {
        _arquivoSelecionado.value = null
        _uploadState.value = UploadState.Idle
    }
}
