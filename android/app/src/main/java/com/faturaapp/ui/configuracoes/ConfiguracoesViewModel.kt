package com.faturaapp.ui.configuracoes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faturaapp.data.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConfiguracoesViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application)

    val temaPreferido: StateFlow<String> = preferencesRepository.temaPreferido.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PreferencesRepository.TEMA_SISTEMA,
    )

    fun selecionarTema(valor: String) {
        viewModelScope.launch { preferencesRepository.setTemaPreferido(valor) }
    }
}
