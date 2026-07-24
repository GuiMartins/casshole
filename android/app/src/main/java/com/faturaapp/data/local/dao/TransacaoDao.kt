package com.faturaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.faturaapp.data.local.entity.TransacaoEntity

@Dao
interface TransacaoDao {
    @Insert
    suspend fun inserirTodas(transacoes: List<TransacaoEntity>)

    @Query("UPDATE transacoes SET categoria = :categoria WHERE id = :id")
    suspend fun atualizarCategoria(id: Long, categoria: String)

    @Query(
        """
        SELECT transacoes.* FROM transacoes
        INNER JOIN faturas ON faturas.id = transacoes.faturaId
        WHERE faturas.mesReferencia = :mes AND faturas.anoReferencia = :ano
        """
    )
    suspend fun listarPorPeriodo(mes: Int, ano: Int): List<TransacaoEntity>
}
