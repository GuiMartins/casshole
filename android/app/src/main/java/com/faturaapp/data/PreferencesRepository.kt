package com.faturaapp.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "fatura_app_prefs")

class PreferencesRepository(private val context: Context) {

    companion object {
        private val TEMA_KEY = stringPreferencesKey("tema_preferido")
        const val TEMA_SISTEMA = "sistema"
        const val TEMA_CLARO = "claro"
        const val TEMA_ESCURO = "escuro"
    }

    val temaPreferido: Flow<String> =
        context.dataStore.data.map { it[TEMA_KEY] ?: TEMA_SISTEMA }

    suspend fun setTemaPreferido(valor: String) {
        context.dataStore.edit { it[TEMA_KEY] = valor }
    }
}
