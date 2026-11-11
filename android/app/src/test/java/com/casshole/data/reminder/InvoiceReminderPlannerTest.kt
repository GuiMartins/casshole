package com.casshole.data.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class InvoiceReminderPlannerTest {

    private val zone: ZoneId = ZoneOffset.ofHours(-3)
    private val nubank = ReminderCard("nubank", "")
    private val itau = ReminderCard("itau", "1234")

    private fun at(text: String) = LocalDateTime.parse(text).atZone(zone).toInstant()

    private fun invoice(
        bank: String = "nubank",
        card: String = "",
        month: Int,
        year: Int = 2026,
        email: String? = null,
        imported: String? = null,
    ) = ReminderInvoice(bank, card, month, year, email?.let(::at), imported?.let(::at))

    @Test
    fun arrivalDate_isTheEmailDayEvenWhenImportedLater() {
        val arrival = InvoiceReminderPlanner.arrivalDate(
            invoice(month = 11, email = "2026-11-03T06:10:00", imported = "2026-11-08T12:00:00"),
            zone,
        )
        assertEquals(LocalDate.of(2026, 11, 3), arrival)
    }

    @Test
    fun arrivalDate_usesTheImportDayOnlyForAFreshInvoice() {
        // Reference month = due date's month, so a fresh invoice is filed under the import month
        // or the next one.
        assertEquals(
            LocalDate.of(2026, 11, 26),
            InvoiceReminderPlanner.arrivalDate(invoice(month = 12, imported = "2026-11-26T10:00:00"), zone),
        )
        assertEquals(
            LocalDate.of(2026, 11, 5),
            InvoiceReminderPlanner.arrivalDate(invoice(month = 11, imported = "2026-11-05T10:00:00"), zone),
        )
        // Catching up on an old invoice says nothing about when this card's invoices arrive.
        assertNull(InvoiceReminderPlanner.arrivalDate(invoice(month = 6, imported = "2026-11-05T10:00:00"), zone))
    }

    @Test
    fun arrivalDate_freshRuleCrossesTheYear() {
        assertEquals(
            LocalDate.of(2026, 12, 28),
            InvoiceReminderPlanner.arrivalDate(invoice(month = 1, year = 2027, imported = "2026-12-28T10:00:00"), zone),
        )
    }

    @Test
    fun lastArrivalByCard_keepsTheMostRecentArrivalOfEachCard() {
        val arrivals = InvoiceReminderPlanner.lastArrivalByCard(
            listOf(
                invoice(month = 10, email = "2026-10-03T06:00:00"),
                invoice(month = 11, email = "2026-11-04T06:00:00"),
                invoice(bank = "itau", card = "1234", month = 11, email = "2026-11-01T08:00:00"),
                invoice(bank = "itau", card = "9999", month = 3, imported = "2026-11-01T08:00:00"),
            ),
            zone,
        )
        assertEquals(mapOf(nubank to LocalDate.of(2026, 11, 4), itau to LocalDate.of(2026, 11, 1)), arrivals)
    }

    @Test
    fun reminderTimes_areOffsetFromTheArrivalNotChained() {
        assertEquals(
            listOf(LocalDateTime.parse("2027-02-28T19:00"), LocalDateTime.parse("2027-03-31T19:00")),
            InvoiceReminderPlanner.reminderTimes(LocalDate.of(2027, 1, 31)),
        )
    }

    @Test
    fun nextReminderAt_isOneMonthAfterTheArrivalAndPicksTheEarliestCard() {
        val arrivals = mapOf(nubank to LocalDate.of(2026, 11, 4), itau to LocalDate.of(2026, 11, 1))
        val next = InvoiceReminderPlanner.nextReminderAt(
            arrivals,
            now = LocalDateTime.parse("2026-11-11T10:00"),
            lastRun = LocalDateTime.parse("2026-11-11T09:00"),
        )
        assertEquals(LocalDateTime.parse("2026-12-01T19:00"), next)
    }

    @Test
    fun nextReminderAt_neverTheSameDayTheInvoiceArrived() {
        val next = InvoiceReminderPlanner.nextReminderAt(
            mapOf(nubank to LocalDate.of(2026, 11, 11)),
            now = LocalDateTime.parse("2026-11-11T08:00"),
            lastRun = LocalDateTime.parse("2026-11-11T07:00"),
        )
        assertEquals(LocalDateTime.parse("2026-12-11T19:00"), next)
    }

    @Test
    fun nextReminderAt_overdueButUnsentReminderIsReturnedForImmediateDelivery() {
        // Phone was off at 19:00 and came back at 23:00: still within the grace window.
        val next = InvoiceReminderPlanner.nextReminderAt(
            mapOf(nubank to LocalDate.of(2026, 11, 4)),
            now = LocalDateTime.parse("2026-12-04T23:00"),
            lastRun = LocalDateTime.parse("2026-12-01T10:00"),
        )
        assertEquals(LocalDateTime.parse("2026-12-04T19:00"), next)
    }

    @Test
    fun nextReminderAt_sentReminderIsNotRepeated() {
        val next = InvoiceReminderPlanner.nextReminderAt(
            mapOf(nubank to LocalDate.of(2026, 11, 4)),
            now = LocalDateTime.parse("2026-12-04T19:30"),
            lastRun = LocalDateTime.parse("2026-12-04T19:00:02"),
        )
        assertEquals(LocalDateTime.parse("2027-01-04T19:00"), next)
    }

    @Test
    fun nextReminderAt_tooLateReminderIsSkippedNotSentNextMorning() {
        val next = InvoiceReminderPlanner.nextReminderAt(
            mapOf(nubank to LocalDate.of(2026, 11, 4)),
            now = LocalDateTime.parse("2026-12-05T09:00"),
            lastRun = LocalDateTime.parse("2026-12-01T10:00"),
        )
        assertEquals(LocalDateTime.parse("2027-01-04T19:00"), next)
    }

    @Test
    fun nextReminderAt_cardWithoutNewInvoicesGoesQuiet() {
        val next = InvoiceReminderPlanner.nextReminderAt(
            mapOf(nubank to LocalDate.of(2026, 8, 4)),
            now = LocalDateTime.parse("2026-11-11T10:00"),
            lastRun = LocalDateTime.parse("2026-11-11T09:00"),
        )
        assertNull(next)
    }

    @Test
    fun cardsDue_onlyCardsWhoseReminderTimeHasCome() {
        val arrivals = mapOf(nubank to LocalDate.of(2026, 11, 4), itau to LocalDate.of(2026, 11, 20))
        val due = InvoiceReminderPlanner.cardsDue(
            arrivals,
            now = LocalDateTime.parse("2026-12-04T19:00:01"),
            lastRun = LocalDateTime.parse("2026-12-01T10:00"),
        )
        assertEquals(listOf(nubank), due)
    }

    @Test
    fun cardsDue_emptyWhenAlreadySent() {
        val due = InvoiceReminderPlanner.cardsDue(
            mapOf(nubank to LocalDate.of(2026, 11, 4)),
            now = LocalDateTime.parse("2026-12-04T20:00"),
            lastRun = LocalDateTime.parse("2026-12-04T19:00:01"),
        )
        assertTrue(due.isEmpty())
    }
}
