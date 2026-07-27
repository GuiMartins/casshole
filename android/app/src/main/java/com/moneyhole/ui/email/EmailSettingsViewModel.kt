package com.moneyhole.ui.email

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneyhole.R
import com.moneyhole.data.email.EmailAuthenticationException
import com.moneyhole.data.email.EmailConnectionException
import com.moneyhole.data.email.EmailCredentials
import com.moneyhole.data.email.EmailCredentialsRepository
import com.moneyhole.data.email.EmailFetcher
import com.moneyhole.data.local.DuplicateFileException
import com.moneyhole.data.local.DuplicatePeriodException
import com.moneyhole.data.local.InvoiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EmailImportResult(val imported: Int, val duplicates: Int, val failed: Int)

sealed class EmailFetchState {
    data object Idle : EmailFetchState()
    data class Fetching(val processed: Int = 0, val total: Int = 0) : EmailFetchState()
    data class Done(val result: EmailImportResult) : EmailFetchState()
    data class Error(val message: String) : EmailFetchState()
}

class EmailSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val credentialsRepository = EmailCredentialsRepository(application)
    private val invoiceRepository = InvoiceRepository(application)

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address.asStateFlow()

    private val _appPassword = MutableStateFlow("")
    val appPassword: StateFlow<String> = _appPassword.asStateFlow()

    private val _imapHost = MutableStateFlow(EmailCredentialsRepository.DEFAULT_IMAP_HOST)
    val imapHost: StateFlow<String> = _imapHost.asStateFlow()

    private val _fetchState = MutableStateFlow<EmailFetchState>(EmailFetchState.Idle)
    val fetchState: StateFlow<EmailFetchState> = _fetchState.asStateFlow()

    init {
        credentialsRepository.get()?.let { creds ->
            _address.value = creds.address
            _appPassword.value = creds.appPassword
            _imapHost.value = creds.imapHost
        }
    }

    fun onAddressChange(value: String) { _address.value = value }
    fun onAppPasswordChange(value: String) { _appPassword.value = value }
    fun onImapHostChange(value: String) { _imapHost.value = value }

    /** Validates, persists, and immediately fetches - one action instead of separate Save/Fetch steps. */
    fun saveAndFetch() {
        val credentials = EmailCredentials(
            address = _address.value.trim(),
            appPassword = _appPassword.value.trim(),
            imapHost = _imapHost.value.trim().ifBlank { EmailCredentialsRepository.DEFAULT_IMAP_HOST },
            imapPort = EmailCredentialsRepository.DEFAULT_IMAP_PORT,
        )
        if (credentials.address.isBlank() || credentials.appPassword.isBlank()) {
            _fetchState.value = EmailFetchState.Error(
                getApplication<Application>().getString(R.string.email_validation_error)
            )
            return
        }
        credentialsRepository.save(credentials)

        viewModelScope.launch {
            _fetchState.value = EmailFetchState.Fetching()
            try {
                val attachments = withContext(Dispatchers.IO) {
                    EmailFetcher.fetchPdfAttachments(credentials) { processed, total ->
                        _fetchState.value = EmailFetchState.Fetching(processed, total)
                    }
                }

                var imported = 0
                var duplicates = 0
                var failed = 0
                for (attachment in attachments) {
                    try {
                        invoiceRepository.processAndStore(attachment.bytes, enteredPassword = null)
                        imported++
                    } catch (e: DuplicateFileException) {
                        duplicates++
                    } catch (e: DuplicatePeriodException) {
                        duplicates++
                    } catch (e: Exception) {
                        failed++
                    }
                }
                _fetchState.value = EmailFetchState.Done(EmailImportResult(imported, duplicates, failed))
            } catch (e: EmailAuthenticationException) {
                _fetchState.value = EmailFetchState.Error(
                    getApplication<Application>().getString(R.string.email_error_auth)
                )
            } catch (e: EmailConnectionException) {
                _fetchState.value = EmailFetchState.Error(
                    getApplication<Application>().getString(R.string.email_error_connection)
                )
            } catch (e: Exception) {
                _fetchState.value = EmailFetchState.Error(
                    e.message ?: getApplication<Application>().getString(R.string.email_error_generic)
                )
            }
        }
    }
}
