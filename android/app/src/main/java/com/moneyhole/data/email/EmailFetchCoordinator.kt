package com.moneyhole.data.email

import android.content.Context
import android.util.Log
import com.moneyhole.R
import com.moneyhole.data.local.DuplicateFileException
import com.moneyhole.data.local.DuplicatePeriodException
import com.moneyhole.data.local.InvoiceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

/**
 * App-wide singleton coordinating email fetches. Both the automatic
 * fetch-on-launch trigger and the manual "Fetch invoices now" button go
 * through here, so progress/result is visible from any screen (Dashboard,
 * Email Settings) and a fetch already running is never started twice.
 */
object EmailFetchCoordinator {

    private const val TAG = "EmailFetchCoordinator"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<EmailFetchState>(EmailFetchState.Idle)
    val state: StateFlow<EmailFetchState> = _state.asStateFlow()

    private var autoFetchTriggered = false

    /** Runs once per process lifetime, and only if an email account is already configured. */
    fun fetchOnAppStart(context: Context) {
        if (autoFetchTriggered) return
        autoFetchTriggered = true
        val appContext = context.applicationContext
        val credentials = EmailCredentialsRepository(appContext).get() ?: return
        fetch(appContext, credentials)
    }

    /** Manual trigger from the Email Settings screen - always runs, even right after auto-fetch. */
    fun fetchNow(context: Context, credentials: EmailCredentials) {
        fetch(context.applicationContext, credentials)
    }

    private fun fetch(context: Context, credentials: EmailCredentials) {
        if (_state.value is EmailFetchState.Fetching) return

        val credentialsRepository = EmailCredentialsRepository(context)
        val invoiceRepository = InvoiceRepository(context)

        scope.launch {
            _state.value = EmailFetchState.Fetching()
            try {
                val lastProcessedUid = credentialsRepository.getLastProcessedUid()
                val result = withContext(Dispatchers.IO) {
                    EmailFetcher.fetchPdfAttachments(credentials, lastProcessedUid) { processed, total ->
                        _state.value = EmailFetchState.Fetching(processed, total)
                    }
                }

                var imported = 0
                var duplicates = 0
                var failed = 0
                for (attachment in result.attachments) {
                    try {
                        invoiceRepository.processAndStore(attachment.bytes, enteredPassword = null)
                        imported++
                    } catch (e: DuplicateFileException) {
                        duplicates++
                    } catch (e: DuplicatePeriodException) {
                        duplicates++
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to import attachment '${attachment.fileName}'", e)
                        failed++
                    }
                }
                credentialsRepository.setLastProcessedUid(result.lastUid)
                _state.value = EmailFetchState.Done(EmailImportResult(imported, duplicates, failed))
            } catch (e: EmailAuthenticationException) {
                _state.value = EmailFetchState.Error(context.getString(R.string.email_error_auth))
            } catch (e: EmailConnectionException) {
                _state.value = EmailFetchState.Error(context.getString(R.string.email_error_connection))
            } catch (e: Exception) {
                _state.value = EmailFetchState.Error(e.message ?: context.getString(R.string.email_error_generic))
            }
        }
    }
}
