package com.casshole.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.casshole.data.local.entity.TransactionEntity

@Dao
interface TransactionDao {
    @Insert
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Query("UPDATE transacoes SET categoria = :category WHERE id = :id")
    suspend fun updateCategory(id: Long, category: String)

    @Query("SELECT * FROM transacoes WHERE id = :id")
    suspend fun findById(id: Long): TransactionEntity?

    @Query(
        """
        SELECT transacoes.* FROM transacoes
        INNER JOIN faturas ON faturas.id = transacoes.faturaId
        WHERE faturas.mesReferencia = :month AND faturas.anoReferencia = :year
        """
    )
    suspend fun listByPeriod(month: Int, year: Int): List<TransactionEntity>
}
