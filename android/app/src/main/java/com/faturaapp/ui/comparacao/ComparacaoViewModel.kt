package com.faturaapp.ui.comparacao

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faturaapp.data.PreferencesRepository
import com.faturaapp.data.model.ComparacaoMensal
import com.faturaapp.data.network.ApiClientProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MesAno(val mes: Int, val ano: Int) : Comparable<MesAno> {
    override fun compareTo(other: MesAno): Int =
        compareValuesBy(this, other, { it.ano }, { it.mes })

    fun paraPeriodo(): String = "%02d-%d".format(mes, ano)

    override fun toString(): String = "%02d/%d".format(mes, ano)
}

sealed class PeriodosState {
    data object Carregando : PeriodosState()
    data class Disponivel(val periodos: List<MesAno>) : PeriodosState()
    data class Erro(val mensagem: String) : PeriodosState()
}

sealed class ComparacaoUiState {
    data object Idle : ComparacaoUiState()
    data object Comparando : ComparacaoUiState()
    data class Resultado(val comparacao: ComparacaoMensal) : ComparacaoUiState()
    data class Erro(val mensagem: String) : ComparacaoUiState()
}

class ComparacaoViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application)

    private val _periodosState = MutableStateFlow<PeriodosState>(PeriodosState.Carregando)
    val periodosState: StateFlow<PeriodosState> = _periodosState.asStateFlow()

    private val _selecionados = MutableStateFlow<Set<MesAno>>(emptySet())
    val selecionados: StateFlow<Set<MesAno>> = _selecionados.asStateFlow()

    private val _comparacaoState = MutableStateFlow<ComparacaoUiState>(ComparacaoUiState.Idle)
    val comparacaoState: StateFlow<ComparacaoUiState> = _comparacaoState.asStateFlow()

    init {
        carregarPeriodosDisponiveis()
    }

    fun carregarPeriodosDisponiveis() {
        viewModelScope.launch {
            _periodosState.value = PeriodosState.Carregando
            val backendUrl = preferencesRepository.backendUrl.first()
            if (backendUrl.isNullOrBlank()) {
                _periodosState.value = PeriodosState.Erro("Backend não configurado")
                return@launch
            }
            try {
                val faturas = ApiClientProvider.getApi(backendUrl).listarFaturas()
                val periodos = faturas
                    .map { MesAno(it.mes_referencia, it.ano_referencia) }
                    .distinct()
                    .sorted()
                _periodosState.value = PeriodosState.Disponivel(periodos)
                _selecionados.value = periodos.takeLast(3).toSet()
            } catch (e: Exception) {
                _periodosState.value = PeriodosState.Erro(e.message ?: "Erro ao carregar períodos")
            }
        }
    }

    fun alternarSelecao(periodo: MesAno) {
        _selecionados.value = if (periodo in _selecionados.value) {
            _selecionados.value - periodo
        } else {
            _selecionados.value + periodo
        }
    }

    fun comparar() {
        val periodos = _selecionados.value.sorted()
        if (periodos.isEmpty()) {
            _comparacaoState.value = ComparacaoUiState.Erro("Selecione ao menos um mês")
            return
        }

        viewModelScope.launch {
            _comparacaoState.value = ComparacaoUiState.Comparando
            try {
                val backendUrl = preferencesRepository.backendUrl.first()
                if (backendUrl.isNullOrBlank()) {
                    _comparacaoState.value = ComparacaoUiState.Erro("Backend não configurado")
                    return@launch
                }
                val query = periodos.joinToString(",") { it.paraPeriodo() }
                val resultado = ApiClientProvider.getApi(backendUrl).compararMeses(query)
                _comparacaoState.value = ComparacaoUiState.Resultado(resultado)
            } catch (e: Exception) {
                _comparacaoState.value = ComparacaoUiState.Erro(e.message ?: "Erro ao comparar meses")
            }
        }
    }
}
