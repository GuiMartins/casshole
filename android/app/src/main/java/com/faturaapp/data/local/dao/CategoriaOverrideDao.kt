package com.faturaapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.faturaapp.data.local.entity.CategoriaOverrideEntity

@Dao
interface CategoriaOverrideDao {
    @Query("SELECT * FROM categoria_overrides ORDER BY descricao")
    suspend fun listar(): List<CategoriaOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun salvar(override: CategoriaOverrideEntity)

    @Query("DELETE FROM categoria_overrides WHERE id = :id")
    suspend fun remover(id: Long)
}
