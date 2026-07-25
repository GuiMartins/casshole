package com.faturaapp.data.local

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.faturaapp.data.local.entity.InvoiceEntity
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
                com.faturaapp.data.local.entity.TransactionEntity(
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
                com.faturaapp.data.local.entity.TransactionEntity(
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
