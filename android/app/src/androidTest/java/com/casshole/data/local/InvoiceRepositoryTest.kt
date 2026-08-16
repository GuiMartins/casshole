package com.casshole.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.casshole.data.local.entity.InvoiceEntity
import com.casshole.data.local.entity.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class InvoiceRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: InvoiceRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = InvoiceRepository(context, db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private suspend fun insertInvoice(bank: String, month: Int, hash: String): Long =
        db.invoiceDao().insert(
            InvoiceEntity(
                bank = bank,
                referenceMonth = month,
                referenceYear = 2026,
                fileHash = hash,
                processedAt = "2026-07-24T00:00:00",
            )
        )

    private fun transaction(invoiceId: Long, description: String, amount: Double) = TransactionEntity(
        invoiceId = invoiceId,
        date = "2026-06-05",
        description = description,
        amount = amount,
    )

    @Test
    fun reuploadingTheSameFileIsRejectedBeforeParsing(): Unit = runBlocking {
        val bytes = "conteudo-qualquer".toByteArray()
        db.invoiceDao().insert(
            InvoiceEntity(
                bank = "itau",
                referenceMonth = 7,
                referenceYear = 2026,
                fileHash = sha256(bytes),
                processedAt = "2026-07-24T00:00:00",
            )
        )

        assertThrows(DuplicateFileException::class.java) {
            runBlocking { repository.processAndStore(bytes, enteredPassword = null) }
        }
        Unit
    }

    @Test
    fun updateCategory_persistsOnTheTransactionAndRemembersAsOverride() = runBlocking {
        val invoiceId = db.invoiceDao().insert(
            InvoiceEntity(
                bank = "nubank",
                referenceMonth = 7,
                referenceYear = 2026,
                fileHash = "hash-1",
                processedAt = "2026-07-24T00:00:00",
            )
        )
        db.transactionDao().insertAll(
            listOf(
                com.casshole.data.local.entity.TransactionEntity(
                    invoiceId = invoiceId,
                    date = "2026-06-05",
                    description = "  crOc salgaderia  ",
                    amount = 32.5,
                )
            )
        )
        val transactionId = db.invoiceDao().getWithTransactions(invoiceId)!!.transactions.first().id

        repository.updateCategory(transactionId, "Alimentação")

        val stored = db.invoiceDao().getWithTransactions(invoiceId)!!.transactions.first()
        assertEquals("Alimentação", stored.category)

        val overrides = repository.listCategoryOverrides()
        assertEquals(1, overrides.size)
        assertEquals("CROC SALGADERIA", overrides.first().description)
        assertEquals("Alimentação", overrides.first().category)
    }

    @Test
    fun updateCategory_withApplyToPast_recategorizesEveryTransactionWithTheSameDescription() = runBlocking {
        val juneInvoiceId = insertInvoice(bank = "nubank", month = 6, hash = "hash-june")
        val julyInvoiceId = insertInvoice(bank = "nubank", month = 7, hash = "hash-july")
        db.transactionDao().insertAll(
            listOf(
                transaction(juneInvoiceId, "Hospital das Bonecas", 40.0),
                transaction(julyInvoiceId, "hospital das bonecas ", 60.0),
                transaction(julyInvoiceId, "Padaria do Zé", 15.0),
            )
        )
        val julyTransactionId = db.invoiceDao().getWithTransactions(julyInvoiceId)!!
            .transactions.first { it.description.trim().equals("hospital das bonecas", ignoreCase = true) }.id

        repository.updateCategory(julyTransactionId, "Compras", applyToPast = true)

        val allTransactions = db.invoiceDao().listWithTransactions().flatMap { it.transactions }
        assertEquals(
            listOf("Compras", "Compras"),
            allTransactions.filter { it.description.contains("ospital") }.map { it.category },
        )
        // Untouched: a different merchant keeps whatever category it had.
        assertEquals("Outros", allTransactions.first { it.description == "Padaria do Zé" }.category)
        assertEquals("Compras", repository.listCategoryOverrides().single().category)
    }

    @Test
    fun updateCategory_withoutApplyToPast_onlyTouchesTheEditedTransaction() = runBlocking {
        val juneInvoiceId = insertInvoice(bank = "nubank", month = 6, hash = "hash-june-2")
        val julyInvoiceId = insertInvoice(bank = "nubank", month = 7, hash = "hash-july-2")
        db.transactionDao().insertAll(
            listOf(
                transaction(juneInvoiceId, "Amazon Prime", 19.9),
                transaction(julyInvoiceId, "Amazon Prime", 19.9),
            )
        )
        val julyTransactionId = db.invoiceDao().getWithTransactions(julyInvoiceId)!!.transactions.first().id

        repository.updateCategory(julyTransactionId, "Compras", applyToPast = false)

        assertEquals("Compras", db.invoiceDao().getWithTransactions(julyInvoiceId)!!.transactions.first().category)
        assertEquals("Outros", db.invoiceDao().getWithTransactions(juneInvoiceId)!!.transactions.first().category)
    }

    @Test
    fun countTransactionsWithDescription_ignoresCaseAndSurroundingSpaces() = runBlocking {
        val invoiceId = insertInvoice(bank = "itau", month = 7, hash = "hash-count")
        db.transactionDao().insertAll(
            listOf(
                transaction(invoiceId, "Açaí da Praça", 12.0),
                transaction(invoiceId, "  açaí da praça ", 18.0),
                transaction(invoiceId, "Outra coisa", 5.0),
            )
        )

        assertEquals(2, repository.countTransactionsWithDescription("AÇAÍ DA PRAÇA"))
    }

    @Test
    fun applyCategoryToDescription_updatesStoredTransactionsAndSavesTheOverride() = runBlocking {
        val invoiceId = insertInvoice(bank = "itau", month = 7, hash = "hash-apply")
        db.transactionDao().insertAll(
            listOf(
                transaction(invoiceId, "Posto Shell", 200.0),
                transaction(invoiceId, "posto shell", 100.0),
            )
        )

        val updated = repository.applyCategoryToDescription("Posto Shell", "Transporte")

        assertEquals(2, updated)
        assertEquals(
            listOf("Transporte", "Transporte"),
            db.invoiceDao().getWithTransactions(invoiceId)!!.transactions.map { it.category },
        )
        val override = repository.listCategoryOverrides().single()
        assertEquals("POSTO SHELL", override.description)
        assertEquals("Transporte", override.category)
    }

    @Test
    fun removingACategoryOverride_deletesIt() = runBlocking {
        val invoiceId = db.invoiceDao().insert(
            InvoiceEntity(
                bank = "nubank",
                referenceMonth = 7,
                referenceYear = 2026,
                fileHash = "hash-2",
                processedAt = "2026-07-24T00:00:00",
            )
        )
        db.transactionDao().insertAll(
            listOf(
                com.casshole.data.local.entity.TransactionEntity(
                    invoiceId = invoiceId,
                    date = "2026-06-05",
                    description = "Shopee *LojaPi",
                    amount = 10.0,
                )
            )
        )
        val transactionId = db.invoiceDao().getWithTransactions(invoiceId)!!.transactions.first().id
        repository.updateCategory(transactionId, "Compras")
        val overrideId = repository.listCategoryOverrides().first().id

        repository.removeCategoryOverride(overrideId)

        assertEquals(0, repository.listCategoryOverrides().size)
    }
}
