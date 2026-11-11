package com.casshole.data.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import com.casshole.data.PreferencesRepository
import com.casshole.data.local.DatabaseProvider
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.time.ZonedDateTime

/** Sends the reminders due now (one notification per card) and queues the next check. */
class InvoiceReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val preferences = PreferencesRepository(applicationContext)
        if (!preferences.invoiceReminderEnabled.first()) return Result.success()

        val database = DatabaseProvider.getDatabase(applicationContext)
        val invoices = database.invoiceDao().listAll()
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        val lastArrivals = InvoiceReminderScheduler.lastArrivals(invoices, zone)
        val due = InvoiceReminderPlanner.cardsDue(
            lastArrivals,
            now.toLocalDateTime(),
            InvoiceReminderScheduler.lastRun(preferences, zone),
        )
        if (due.isNotEmpty()) {
            val nicknames = database.cardNicknameDao().list().associate { ReminderCard(it.bank, it.card) to it.nickname }
            due.forEach { card -> InvoiceReminderNotifier.notify(applicationContext, card, nicknames[card]) }
        }

        preferences.setInvoiceReminderLastRun(now.toInstant().toEpochMilli())
        // APPEND_OR_REPLACE: queued behind this very job, which REPLACE would cancel.
        InvoiceReminderScheduler.schedule(applicationContext, enabled = true, invoices, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }
}
