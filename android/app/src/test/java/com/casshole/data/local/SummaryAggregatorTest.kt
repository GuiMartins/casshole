package com.casshole.data.local

import com.casshole.data.local.entity.InvoiceEntity
import com.casshole.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryAggregatorTest {

    private fun transaction(category: String, amount: Double) =
        TransactionEntity(invoiceId = 1, date = "2026-07-01", description = "X", amount = amount, category = category)

    private fun invoice(id: Long, month: Int, year: Int) = InvoiceWithTransactions(
        invoice = InvoiceEntity(
            id = id,
            bank = "itau",
            referenceMonth = month,
            referenceYear = year,
            fileHash = "hash-$id",
            processedAt = "2026-07-25T00:00:00",
        ),
        transactions = emptyList(),
    )

    @Test
    fun `filterByMonth keeps only invoices matching both month and year`() {
        val invoices = listOf(
            invoice(1, month = 7, year = 2026),
            invoice(2, month = 5, year = 2026),
            invoice(3, month = 7, year = 2025),
        )

        val result = SummaryAggregator.filterByMonth(invoices, month = 7, year = 2026)

        assertEquals(listOf(1L), result.map { it.id })
    }

    @Test
    fun `filterByMonth returns empty list when nothing matches`() {
        val invoices = listOf(invoice(1, month = 5, year = 2026))

        assertTrue(SummaryAggregator.filterByMonth(invoices, month = 7, year = 2026).isEmpty())
    }

    @Test
    fun `empty transaction list yields no summary`() {
        assertNull(SummaryAggregator.buildMonthlySummary(emptyList(), 7, 2026))
    }

    @Test
    fun `groups by category, sums and sorts descending`() {
        val summary = SummaryAggregator.buildMonthlySummary(
            listOf(
                transaction("Compras", 100.0),
                transaction("Alimentação", 250.0),
                transaction("Compras", 50.0),
            ),
            month = 7,
            year = 2026,
        )!!

        assertEquals(400.0, summary.totalSpent, 0.0)
        assertEquals(listOf("Alimentação", "Compras"), summary.byCategory.map { it.category })
        assertEquals(250.0, summary.byCategory[0].total, 0.0)
        assertEquals(150.0, summary.byCategory[1].total, 0.0)
    }

    @Test
    fun `rounds totals to 2 decimal places`() {
        val summary = SummaryAggregator.buildMonthlySummary(
            listOf(transaction("Outros", 10.005), transaction("Outros", 0.001)),
            month = 7,
            year = 2026,
        )!!

        assertEquals(10.01, summary.totalSpent, 0.0)
    }

    @Test
    fun `comparing fewer than 2 months has no percentage change`() {
        val comparison = SummaryAggregator.compareMonths(
            listOf(Triple(7, 2026, listOf(transaction("Compras", 100.0))))
        )

        assertNull(comparison.totalPercentageChange)
    }

    @Test
    fun `percentage change is computed between the first and last period`() {
        val comparison = SummaryAggregator.compareMonths(
            listOf(
                Triple(6, 2026, listOf(transaction("Compras", 100.0))),
                Triple(7, 2026, listOf(transaction("Compras", 150.0))),
            )
        )

        assertEquals(2, comparison.months.size)
        assertEquals(50.0, comparison.totalPercentageChange!!, 0.0)
    }

    @Test
    fun `category comparison lines every category up across the compared periods`() {
        val comparison = SummaryAggregator.compareMonths(
            listOf(
                Triple(6, 2026, listOf(transaction("Compras", 100.0), transaction("Alimentação", 80.0))),
                Triple(7, 2026, listOf(transaction("Compras", 150.0))),
            )
        )

        val compras = comparison.byCategory.first { it.category == "Compras" }
        assertEquals(listOf(100.0, 150.0), compras.totalsByPeriod)
        assertEquals(250.0, compras.total, 0.0)
        assertEquals(125.0, compras.average, 0.0)
        assertEquals(50.0, compras.change, 0.0)
        assertEquals(50.0, compras.percentageChange!!, 0.0)

        // Absent from the last period - a drop to zero, not a missing data point.
        val alimentacao = comparison.byCategory.first { it.category == "Alimentação" }
        assertEquals(listOf(80.0, 0.0), alimentacao.totalsByPeriod)
        assertEquals(-100.0, alimentacao.percentageChange!!, 0.0)
    }

    @Test
    fun `category comparison has no percentage when the category is new in the last period`() {
        val comparison = SummaryAggregator.compareMonths(
            listOf(
                Triple(6, 2026, listOf(transaction("Compras", 100.0))),
                Triple(7, 2026, listOf(transaction("Compras", 100.0), transaction("Pets", 60.0))),
            )
        )

        val pets = comparison.byCategory.first { it.category == "Pets" }
        assertNull(pets.percentageChange)
        assertEquals(60.0, pets.change, 0.0)
    }

    @Test
    fun `category comparison carries the share of the range and the peak period`() {
        val comparison = SummaryAggregator.compareMonths(
            listOf(
                Triple(5, 2026, listOf(transaction("Compras", 100.0))),
                Triple(6, 2026, listOf(transaction("Compras", 300.0))),
                Triple(7, 2026, listOf(transaction("Compras", 100.0), transaction("Pets", 100.0))),
            )
        )

        val compras = comparison.byCategory.first { it.category == "Compras" }
        // 500 de 600 gastos no intervalo inteiro.
        assertEquals(83.33, compras.share, 0.0)
        assertEquals(1, compras.peakPeriodIndex)

        val pets = comparison.byCategory.first { it.category == "Pets" }
        assertEquals(16.67, pets.share, 0.0)
        assertEquals(2, pets.peakPeriodIndex)
    }

    @Test
    fun `category comparison is sorted by weight in the whole range`() {
        val comparison = SummaryAggregator.compareMonths(
            listOf(
                Triple(6, 2026, listOf(transaction("Compras", 10.0), transaction("Saúde", 200.0))),
                Triple(7, 2026, listOf(transaction("Compras", 20.0), transaction("Saúde", 5.0))),
            )
        )

        assertEquals(listOf("Saúde", "Compras"), comparison.byCategory.map { it.category })
    }

    @Test
    fun `range stats cover total, average and the extreme months`() {
        val comparison = SummaryAggregator.compareMonths(
            listOf(
                Triple(5, 2026, listOf(transaction("Compras", 100.0))),
                Triple(6, 2026, listOf(transaction("Compras", 300.0))),
                Triple(7, 2026, listOf(transaction("Compras", 200.0))),
            )
        )

        assertEquals(600.0, comparison.total, 0.0)
        assertEquals(200.0, comparison.average, 0.0)
        assertEquals(6, comparison.highest!!.referenceMonth)
        assertEquals(5, comparison.lowest!!.referenceMonth)
    }

    @Test
    fun `percentage change is null when the first period total is zero`() {
        val comparison = SummaryAggregator.compareMonths(
            listOf(
                Triple(6, 2026, emptyList()),
                Triple(7, 2026, listOf(transaction("Compras", 150.0))),
            )
        )

        assertTrue(comparison.months.size == 1)
        assertNull(comparison.totalPercentageChange)
    }
}
