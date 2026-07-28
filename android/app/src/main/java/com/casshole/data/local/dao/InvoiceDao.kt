package com.casshole.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.casshole.data.local.InvoiceWithTransactions
import com.casshole.data.local.entity.InvoiceEntity

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
}
