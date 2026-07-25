package com.moneyhole.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "fatura_app_prefs")

class PreferencesRepository(private val context: Context) {

    companion object {
        private val THEME_KEY = stringPreferencesKey("tema_preferido")
        const val THEME_SYSTEM = "sistema"
        const val THEME_LIGHT = "claro"
        const val THEME_DARK = "escuro"

        private val SUMMARY_DISPLAY_KEY = stringPreferencesKey("summary_display_mode")
        const val SUMMARY_DISPLAY_NUMBERS = "numbers"
        const val SUMMARY_DISPLAY_PIE_CHART = "pie_chart"
    }

    val preferredTheme: Flow<String> =
        context.dataStore.data.map { it[THEME_KEY] ?: THEME_SYSTEM }

    suspend fun setPreferredTheme(value: String) {
        context.dataStore.edit { it[THEME_KEY] = value }
    }

    val summaryDisplayMode: Flow<String> =
        context.dataStore.data.map { it[SUMMARY_DISPLAY_KEY] ?: SUMMARY_DISPLAY_NUMBERS }

    suspend fun setSummaryDisplayMode(value: String) {
        context.dataStore.edit { it[SUMMARY_DISPLAY_KEY] = value }
    }
}
