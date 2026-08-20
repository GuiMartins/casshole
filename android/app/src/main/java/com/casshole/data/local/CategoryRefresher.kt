package com.casshole.data.local

import android.content.Context
import android.util.Log
import com.casshole.categorizer.CATEGORIZER_REVISION
import com.casshole.data.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Keeps invoices already in the database in sync with the categorization
 * rules: when [CATEGORIZER_REVISION] moves ahead of what the device has
 * applied, every transaction the user hasn't corrected by hand is run through
 * the rules again, once.
 *
 * Own scope, not tied to any screen - same shape as EmailFetchCoordinator,
 * since this also runs from Application.onCreate() before any UI exists.
 */
object CategoryRefresher {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun refreshOnAppStart(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val preferences = PreferencesRepository(appContext)
                if (preferences.categorizerRevision.first() >= CATEGORIZER_REVISION) return@launch

                val updated = InvoiceRepository(appContext).recategorizeStoredTransactions()
                preferences.setCategorizerRevision(CATEGORIZER_REVISION)
                Log.i("CategoryRefresher", "Recategorized $updated transactions for revision $CATEGORIZER_REVISION")
            } catch (e: Exception) {
                // Never block app start over this: the rules still apply to
                // new invoices, and the next cold start tries again (the
                // revision is only stored after a successful pass).
                Log.w("CategoryRefresher", "Could not recategorize stored transactions", e)
            }
        }
    }
}
