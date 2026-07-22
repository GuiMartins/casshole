package com.faturaapp.ui.faturadetalhe

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.faturaapp.data.PreferencesRepository
import com.faturaapp.data.model.Fatura
import com.faturaapp.data.model.TransacaoUpdate
import com.faturaapp.data.network.ApiClientProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class CategoriaTotal(val categoria: String, val total: Double)

sealed class FaturaDetalheState {
    data object Carregando : FaturaDetalheState()
    data class Carregado(val fatura: Fatura, val porCategoria: List<CategoriaTotal>) : FaturaDetalheState()
    data class Erro(val mensagem: String) : FaturaDetalheState()
}

class FaturaDetalheViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application)

    private val _state = MutableStateFlow<FaturaDetalheState>(FaturaDetalheState.Carregando)
    val state: StateFlow<FaturaDetalheState> = _state.asStateFlow()

    private val _categoriasDisponiveis = MutableStateFlow<List<String>>(emptyList())
    val categoriasDisponiveis: StateFlow<List<String>> = _categoriasDisponiveis.asStateFlow()

    private val _erroEdicao = MutableStateFlow<String?>(null)
    val erroEdicao: StateFlow<String?> = _erroEdicao.asStateFlow()

    init {
        carregar()
        carregarCategorias()
    }

    fun carregar() {
        val faturaId = savedStateHandle.get<Int>("faturaId") ?: run {
            _state.value = FaturaDetalheState.Erro("ID da fatura inválido")
            return
        }

        viewModelScope.launch {
            _state.value = FaturaDetalheState.Carregando
            try {
                val backendUrl = preferencesRepository.backendUrl.first()
                if (backendUrl.isNullOrBlank()) {
                    _state.value = FaturaDetalheState.Erro("Backend não configurado")
                    return@launch
                }
                val fatura = ApiClientProvider.getApi(backendUrl).obterFatura(faturaId)
                val porCategoria = fatura.transacoes
                    .groupBy { it.categoria }
                    .map { (categoria, transacoes) -> CategoriaTotal(categoria, transacoes.sumOf { it.valor }) }
                    .sortedByDescending { it.total }
                _state.value = FaturaDetalheState.Carregado(fatura, porCategoria)
            } catch (e: Exception) {
                _state.value = FaturaDetalheState.Erro(e.message ?: "Erro ao carregar fatura")
            }
        }
    }

    private fun carregarCategorias() {
        viewModelScope.launch {
            try {
                val backendUrl = preferencesRepository.backendUrl.first() ?: return@launch
                _categoriasDisponiveis.value = ApiClientProvider.getApi(backendUrl).listarCategorias()
            } catch (e: Exception) {
                // Silencioso: o dialog de edicao so nao tera opcoes se isso falhar
            }
        }
    }

    fun atualizarCategoria(transacaoId: Int, novaCategoria: String) {
        viewModelScope.launch {
            try {
                val backendUrl = preferencesRepository.backendUrl.first() ?: return@launch
                ApiClientProvider.getApi(backendUrl).atualizarTransacao(
                    transacaoId,
                    TransacaoUpdate(novaCategoria),
                )
                _erroEdicao.value = null
                carregar()
            } catch (e: Exception) {
                _erroEdicao.value = e.message ?: "Erro ao atualizar categoria"
            }
        }
    }
}
