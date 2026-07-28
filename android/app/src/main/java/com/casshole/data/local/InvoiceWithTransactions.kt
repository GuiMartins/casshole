package com.casshole.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.casshole.data.local.entity.InvoiceEntity
import com.casshole.data.local.entity.TransactionEntity

data class InvoiceWithTransactions(
    @Embedded val invoice: InvoiceEntity,
    @Relation(parentColumn = "id", entityColumn = "faturaId")
    val transactions: List<TransactionEntity>,
) {
    val id: Long get() = invoice.id
    val bank: String get() = invoice.bank
    val card: String get() = invoice.card
    val referenceMonth: Int get() = invoice.referenceMonth
    val referenceYear: Int get() = invoice.referenceYear
    val fileHash: String get() = invoice.fileHash
    val processedAt: String get() = invoice.processedAt
}
