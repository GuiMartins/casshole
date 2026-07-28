package com.casshole.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.casshole.data.local.entity.DefaultPasswordEntity

@Dao
interface DefaultPasswordDao {
    @Query("SELECT * FROM senhas_padrao")
    suspend fun list(): List<DefaultPasswordEntity>

    @Insert
    suspend fun insert(password: DefaultPasswordEntity): Long

    @Query("DELETE FROM senhas_padrao WHERE id = :id")
    suspend fun remove(id: Long)
}
