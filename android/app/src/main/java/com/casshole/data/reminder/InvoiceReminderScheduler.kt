package com.casshole.data.reminder

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.casshole.data.PreferencesRepository
import com.casshole.data.local.DatabaseProvider
import com.casshole.data.local.entity.InvoiceEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Keeps exactly one WorkManager job pending: the next monthly reminder across all cards
 * (WorkManager, not AlarmManager - a reminder a few minutes late is fine, and it survives reboots
 * without exact-alarm permissions). The job sends whatever is due and schedules the following one.
 */
object InvoiceReminderScheduler {

    private const val TAG = "InvoiceReminder"
    private const val WORK_NAME = "invoice-reminder"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Watches the invoices table and the on/off setting for the whole process lifetime, so any
     * import, delete or toggle reschedules - no call needed from each screen that changes them. */
    fun startOnAppStart(context: Context) {
        val appContext = context.applicationContext
        InvoiceReminderNotifier.createChannel(appContext)
        val preferences = PreferencesRepository(appContext)
        val invoiceDao = DatabaseProvider.getDatabase(appContext).invoiceDao()
        scope.launch {
            try {
                combine(
                    preferences.invoiceReminderEnabled.distinctUntilChanged(),
                    invoiceDao.observeAll(),
                ) { enabled, invoices -> enabled to invoices }
                    .distinctUntilChanged()
                    .collectIndexed { index, (enabled, invoices) ->
                        // The first value only means "the process started" - possibly started by
                        // WorkManager to run the reminder job itself, which REPLACE would cancel
                        // mid-run. Nothing changed yet, so whatever is pending stays (KEEP).
                        val policy = if (index == 0) ExistingWorkPolicy.KEEP else ExistingWorkPolicy.REPLACE
                        schedule(appContext, enabled, invoices, policy)
                    }
            } catch (e: Exception) {
                Log.w(TAG, "Invoice reminder scheduling stopped", e)
            }
        }
    }

    suspend fun schedule(
        context: Context,
        enabled: Boolean,
        invoices: List<InvoiceEntity>,
        policy: ExistingWorkPolicy,
    ) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val preferences = PreferencesRepository(context)
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        // First run ever: start counting from now, so installing the update doesn't fire a
        // reminder for a day that already went by.
        val lastRun = lastRun(preferences, zone)
            ?: now.toLocalDateTime().also { preferences.setInvoiceReminderLastRun(now.toInstant().toEpochMilli()) }

        val next = InvoiceReminderPlanner.nextReminderAt(lastArrivals(invoices, zone), now.toLocalDateTime(), lastRun)
        if (next == null) {
            // From the job itself (APPEND_OR_REPLACE) there's simply nothing after it.
            if (policy == ExistingWorkPolicy.REPLACE) workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val delay = Duration.between(now, next.atZone(zone)).coerceAtLeast(Duration.ZERO)
        val request = OneTimeWorkRequestBuilder<InvoiceReminderWorker>().setInitialDelay(delay).build()
        workManager.enqueueUniqueWork(WORK_NAME, policy, request)
        Log.i(TAG, "Next invoice reminder check at $next ($policy)")
    }

    internal suspend fun lastRun(preferences: PreferencesRepository, zone: ZoneId): LocalDateTime? =
        preferences.invoiceReminderLastRun.first()?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDateTime() }

    internal fun lastArrivals(invoices: List<InvoiceEntity>, zone: ZoneId): Map<ReminderCard, LocalDate> =
        InvoiceReminderPlanner.lastArrivalByCard(
            invoices.map { invoice ->
                ReminderInvoice(
                    bank = invoice.bank,
                    card = invoice.card,
                    referenceMonth = invoice.referenceMonth,
                    referenceYear = invoice.referenceYear,
                    emailReceivedAt = invoice.emailReceivedAt?.let(Instant::ofEpochMilli),
                    processedAt = runCatching { Instant.parse(invoice.processedAt) }.getOrNull(),
                )
            },
            zone,
        )
}
