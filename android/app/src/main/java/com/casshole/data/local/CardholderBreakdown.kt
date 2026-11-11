package com.casshole.data.local

import com.casshole.data.local.entity.TransactionEntity

/**
 * Spending of one cardholder (the account holder or an additional card).
 * [key] is the normalized name used for grouping/filtering; [name] is the
 * first spelling seen, for display. Both are blank for transactions whose
 * invoice didn't say who made them - those stay visible as their own group
 * instead of silently disappearing from the breakdown.
 */
data class CardholderSummary(
    val key: String,
    val name: String,
    val total: Double,
    val count: Int,
    /** Share of the whole set, in percent. */
    val share: Double,
)

object CardholderBreakdown {

    private val WHITESPACE = Regex("\\s+")

    /**
     * Banks spell the same person differently ("GUILHERME M MARTINS" on Itaú,
     * "Guilherme M Martins" on Nubank), so grouping goes by a case- and
     * whitespace-insensitive key instead of the raw string.
     */
    fun keyOf(cardholder: String): String = cardholder.trim().replace(WHITESPACE, " ").uppercase()

    /** Heaviest cardholder first; the "unidentified" group (blank key) always goes last. */
    fun summarize(transactions: List<TransactionEntity>): List<CardholderSummary> {
        val grandTotal = transactions.sumOf { it.amount }
        return transactions
            .groupBy { keyOf(it.cardholder) }
            .map { (key, items) ->
                val total = items.sumOf { it.amount }
                CardholderSummary(
                    key = key,
                    name = items.first().cardholder.trim().replace(WHITESPACE, " "),
                    total = round(total),
                    count = items.size,
                    share = if (grandTotal > 0) round(total / grandTotal * 100) else 0.0,
                )
            }
            .sortedWith(compareBy<CardholderSummary> { it.key.isEmpty() }.thenByDescending { it.total })
    }

    fun filterTransactions(transactions: List<TransactionEntity>, key: String?): List<TransactionEntity> =
        if (key == null) transactions else transactions.filter { keyOf(it.cardholder) == key }

    /**
     * Same invoices, keeping only this cardholder's transactions. Invoices left
     * empty are dropped so they don't count as "a month with data".
     */
    fun filterInvoices(invoices: List<InvoiceWithTransactions>, key: String?): List<InvoiceWithTransactions> =
        if (key == null) {
            invoices
        } else {
            invoices
                .map { it.copy(transactions = filterTransactions(it.transactions, key)) }
                .filter { it.transactions.isNotEmpty() }
        }

    private fun round(value: Double): Double = Math.round(value * 100) / 100.0
}
