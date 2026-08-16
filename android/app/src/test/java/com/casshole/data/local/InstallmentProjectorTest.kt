package com.casshole.data.local

import com.casshole.data.local.entity.InvoiceEntity
import com.casshole.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallmentProjectorTest {

    private fun transaction(
        description: String,
        amount: Double,
        currentInstallment: Int? = null,
        totalInstallments: Int? = null,
    ) = TransactionEntity(
        invoiceId = 1,
        date = "2026-07-01",
        description = description,
        amount = amount,
        category = "Compras",
        currentInstallment = currentInstallment,
        totalInstallments = totalInstallments,
    )

    private fun invoice(
        id: Long,
        month: Int,
        year: Int,
        card: String = "1234",
        bank: String = "nubank",
        transactions: List<TransactionEntity> = emptyList(),
    ) = InvoiceWithTransactions(
        invoice = InvoiceEntity(
            id = id,
            bank = bank,
            card = card,
            referenceMonth = month,
            referenceYear = year,
            fileHash = "hash-$id",
            processedAt = "2026-07-25T00:00:00",
        ),
        transactions = transactions,
    )

    @Test
    fun `projects one month per remaining installment`() {
        val projection = InstallmentProjector.project(
            listOf(invoice(1, month = 7, year = 2026, transactions = listOf(transaction("Geladeira", 200.0, 8, 10))))
        )

        assertEquals(2, projection.size)
        assertEquals(8 to 2026, projection[0].referenceMonth to projection[0].referenceYear)
        assertEquals(200.0, projection[0].total, 0.0)
        assertEquals(9, projection[0].installments.first().installmentNumber)
        assertEquals(9 to 2026, projection[1].referenceMonth to projection[1].referenceYear)
        assertEquals(10, projection[1].installments.first().installmentNumber)
    }

    @Test
    fun `rolls the year over at december`() {
        val projection = InstallmentProjector.project(
            listOf(invoice(1, month = 12, year = 2026, transactions = listOf(transaction("Notebook", 500.0, 1, 2))))
        )

        assertEquals(1, projection.size)
        assertEquals(1 to 2027, projection[0].referenceMonth to projection[0].referenceYear)
    }

    @Test
    fun `ignores single charges and finished installments`() {
        val projection = InstallmentProjector.project(
            listOf(
                invoice(
                    1, month = 7, year = 2026,
                    transactions = listOf(
                        transaction("Padaria", 25.0),
                        transaction("Fone", 100.0, 3, 3),
                    ),
                )
            )
        )

        assertTrue(projection.isEmpty())
    }

    @Test
    fun `only the newest invoice of a card feeds the projection`() {
        // The same purchase shows up in both invoices, one installment apart -
        // projecting from both would count it twice.
        val projection = InstallmentProjector.project(
            listOf(
                invoice(1, month = 6, year = 2026, transactions = listOf(transaction("Sofá", 300.0, 1, 3))),
                invoice(2, month = 7, year = 2026, transactions = listOf(transaction("Sofá", 300.0, 2, 3))),
            )
        )

        assertEquals(1, projection.size)
        assertEquals(8 to 2026, projection[0].referenceMonth to projection[0].referenceYear)
        assertEquals(300.0, projection[0].total, 0.0)
    }

    @Test
    fun `installments of different cards land on the same projected month`() {
        val projection = InstallmentProjector.project(
            listOf(
                invoice(1, month = 7, year = 2026, card = "1234", transactions = listOf(transaction("Sofá", 300.0, 1, 3))),
                invoice(2, month = 7, year = 2026, bank = "itau", card = "9876", transactions = listOf(transaction("TV", 150.0, 2, 4))),
            )
        )

        assertEquals(450.0, projection.first().total, 0.0)
        assertEquals(2, projection.first().installments.size)
    }

    @Test
    fun `months already covered by an imported invoice are not projected`() {
        // The newest Itaú invoice is older than the newest Nubank one: its
        // remaining installments for 07/2026 are already known, not estimated.
        val projection = InstallmentProjector.project(
            listOf(
                invoice(1, month = 7, year = 2026, card = "1234", transactions = listOf(transaction("Compra", 100.0, 5, 5))),
                invoice(2, month = 6, year = 2026, bank = "itau", card = "9876", transactions = listOf(transaction("TV", 150.0, 1, 3))),
            )
        )

        assertEquals(1, projection.size)
        assertEquals(8 to 2026, projection[0].referenceMonth to projection[0].referenceYear)
    }

    @Test
    fun `does not project further ahead than the requested window`() {
        val projection = InstallmentProjector.project(
            listOf(invoice(1, month = 7, year = 2026, transactions = listOf(transaction("Móveis", 90.0, 1, 24)))),
            monthsAhead = 3,
        )

        assertEquals(3, projection.size)
    }

    @Test
    fun `no invoices yields no projection`() {
        assertTrue(InstallmentProjector.project(emptyList()).isEmpty())
    }
}
