package com.faturaapp.parsing

data class ParsedTransacao(
    val data: String,
    val descricao: String,
    val valor: Double,
    val parcelaAtual: Int? = null,
    val parcelaTotal: Int? = null,
    val titular: String = "",
    val cidade: String = "",
    val cartao: String = "",
)
