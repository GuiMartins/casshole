package com.faturaapp.ui.faturadetalhe

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.faturaapp.categorizer.CATEGORIAS_DISPONIVEIS
import com.faturaapp.data.local.FaturaComTransacoes
import com.faturaapp.data.local.FaturaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoriaTotal(val categoria: String, val total: Double)

sealed class FaturaDetalheState {
    data object Carregando : FaturaDetalheState()
    data class Carregado(val fatura: FaturaComTransacoes, val porCategoria: List<CategoriaTotal>) : FaturaDetalheState()
    data class Erro(val mensagem: String) : FaturaDetalheState()
}

class FaturaDetalheViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val repository = FaturaRepository(application)

    private val _state = MutableStateFlow<FaturaDetalheState>(FaturaDetalheState.Carregando)
    val state: StateFlow<FaturaDetalheState> = _state.asStateFlow()

    val categoriasDisponiveis: StateFlow<List<String>> = MutableStateFlow(CATEGORIAS_DISPONIVEIS)

    private val _erroEdicao = MutableStateFlow<String?>(null)
    val erroEdicao: StateFlow<String?> = _erroEdicao.asStateFlow()

    init {
        carregar()
    }

    fun carregar() {
        val faturaId = savedStateHandle.get<Long>("faturaId") ?: run {
            _state.value = FaturaDetalheState.Erro("ID da fatura inválido")
            return
        }

        viewModelScope.launch {
            _state.value = FaturaDetalheState.Carregando
            try {
                val fatura = repository.obterFatura(faturaId)
                    ?: throw IllegalStateException("Fatura não encontrada")
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

    fun atualizarCategoria(transacaoId: Long, novaCategoria: String) {
        viewModelScope.launch {
            try {
                repository.atualizarCategoria(transacaoId, novaCategoria)
                _erroEdicao.value = null
                carregar()
            } catch (e: Exception) {
                _erroEdicao.value = e.message ?: "Erro ao atualizar categoria"
            }
        }
    }
}
