package com.moneyhole.ui.upload

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneyhole.R
import com.moneyhole.data.local.InvoiceWithTransactions
import com.moneyhole.data.local.InvoiceRepository
import com.moneyhole.data.local.DuplicatePeriodException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SelectedFile(val uri: Uri, val name: String)

sealed class UploadState {
    data object Idle : UploadState()
    data object Sending : UploadState()
    data class Success(val invoice: InvoiceWithTransactions) : UploadState()
    data class Error(val message: String) : UploadState()
}

class UploadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InvoiceRepository(application)

    private val _selectedFile = MutableStateFlow<SelectedFile?>(null)
    val selectedFile: StateFlow<SelectedFile?> = _selectedFile.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _savePassword = MutableStateFlow(false)
    val savePassword: StateFlow<Boolean> = _savePassword.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun onSavePasswordChange(value: Boolean) {
        _savePassword.value = value
    }

    fun selectFile(uri: Uri) {
        val name = resolveFileName(uri) ?: getApplication<Application>().getString(R.string.upload_default_filename)
        _selectedFile.value = SelectedFile(uri, name)
        _uploadState.value = UploadState.Idle
    }

    private fun resolveFileName(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }

    fun sendInvoice() {
        val file = _selectedFile.value ?: run {
            _uploadState.value = UploadState.Error(getApplication<Application>().getString(R.string.error_select_file))
            return
        }

        viewModelScope.launch {
            _uploadState.value = UploadState.Sending
            try {
                val bytes = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openInputStream(file.uri)?.use { it.readBytes() }
                } ?: throw IllegalStateException(getApplication<Application>().getString(R.string.error_read_file))

                val enteredPassword = _password.value.trim().ifBlank { null }
                val invoice = repository.processAndStore(bytes, enteredPassword)
                saveDefaultPasswordIfNeeded(enteredPassword)
                _uploadState.value = UploadState.Success(invoice)
            } catch (e: DuplicatePeriodException) {
                // The password was already validated successfully (the PDF
                // was opened and identified) before this duplicate check, so
                // it's still worth saving.
                saveDefaultPasswordIfNeeded(_password.value.trim().ifBlank { null })
                _uploadState.value = UploadState.Error(e.message ?: getApplication<Application>().getString(R.string.error_invoice_duplicate))
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(e.message ?: getApplication<Application>().getString(R.string.error_upload_invoice))
            }
        }
    }

    private suspend fun saveDefaultPasswordIfNeeded(enteredPassword: String?) {
        if (_savePassword.value && enteredPassword != null) {
            try {
                repository.addDefaultPassword(enteredPassword)
            } catch (e: SQLiteConstraintException) {
                // Password was already saved as default, nothing to do.
            }
        }
    }

    fun clear() {
        _selectedFile.value = null
        _uploadState.value = UploadState.Idle
    }
}
