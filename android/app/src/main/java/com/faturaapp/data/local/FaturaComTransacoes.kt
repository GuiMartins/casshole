package com.faturaapp.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.faturaapp.data.local.entity.FaturaEntity
import com.faturaapp.data.local.entity.TransacaoEntity

data class FaturaComTransacoes(
    @Embedded val fatura: FaturaEntity,
    @Relation(parentColumn = "id", entityColumn = "faturaId")
    val transacoes: List<TransacaoEntity>,
) {
    val id: Long get() = fatura.id
    val banco: String get() = fatura.banco
    val cartao: String get() = fatura.cartao
    val mesReferencia: Int get() = fatura.mesReferencia
    val anoReferencia: Int get() = fatura.anoReferencia
    val arquivoHash: String get() = fatura.arquivoHash
    val processadaEm: String get() = fatura.processadaEm
}
