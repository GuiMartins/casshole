package com.moneyhole.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "senhas_padrao", indices = [Index(value = ["valor"], unique = true)])
data class DefaultPasswordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "valor") val value: String,
)
