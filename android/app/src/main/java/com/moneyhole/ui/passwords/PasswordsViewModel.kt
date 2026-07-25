package com.moneyhole.ui.passwords

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneyhole.data.local.InvoiceRepository
import com.moneyhole.data.local.entity.DefaultPasswordEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PasswordsState {
    data object Loading : PasswordsState()
    data class Loaded(val passwords: List<DefaultPasswordEntity>) : PasswordsState()
    data class Error(val message: String) : PasswordsState()
}

class PasswordsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InvoiceRepository(application)

    private val _state = MutableStateFlow<PasswordsState>(PasswordsState.Loading)
    val state: StateFlow<PasswordsState> = _state.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = PasswordsState.Loading
            try {
                val passwords = repository.listDefaultPasswords()
                _state.value = PasswordsState.Loaded(passwords)
            } catch (e: Exception) {
                _state.value = PasswordsState.Error(e.message ?: "Erro ao carregar senhas")
            }
        }
    }

    fun add(value: String) {
        if (value.isBlank()) {
            _actionError.value = "Informe uma senha"
            return
        }
        viewModelScope.launch {
            try {
                repository.addDefaultPassword(value.trim())
                _actionError.value = null
                load()
            } catch (e: SQLiteConstraintException) {
                _actionError.value = "Esta senha já está cadastrada"
            } catch (e: Exception) {
                _actionError.value = e.message ?: "Erro ao salvar senha"
            }
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            try {
                repository.removeDefaultPassword(id)
                load()
            } catch (e: Exception) {
                _actionError.value = e.message ?: "Erro ao remover senha"
            }
        }
    }
}
