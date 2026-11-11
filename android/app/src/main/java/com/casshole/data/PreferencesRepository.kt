package com.casshole.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
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
        private val INVOICE_REMINDER_ENABLED_KEY = booleanPreferencesKey("invoice_reminder_enabled")
        private val INVOICE_REMINDER_LAST_RUN_KEY = longPreferencesKey("invoice_reminder_last_run")
        private val NOTIFICATION_PERMISSION_ASKED_KEY = booleanPreferencesKey("notification_permission_asked")
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

    /** Monthly "your invoice usually arrives today" notification - on by default. */
    val invoiceReminderEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[INVOICE_REMINDER_ENABLED_KEY] ?: true }

    suspend fun setInvoiceReminderEnabled(value: Boolean) {
        context.dataStore.edit { it[INVOICE_REMINDER_ENABLED_KEY] = value }
    }

    /** Epoch millis of the last reminder check: reminders at or before it are never sent again. */
    val invoiceReminderLastRun: Flow<Long?> =
        context.dataStore.data.map { it[INVOICE_REMINDER_LAST_RUN_KEY] }

    suspend fun setInvoiceReminderLastRun(value: Long) {
        context.dataStore.edit { it[INVOICE_REMINDER_LAST_RUN_KEY] = value }
    }

    /** The app asks for the notification permission on its own only once (Android 13+). */
    val notificationPermissionAsked: Flow<Boolean> =
        context.dataStore.data.map { it[NOTIFICATION_PERMISSION_ASKED_KEY] ?: false }

    suspend fun setNotificationPermissionAsked(value: Boolean) {
        context.dataStore.edit { it[NOTIFICATION_PERMISSION_ASKED_KEY] = value }
    }
}
