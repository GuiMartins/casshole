package com.casshole.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transacoes",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["faturaId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["faturaId"])],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "faturaId") val invoiceId: Long,
    @ColumnInfo(name = "data") val date: String,
    @ColumnInfo(name = "descricao") val description: String,
    @ColumnInfo(name = "valor") val amount: Double,
    @ColumnInfo(name = "categoria") val category: String = "Outros",
    @ColumnInfo(name = "parcelaAtual") val currentInstallment: Int? = null,
    @ColumnInfo(name = "parcelaTotal") val totalInstallments: Int? = null,
    @ColumnInfo(name = "titular") val cardholder: String = "",
    @ColumnInfo(name = "cidade") val city: String = "",
    @ColumnInfo(name = "cartao") val card: String = "",
)
