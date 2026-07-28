package com.casshole.ui.email

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.casshole.R
import com.casshole.data.email.EmailCredentials
import com.casshole.data.email.EmailCredentialsRepository
import com.casshole.data.email.EmailFetchCoordinator
import com.casshole.data.email.EmailFetchState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EmailSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val credentialsRepository = EmailCredentialsRepository(application)

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address.asStateFlow()

    private val _appPassword = MutableStateFlow("")
    val appPassword: StateFlow<String> = _appPassword.asStateFlow()

    private val _imapHost = MutableStateFlow(EmailCredentialsRepository.DEFAULT_IMAP_HOST)
    val imapHost: StateFlow<String> = _imapHost.asStateFlow()

    /** Shared with the Dashboard's in-progress banner and the auto-fetch-on-launch trigger. */
    val fetchState: StateFlow<EmailFetchState> = EmailFetchCoordinator.state

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    init {
        credentialsRepository.get()?.let { creds ->
            _address.value = creds.address
            _appPassword.value = creds.appPassword
            _imapHost.value = creds.imapHost
        }
    }

    fun onAddressChange(value: String) { _address.value = value; _validationError.value = null }
    fun onAppPasswordChange(value: String) { _appPassword.value = value; _validationError.value = null }
    fun onImapHostChange(value: String) { _imapHost.value = value; _validationError.value = null }

    /** Validates, persists, and immediately fetches - one action instead of separate Save/Fetch steps. */
    fun saveAndFetch() {
        val credentials = EmailCredentials(
            address = _address.value.trim(),
            appPassword = _appPassword.value.trim(),
            imapHost = _imapHost.value.trim().ifBlank { EmailCredentialsRepository.DEFAULT_IMAP_HOST },
            imapPort = EmailCredentialsRepository.DEFAULT_IMAP_PORT,
        )
        if (credentials.address.isBlank() || credentials.appPassword.isBlank()) {
            _validationError.value = getApplication<Application>().getString(R.string.email_validation_error)
            return
        }
        _validationError.value = null
        credentialsRepository.save(credentials)
        EmailFetchCoordinator.fetchNow(getApplication(), credentials)
    }
}
