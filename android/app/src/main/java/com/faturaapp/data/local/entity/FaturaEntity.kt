package com.faturaapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "faturas",
    indices = [
        Index(value = ["arquivoHash"], unique = true),
        Index(value = ["banco", "cartao", "mesReferencia", "anoReferencia"], unique = true),
    ],
)
data class FaturaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val banco: String,
    val cartao: String = "",
    val mesReferencia: Int,
    val anoReferencia: Int,
    val arquivoHash: String,
    val processadaEm: String,
)
