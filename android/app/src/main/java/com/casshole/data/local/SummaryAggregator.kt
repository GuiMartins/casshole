package com.casshole.data.local

import com.casshole.data.local.entity.TransactionEntity

data class CategorySummary(val category: String, val total: Double)

data class MonthlySummary(
    val referenceMonth: Int,
    val referenceYear: Int,
    val totalSpent: Double,
    val byCategory: List<CategorySummary>,
)

data class MonthlyComparison(
    val months: List<MonthlySummary>,
    val totalPercentageChange: Double?,
)

object SummaryAggregator {

    /** Invoices whose reference month/year match exactly - used to keep the Dashboard's summary scoped to one period. */
    fun filterByMonth(
        invoices: List<InvoiceWithTransactions>,
        month: Int,
        year: Int,
    ): List<InvoiceWithTransactions> =
        invoices.filter { it.referenceMonth == month && it.referenceYear == year }

    fun buildMonthlySummary(transactions: List<TransactionEntity>, month: Int, year: Int): MonthlySummary? {
        if (transactions.isEmpty()) return null

        val byCategory = transactions
            .groupBy { it.category }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
            .toList()
            .sortedByDescending { (_, total) -> total }
            .map { (category, total) -> CategorySummary(category, round(total)) }

        return MonthlySummary(
            referenceMonth = month,
            referenceYear = year,
            totalSpent = round(transactions.sumOf { it.amount }),
            byCategory = byCategory,
        )
    }

    fun compareMonths(periods: List<Triple<Int, Int, List<TransactionEntity>>>): MonthlyComparison {
        val summaries = periods.mapNotNull { (month, year, transactions) -> buildMonthlySummary(transactions, month, year) }

        val change = if (summaries.size >= 2 && summaries.first().totalSpent > 0) {
            round(
                (summaries.last().totalSpent - summaries.first().totalSpent) / summaries.first().totalSpent * 100
            )
        } else {
            null
        }

        return MonthlyComparison(months = summaries, totalPercentageChange = change)
    }

    private fun round(value: Double): Double = Math.round(value * 100) / 100.0
}
