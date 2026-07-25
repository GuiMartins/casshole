package com.moneyhole.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.moneyhole.data.local.entity.InvoiceEntity
import com.moneyhole.data.local.entity.DefaultPasswordEntity
import com.moneyhole.data.local.entity.TransactionEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomFoundationTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    @Test
    fun insertInvoiceWithTransactions_showsUpInQuery() = runBlocking {
        val invoiceId = db.invoiceDao().insert(
            InvoiceEntity(
                bank = "itau",
                card = "2222",
                referenceMonth = 7,
                referenceYear = 2026,
                fileHash = "hash-1",
                processedAt = "2026-07-24T00:00:00",
            )
        )
        db.transactionDao().insertAll(
            listOf(
                TransactionEntity(invoiceId = invoiceId, date = "2026-06-07", description = "IFOOD", amount = 51.88),
                TransactionEntity(invoiceId = invoiceId, date = "2026-06-08", description = "WELLHUB", amount = 69.99),
            )
        )

        val result = db.invoiceDao().getWithTransactions(invoiceId)
        assertEquals(2, result?.transactions?.size)
        assertEquals(121.87, result!!.transactions.sumOf { it.amount }, 0.001)
    }

    @Test
    fun deleteInvoice_cascadeDeletesTransactions() = runBlocking {
        val invoice = InvoiceEntity(
            bank = "nubank",
            referenceMonth = 7,
            referenceYear = 2026,
            fileHash = "hash-cascade",
            processedAt = "2026-07-24T00:00:00",
        )
        val invoiceId = db.invoiceDao().insert(invoice)
        db.transactionDao().insertAll(
            listOf(TransactionEntity(invoiceId = invoiceId, date = "2026-06-01", description = "X", amount = 10.0))
        )

        db.invoiceDao().delete(invoice.copy(id = invoiceId))

        assertNull(db.invoiceDao().getWithTransactions(invoiceId))
        assertTrue(db.transactionDao().listByPeriod(7, 2026).isEmpty())
    }

    @Test(expected = SQLiteConstraintException::class)
    fun duplicateFileHash_throwsException(): Unit = runBlocking {
        val base = InvoiceEntity(
            bank = "itau",
            referenceMonth = 7,
            referenceYear = 2026,
            fileHash = "hash-repetido",
            processedAt = "2026-07-24T00:00:00",
        )
        db.invoiceDao().insert(base)
        db.invoiceDao().insert(base.copy(card = "outro-cartao"))
        Unit
    }

    @Test(expected = SQLiteConstraintException::class)
    fun duplicatePeriodSameBankAndCard_throwsException(): Unit = runBlocking {
        db.invoiceDao().insert(
            InvoiceEntity(
                bank = "mercadopago", card = "1111", referenceMonth = 7, referenceYear = 2026,
                fileHash = "hash-a", processedAt = "2026-07-24T00:00:00",
            )
        )
        db.invoiceDao().insert(
            InvoiceEntity(
                bank = "mercadopago", card = "1111", referenceMonth = 7, referenceYear = 2026,
                fileHash = "hash-b", processedAt = "2026-07-24T00:00:00",
            )
        )
        Unit
    }

    @Test(expected = SQLiteConstraintException::class)
    fun duplicateDefaultPassword_throwsException(): Unit = runBlocking {
        db.defaultPasswordDao().insert(DefaultPasswordEntity(value = "12345"))
        db.defaultPasswordDao().insert(DefaultPasswordEntity(value = "12345"))
        Unit
    }
}
