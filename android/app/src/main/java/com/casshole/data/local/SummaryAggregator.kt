package com.casshole.data.local

import com.casshole.data.local.entity.TransactionEntity

data class CategorySummary(val category: String, val total: Double)

data class MonthlySummary(
    val referenceMonth: Int,
    val referenceYear: Int,
    val totalSpent: Double,
    val byCategory: List<CategorySummary>,
)

/**
 * One category tracked across every compared period. [totalsByPeriod] is
 * aligned index-by-index with [MonthlyComparison.months] and carries 0.0 for
 * periods where the category didn't show up at all, so the UI can render a
 * row per category without re-scanning the summaries.
 */
data class CategoryComparison(
    val category: String,
    val totalsByPeriod: List<Double>,
    val total: Double,
    val average: Double,
    val change: Double,
    val percentageChange: Double?,
    /** How much of everything spent in the range went to this category. */
    val share: Double,
    /** Index into [totalsByPeriod] of the heaviest period for this category. */
    val peakPeriodIndex: Int,
)

data class MonthlyComparison(
    val months: List<MonthlySummary>,
    val totalPercentageChange: Double?,
    val byCategory: List<CategoryComparison> = emptyList(),
    val total: Double = 0.0,
    val average: Double = 0.0,
    val highest: MonthlySummary? = null,
    val lowest: MonthlySummary? = null,
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

        return MonthlyComparison(
            months = summaries,
            totalPercentageChange = change,
            byCategory = compareCategories(summaries),
            total = round(summaries.sumOf { it.totalSpent }),
            average = if (summaries.isEmpty()) 0.0 else round(summaries.sumOf { it.totalSpent } / summaries.size),
            highest = summaries.maxByOrNull { it.totalSpent },
            lowest = summaries.minByOrNull { it.totalSpent },
        )
    }

    /**
     * Cross-period view of the same data: one row per category, ordered by how
     * much it weighs on the whole comparison. A category missing from a period
     * counts as 0.0 there - that's a real drop, not a gap.
     */
    fun compareCategories(summaries: List<MonthlySummary>): List<CategoryComparison> {
        if (summaries.isEmpty()) return emptyList()

        val categories = summaries.flatMap { it.byCategory.map { category -> category.category } }.distinct()
        val rangeTotal = summaries.sumOf { it.totalSpent }

        return categories.map { category ->
            val totalsByPeriod = summaries.map { summary ->
                summary.byCategory.find { it.category == category }?.total ?: 0.0
            }
            val first = totalsByPeriod.first()
            val last = totalsByPeriod.last()
            val categoryTotal = totalsByPeriod.sum()
            CategoryComparison(
                category = category,
                totalsByPeriod = totalsByPeriod,
                total = round(categoryTotal),
                average = round(categoryTotal / totalsByPeriod.size),
                change = round(last - first),
                // Undefined when the category simply didn't exist in the first
                // period: "+infinity%" says less than the absolute change does.
                percentageChange = if (summaries.size >= 2 && first > 0) round((last - first) / first * 100) else null,
                share = if (rangeTotal > 0) round(categoryTotal / rangeTotal * 100) else 0.0,
                peakPeriodIndex = totalsByPeriod.indexOf(totalsByPeriod.max()),
            )
        }.sortedByDescending { it.total }
    }

    private fun round(value: Double): Double = Math.round(value * 100) / 100.0
}
