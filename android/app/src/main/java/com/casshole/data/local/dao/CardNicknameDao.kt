package com.casshole.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.casshole.data.local.entity.CardNicknameEntity

@Dao
interface CardNicknameDao {
    @Query("SELECT * FROM apelidos_cartao")
    suspend fun list(): List<CardNicknameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(nickname: CardNicknameEntity)

    @Query("DELETE FROM apelidos_cartao WHERE banco = :bank AND cartao = :card")
    suspend fun remove(bank: String, card: String)
}
