package com.faturaapp.ui.upload

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faturaapp.data.PreferencesRepository
import com.faturaapp.data.model.Fatura
import com.faturaapp.data.network.ApiClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

data class ArquivoSelecionado(val uri: Uri, val nome: String)

sealed class UploadState {
    data object Idle : UploadState()
    data object Enviando : UploadState()
    data class Sucesso(val fatura: Fatura) : UploadState()
    data class Erro(val mensagem: String) : UploadState()
}

class UploadViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application)

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
            try {
                val backendUrl = preferencesRepository.backendUrl.first()
                if (!backendUrl.isNullOrBlank()) {
                    val senhas = ApiClientProvider.getApi(backendUrl).listarSenhasPadrao()
                    _temSenhasCadastradas.value = senhas.isNotEmpty()
                }
            } catch (e: Exception) {
                // Mantem o campo de senha visivel se nao conseguir checar as cadastradas
            }
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
                val backendUrl = preferencesRepository.backendUrl.first()
                if (backendUrl.isNullOrBlank()) {
                    _uploadState.value = UploadState.Erro("Backend não configurado")
                    return@launch
                }

                val bytes = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openInputStream(arquivo.uri)?.use { it.readBytes() }
                } ?: throw IllegalStateException("Não foi possível ler o arquivo selecionado")

                val requestBody = bytes.toRequestBody("application/pdf".toMediaType())
                val part = MultipartBody.Part.createFormData("arquivo", arquivo.nome, requestBody)
                val senhaBody = _senha.value.trim().ifBlank { null }
                    ?.toRequestBody("text/plain".toMediaType())

                val fatura = ApiClientProvider.getApi(backendUrl).uploadFatura(part, senhaBody)
                _uploadState.value = UploadState.Sucesso(fatura)
            } catch (e: HttpException) {
                val mensagem = when (e.code()) {
                    401 -> "Senha incorreta ou fatura protegida por senha"
                    409 -> "Esta fatura já foi enviada anteriormente"
                    422 -> "Não foi possível identificar o banco desta fatura"
                    else -> "Erro do servidor (${e.code()})"
                }
                _uploadState.value = UploadState.Erro(mensagem)
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
