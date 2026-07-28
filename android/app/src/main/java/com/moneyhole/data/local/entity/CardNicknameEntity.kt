package com.moneyhole.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "apelidos_cartao",
    indices = [Index(value = ["banco", "cartao"], unique = true)],
)
data class CardNicknameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "banco") val bank: String,
    @ColumnInfo(name = "cartao") val card: String,
    @ColumnInfo(name = "apelido") val nickname: String,
)
