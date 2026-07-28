package com.casshole.ui.passwords

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.casshole.R
import com.casshole.data.local.InvoiceRepository
import com.casshole.data.local.entity.DefaultPasswordEntity
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
                _state.value = PasswordsState.Error(e.message ?: getApplication<Application>().getString(R.string.error_load_passwords))
            }
        }
    }

    fun add(value: String) {
        if (value.isBlank()) {
            _actionError.value = getApplication<Application>().getString(R.string.error_password_required)
            return
        }
        viewModelScope.launch {
            try {
                repository.addDefaultPassword(value.trim())
                _actionError.value = null
                load()
            } catch (e: SQLiteConstraintException) {
                _actionError.value = getApplication<Application>().getString(R.string.error_password_duplicate)
            } catch (e: Exception) {
                _actionError.value = e.message ?: getApplication<Application>().getString(R.string.error_save_password)
            }
        }
    }

    fun remove(id: Long) {
        viewModelScope.launch {
            try {
                repository.removeDefaultPassword(id)
                load()
            } catch (e: Exception) {
                _actionError.value = e.message ?: getApplication<Application>().getString(R.string.error_remove_password)
            }
        }
    }
}
