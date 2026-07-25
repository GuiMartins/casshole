package com.moneyhole.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.moneyhole.data.local.entity.CategoryOverrideEntity

@Dao
interface CategoryOverrideDao {
    @Query("SELECT * FROM categoria_overrides ORDER BY descricao")
    suspend fun list(): List<CategoryOverrideEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(override: CategoryOverrideEntity)

    @Query("DELETE FROM categoria_overrides WHERE id = :id")
    suspend fun remove(id: Long)
}
