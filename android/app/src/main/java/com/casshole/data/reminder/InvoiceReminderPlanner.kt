package com.casshole.data.reminder

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

/** A stored invoice, reduced to what the reminder needs. */
data class ReminderInvoice(
    val bank: String,
    val card: String,
    val referenceMonth: Int,
    val referenceYear: Int,
    val emailReceivedAt: Instant?,
    val processedAt: Instant?,
)

data class ReminderCard(val bank: String, val card: String)

/**
 * When to send the monthly "time to check your spending" notification, one per card.
 *
 * A card's invoice tends to land on the same day every month (it follows the closing date,
 * which is fixed per card), so the reminder is the day the last one arrived, one month later.
 * The closing date itself isn't used: it's not printed the same way by every bank, and what
 * matters to the user is when the invoice is actually there to look at.
 *
 * Pure java.time, no Android - the scheduler and the worker feed it and act on the answer.
 */
object InvoiceReminderPlanner {

    val REMINDER_TIME: LocalTime = LocalTime.of(19, 0)

    /** Reminders keep coming for a card only this many months past its last invoice: a card
     * that stopped sending invoices (cancelled, or the user stopped importing) goes quiet. */
    const val MAX_REMINDERS_WITHOUT_NEW_INVOICE = 2

    /** A reminder that couldn't go out on time (phone off, Doze) still goes out this late. */
    val LATE_DELIVERY_GRACE: Duration = Duration.ofHours(12)

    /**
     * The day this invoice reached the user. For an invoice imported by hand there's no e-mail
     * date, so the import day stands in - but only when it was a fresh invoice (reference month
     * is the import month or the next one: invoices are filed under the due date's month and
     * arrive a week or so before it). Importing an old invoice says nothing about when the card's
     * invoices arrive.
     */
    fun arrivalDate(invoice: ReminderInvoice, zone: ZoneId): LocalDate? {
        invoice.emailReceivedAt?.let { return it.atZone(zone).toLocalDate() }
        val imported = invoice.processedAt?.atZone(zone)?.toLocalDate() ?: return null
        val reference = YearMonth.of(invoice.referenceYear, invoice.referenceMonth)
        val importMonth = YearMonth.from(imported)
        return imported.takeIf { reference == importMonth || reference == importMonth.plusMonths(1) }
    }

    fun lastArrivalByCard(invoices: List<ReminderInvoice>, zone: ZoneId): Map<ReminderCard, LocalDate> =
        invoices
            .mapNotNull { invoice ->
                arrivalDate(invoice, zone)?.let { ReminderCard(invoice.bank, invoice.card) to it }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, dates) -> dates.max() }

    /** Always offset from the arrival itself, not chained month to month: an invoice that
     * arrived on Jan 31 reminds on Feb 28 and then Mar 31, not Mar 28. */
    fun reminderTimes(lastArrival: LocalDate): List<LocalDateTime> =
        (1..MAX_REMINDERS_WITHOUT_NEW_INVOICE).map { months ->
            lastArrival.plusMonths(months.toLong()).atTime(REMINDER_TIME)
        }

    /** The earliest reminder still to be sent across all cards; at or before [now] means it's
     * overdue and should go out right away. Null when there's nothing left to remind about. */
    fun nextReminderAt(
        lastArrivals: Map<ReminderCard, LocalDate>,
        now: LocalDateTime,
        lastRun: LocalDateTime?,
    ): LocalDateTime? =
        lastArrivals.values.mapNotNull { pendingTimes(it, now, lastRun).firstOrNull() }.minOrNull()

    /** Cards with a reminder due at [now] that hasn't been sent yet. */
    fun cardsDue(
        lastArrivals: Map<ReminderCard, LocalDate>,
        now: LocalDateTime,
        lastRun: LocalDateTime?,
    ): List<ReminderCard> =
        lastArrivals.filterValues { arrival -> pendingTimes(arrival, now, lastRun).any { !it.isAfter(now) } }
            .keys
            .sortedWith(compareBy({ it.bank }, { it.card }))

    private fun pendingTimes(lastArrival: LocalDate, now: LocalDateTime, lastRun: LocalDateTime?): List<LocalDateTime> {
        val tooOld = now.minus(LATE_DELIVERY_GRACE)
        val notBefore = if (lastRun != null && lastRun.isAfter(tooOld)) lastRun else tooOld
        return reminderTimes(lastArrival).filter { it.isAfter(notBefore) }
    }
}
