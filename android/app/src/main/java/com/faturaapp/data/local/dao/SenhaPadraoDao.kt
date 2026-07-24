package com.faturaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.faturaapp.data.local.entity.SenhaPadraoEntity

@Dao
interface SenhaPadraoDao {
    @Query("SELECT * FROM senhas_padrao")
    suspend fun listar(): List<SenhaPadraoEntity>

    @Insert
    suspend fun inserir(senha: SenhaPadraoEntity): Long

    @Query("DELETE FROM senhas_padrao WHERE id = :id")
    suspend fun remover(id: Long)
}
