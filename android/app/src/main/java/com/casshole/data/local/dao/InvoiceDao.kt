package com.casshole.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.casshole.data.local.InvoiceWithTransactions
import com.casshole.data.local.entity.InvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {
    @Insert
    suspend fun insert(invoice: InvoiceEntity): Long

    @Delete
    suspend fun delete(invoice: InvoiceEntity)

    @Transaction
    @Query("SELECT * FROM faturas ORDER BY anoReferencia, mesReferencia")
    suspend fun listWithTransactions(): List<InvoiceWithTransactions>

    @Transaction
    @Query("SELECT * FROM faturas WHERE id = :id")
    suspend fun getWithTransactions(id: Long): InvoiceWithTransactions?

    @Query("SELECT * FROM faturas WHERE arquivoHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): InvoiceEntity?

    @Query(
        """
        SELECT * FROM faturas
        WHERE banco = :bank AND cartao = :card
          AND mesReferencia = :month AND anoReferencia = :year
        LIMIT 1
        """
    )
    suspend fun findByPeriod(bank: String, card: String, month: Int, year: Int): InvoiceEntity?

    @Query("SELECT * FROM faturas")
    suspend fun listAll(): List<InvoiceEntity>

    /** Emits again whenever the invoices table changes (import, delete) - drives the reminder schedule. */
    @Query("SELECT * FROM faturas")
    fun observeAll(): Flow<List<InvoiceEntity>>

    @Query("SELECT COUNT(*) FROM faturas")
    suspend fun count(): Int

    @Query("UPDATE faturas SET recebidaPorEmailEm = :receivedAt WHERE id = :id")
    suspend fun setEmailReceivedAt(id: Long, receivedAt: Long)
}
