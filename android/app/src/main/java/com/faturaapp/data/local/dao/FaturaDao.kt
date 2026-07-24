package com.faturaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.faturaapp.data.local.FaturaComTransacoes
import com.faturaapp.data.local.entity.FaturaEntity

@Dao
interface FaturaDao {
    @Insert
    suspend fun inserir(fatura: FaturaEntity): Long

    @Delete
    suspend fun deletar(fatura: FaturaEntity)

    @Transaction
    @Query("SELECT * FROM faturas ORDER BY anoReferencia, mesReferencia")
    suspend fun listarComTransacoes(): List<FaturaComTransacoes>

    @Transaction
    @Query("SELECT * FROM faturas WHERE id = :id")
    suspend fun obterComTransacoes(id: Long): FaturaComTransacoes?

    @Query("SELECT * FROM faturas WHERE arquivoHash = :hash LIMIT 1")
    suspend fun buscarPorHash(hash: String): FaturaEntity?

    @Query(
        """
        SELECT * FROM faturas
        WHERE banco = :banco AND cartao = :cartao
          AND mesReferencia = :mes AND anoReferencia = :ano
        LIMIT 1
        """
    )
    suspend fun buscarPorPeriodo(banco: String, cartao: String, mes: Int, ano: Int): FaturaEntity?
}
