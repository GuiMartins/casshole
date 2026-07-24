package com.faturaapp.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transacoes",
    foreignKeys = [
        ForeignKey(
            entity = FaturaEntity::class,
            parentColumns = ["id"],
            childColumns = ["faturaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["faturaId"])],
)
data class TransacaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val faturaId: Long,
    val data: String,
    val descricao: String,
    val valor: Double,
    val categoria: String = "Outros",
    val parcelaAtual: Int? = null,
    val parcelaTotal: Int? = null,
    val titular: String = "",
    val cidade: String = "",
    val cartao: String = "",
)
