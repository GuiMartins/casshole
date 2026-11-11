package com.casshole.data.local

import com.casshole.data.local.entity.InvoiceEntity
import com.casshole.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CardholderBreakdownTest {

    private fun transaction(cardholder: String, amount: Double) =
        TransactionEntity(invoiceId = 1, date = "2026-07-01", description = "X", amount = amount, cardholder = cardholder)

    private fun invoice(id: Long, transactions: List<TransactionEntity>) = InvoiceWithTransactions(
        invoice = InvoiceEntity(
            id = id,
            bank = "nubank",
            referenceMonth = 7,
            referenceYear = 2026,
            fileHash = "hash-$id",
            processedAt = "2026-07-25T00:00:00",
        ),
        transactions = transactions,
    )

    @Test
    fun summarize_groupsTheSamePersonSpelledDifferentlyAcrossBanks() {
        val summaries = CardholderBreakdown.summarize(
            listOf(
                transaction("GUILHERME M MARTINS", 100.0),
                transaction("Guilherme  M Martins", 50.0),
                transaction("Maria S Martins", 30.0),
            )
        )
        assertEquals(2, summaries.size)
        assertEquals("GUILHERME M MARTINS", summaries[0].key)
        assertEquals(150.0, summaries[0].total, 0.001)
        assertEquals(2, summaries[0].count)
        assertEquals(83.33, summaries[0].share, 0.001)
        assertEquals("Maria S Martins", summaries[1].name)
    }

    @Test
    fun summarize_keepsUnidentifiedTransactionsAsTheirOwnGroupAtTheEnd() {
        val summaries = CardholderBreakdown.summarize(
            listOf(transaction("", 500.0), transaction("Ana", 10.0))
        )
        assertEquals(listOf("ANA", ""), summaries.map { it.key })
        assertEquals(500.0, summaries.last().total, 0.001)
    }

    @Test
    fun filterInvoices_keepsOnlyThatCardholderAndDropsEmptyInvoices() {
        val invoices = listOf(
            invoice(1, listOf(transaction("Ana", 10.0), transaction("Bia", 20.0))),
            invoice(2, listOf(transaction("Bia", 5.0))),
        )
        val filtered = CardholderBreakdown.filterInvoices(invoices, "ANA")
        assertEquals(1, filtered.size)
        assertEquals(1L, filtered[0].id)
        assertEquals(listOf(10.0), filtered[0].transactions.map { it.amount })
    }

    @Test
    fun nullKeyMeansNoFilter() {
        val invoices = listOf(invoice(1, listOf(transaction("Ana", 10.0), transaction("Bia", 20.0))))
        assertEquals(invoices, CardholderBreakdown.filterInvoices(invoices, null))
    }
}
