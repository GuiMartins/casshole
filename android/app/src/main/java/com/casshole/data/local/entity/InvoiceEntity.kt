package com.casshole.data.local.entity

import androidx.room.ColumnInfo
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
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "banco") val bank: String,
    @ColumnInfo(name = "cartao") val card: String = "",
    @ColumnInfo(name = "mesReferencia") val referenceMonth: Int,
    @ColumnInfo(name = "anoReferencia") val referenceYear: Int,
    @ColumnInfo(name = "arquivoHash") val fileHash: String,
    @ColumnInfo(name = "processadaEm") val processedAt: String,
    /** When the e-mail carrying this invoice's PDF reached the inbox (epoch millis). Null for
     * invoices imported by hand (picker/share) - the monthly reminder falls back to
     * [processedAt] for those, see InvoiceReminderPlanner. */
    @ColumnInfo(name = "recebidaPorEmailEm") val emailReceivedAt: Long? = null,
)
