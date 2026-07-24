package com.faturaapp.parsing

data class ParsedFatura(
    val banco: String,
    val mesReferencia: Int,
    val anoReferencia: Int,
    val transacoes: List<ParsedTransacao>,
    val cartao: String = "",
)
