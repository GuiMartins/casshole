package com.moneyhole.data.email

import android.content.Context
import android.util.Log
import com.moneyhole.R
import com.moneyhole.data.local.DuplicateFileException
import com.moneyhole.data.local.DuplicatePeriodException
import com.moneyhole.data.local.InvoiceRepository
import com.moneyhole.parsing.IncorrectPasswordException
import com.moneyhole.parsing.NotABankInvoiceException
import com.moneyhole.parsing.UnsupportedBankException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** [failedPasswords] is a subset of [failed] - attachments that specifically need a default
 * password registered (Configurações > Senhas) to be imported, worth surfacing separately since
 * it's an actionable fix, unlike an unrecognized-bank or other parsing failure. [unsupportedBanks]
 * is kept OUT of [failed] entirely - it's not an error, just an FYI that a bank invoice was seen
 * but isn't supported yet (nothing the user can do about it, unlike a wrong/missing password).
 * Attachments that aren't a bank invoice at all (random PDFs in the inbox) are silently skipped
 * and don't show up in any of these counts - see [NotABankInvoiceException]. */
data class EmailImportResult(
    val imported: Int,
    val duplicates: Int,
    val failed: Int,
    val failedPasswords: Int,
    val unsupportedBanks: Set<String> = emptySet(),
)

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
                val previouslyFailedUids = credentialsRepository.getFailedUids()
                val result = withContext(Dispatchers.IO) {
                    EmailFetcher.fetchPdfAttachments(credentials, lastProcessedUid, previouslyFailedUids) { processed, total ->
                        _state.value = EmailFetchState.Fetching(processed, total)
                    }
                }

                var imported = 0
                var duplicates = 0
                var failed = 0
                var failedPasswords = 0
                val unsupportedBanks = mutableSetOf<String>()
                // Only messages that still fail this time stay on the retry list - anything
                // imported or already-duplicate this round is done and drops off it. This list
                // is what gets retried next time (regardless of the main cursor), so a message
                // that failed for a fixable reason (wrong/missing password, ...) keeps getting
                // retried instead of being silently skipped forever once the cursor moves past it.
                // Not-a-bank-invoice and unsupported-bank attachments are deliberately left off
                // this list too: retrying them changes nothing (no password to fix, no parser to
                // suddenly appear), so they'd just be dead weight on every future fetch.
                val stillFailedUids = mutableSetOf<Long>()
                for (attachment in result.attachments) {
                    try {
                        invoiceRepository.processAndStore(attachment.bytes, enteredPassword = null)
                        imported++
                    } catch (e: DuplicateFileException) {
                        duplicates++
                    } catch (e: DuplicatePeriodException) {
                        duplicates++
                    } catch (e: NotABankInvoiceException) {
                        // Not an error - just some other PDF that happened to be in the inbox.
                    } catch (e: UnsupportedBankException) {
                        unsupportedBanks.add(e.bankName)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to import attachment '${attachment.fileName}'", e)
                        failed++
                        if (e is IncorrectPasswordException) failedPasswords++
                        stillFailedUids.add(attachment.messageUid)
                    }
                }
                if (result.highestUidSeen > lastProcessedUid) {
                    credentialsRepository.setLastProcessedUid(result.highestUidSeen)
                }
                credentialsRepository.setFailedUids(stillFailedUids)
                _state.value = EmailFetchState.Done(
                    EmailImportResult(imported, duplicates, failed, failedPasswords, unsupportedBanks)
                )
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
