package com.moneyhole.data.local

import com.moneyhole.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SummaryAggregatorTest {

    private fun transaction(category: String, amount: Double) =
        TransactionEntity(invoiceId = 1, date = "2026-07-01", description = "X", amount = amount, category = category)

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
