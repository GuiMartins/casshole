package com.faturaapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "senhas_padrao", indices = [Index(value = ["valor"], unique = true)])
data class SenhaPadraoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val valor: String,
)
