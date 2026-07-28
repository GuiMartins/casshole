package com.casshole.ui.settings

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.casshole.data.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application)

    /** null means "follow the system language" (no per-app override set). */
    private val _appLanguage = MutableStateFlow(AppCompatDelegate.getApplicationLocales().get(0)?.language)
    val appLanguage: StateFlow<String?> = _appLanguage.asStateFlow()

    /** [languageTag] null reverts to following the system language. */
    fun selectLanguage(languageTag: String?) {
        val locales = if (languageTag == null) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
        _appLanguage.value = languageTag
    }

    val preferredTheme: StateFlow<String> = preferencesRepository.preferredTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PreferencesRepository.THEME_SYSTEM,
    )

    fun selectTheme(value: String) {
        viewModelScope.launch { preferencesRepository.setPreferredTheme(value) }
    }

    val summaryDisplayMode: StateFlow<String> = preferencesRepository.summaryDisplayMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PreferencesRepository.SUMMARY_DISPLAY_NUMBERS,
    )

    fun selectSummaryDisplayMode(value: String) {
        viewModelScope.launch { preferencesRepository.setSummaryDisplayMode(value) }
    }
}
