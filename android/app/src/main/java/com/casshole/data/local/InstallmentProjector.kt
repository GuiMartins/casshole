package com.casshole.data.local

/** One future installment of a purchase that is already running, not a charge read from an invoice. */
data class ProjectedInstallment(
    val description: String,
    val category: String,
    val amount: Double,
    val installmentNumber: Int,
    val totalInstallments: Int,
    val bank: String,
    val card: String,
)

data class ProjectedMonth(
    val referenceMonth: Int,
    val referenceYear: Int,
    val total: Double,
    val installments: List<ProjectedInstallment>,
)

/**
 * Estimates what future invoices already owe, from the installments still
 * running in the invoices already imported ("Parcela 3/10" means 7 more
 * charges of the same amount are coming).
 *
 * This is a projection, never a parsed invoice - every screen showing it has
 * to label it as such.
 */
object InstallmentProjector {

    const val DEFAULT_MONTHS_AHEAD = 12

    /**
     * Only the newest invoice of each bank+card feeds the projection: older
     * invoices carry earlier installments of the very same purchase, so
     * projecting from all of them would count it several times over.
     *
     * Months already covered by an imported invoice are skipped as well - the
     * real charge is known there, and mixing it with an estimate would double
     * count it in the charts.
     */
    fun project(
        invoices: List<InvoiceWithTransactions>,
        monthsAhead: Int = DEFAULT_MONTHS_AHEAD,
    ): List<ProjectedMonth> {
        if (invoices.isEmpty() || monthsAhead <= 0) return emptyList()

        val latestPerCard = invoices
            .groupBy { it.bank to it.card }
            .mapNotNull { (_, cardInvoices) -> cardInvoices.maxByOrNull { periodIndex(it.referenceMonth, it.referenceYear) } }
        val lastImportedPeriod = invoices.maxOf { periodIndex(it.referenceMonth, it.referenceYear) }

        val byPeriod = mutableMapOf<Int, MutableList<ProjectedInstallment>>()
        for (invoice in latestPerCard) {
            val base = periodIndex(invoice.referenceMonth, invoice.referenceYear)
            for (transaction in invoice.transactions) {
                val current = transaction.currentInstallment ?: continue
                val total = transaction.totalInstallments ?: continue
                if (total <= current) continue

                for (installment in (current + 1)..total) {
                    val period = base + (installment - current)
                    if (period <= lastImportedPeriod) continue
                    if (period - lastImportedPeriod > monthsAhead) break

                    byPeriod.getOrPut(period) { mutableListOf() }.add(
                        ProjectedInstallment(
                            description = transaction.description,
                            category = transaction.category,
                            amount = transaction.amount,
                            installmentNumber = installment,
                            totalInstallments = total,
                            bank = invoice.bank,
                            card = invoice.card,
                        )
                    )
                }
            }
        }

        return byPeriod.toSortedMap().map { (period, installments) ->
            ProjectedMonth(
                referenceMonth = period % 12 + 1,
                referenceYear = period / 12,
                total = round(installments.sumOf { it.amount }),
                installments = installments.sortedByDescending { it.amount },
            )
        }
    }

    private fun periodIndex(month: Int, year: Int): Int = year * 12 + (month - 1)

    private fun round(value: Double): Double = Math.round(value * 100) / 100.0
}
