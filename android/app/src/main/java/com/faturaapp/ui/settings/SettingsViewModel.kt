package com.faturaapp.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.faturaapp.data.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application)

    val preferredTheme: StateFlow<String> = preferencesRepository.preferredTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PreferencesRepository.THEME_SYSTEM,
    )

    fun selectTheme(value: String) {
        viewModelScope.launch { preferencesRepository.setPreferredTheme(value) }
    }
}
