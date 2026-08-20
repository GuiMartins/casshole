package com.casshole.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        private val AMOUNTS_HIDDEN_KEY = booleanPreferencesKey("amounts_hidden")
        private val CATEGORIZER_REVISION_KEY = intPreferencesKey("categorizer_revision")
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

    val onboardingCompleted: Flow<Boolean> =
        context.dataStore.data.map { it[ONBOARDING_COMPLETED_KEY] ?: false }

    suspend fun setOnboardingCompleted(value: Boolean) {
        context.dataStore.edit { it[ONBOARDING_COMPLETED_KEY] = value }
    }

    /** Global "hide amounts" toggle (the eye icon, like banking apps) - one preference shared
     * across every screen that shows money, toggled from the Dashboard's top bar. */
    val amountsHidden: Flow<Boolean> =
        context.dataStore.data.map { it[AMOUNTS_HIDDEN_KEY] ?: false }

    suspend fun setAmountsHidden(value: Boolean) {
        context.dataStore.edit { it[AMOUNTS_HIDDEN_KEY] = value }
    }

    /** Which revision of the categorization rules the stored invoices were run through. */
    val categorizerRevision: Flow<Int> =
        context.dataStore.data.map { it[CATEGORIZER_REVISION_KEY] ?: 0 }

    suspend fun setCategorizerRevision(value: Int) {
        context.dataStore.edit { it[CATEGORIZER_REVISION_KEY] = value }
    }
}
