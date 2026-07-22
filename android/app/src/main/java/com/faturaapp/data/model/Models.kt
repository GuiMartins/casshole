package com.faturaapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Transacao(
    val id: Int,
    val data: String,
    val descricao: String,
    val valor: Double,
    val categoria: String,
    val parcela_atual: Int? = null,
    val parcela_total: Int? = null,
)

@Serializable
data class Fatura(
    val id: Int,
    val banco: String,
    val cartao: String = "",
    val mes_referencia: Int,
    val ano_referencia: Int,
    val processada_em: String,
    val transacoes: List<Transacao> = emptyList(),
)

@Serializable
data class ResumoCategoria(
    val categoria: String,
    val total: Double,
)

@Serializable
data class ResumoMensal(
    val mes_referencia: Int,
    val ano_referencia: Int,
    val total_gasto: Double,
    val por_categoria: List<ResumoCategoria>,
)

@Serializable
data class ComparacaoMensal(
    val meses: List<ResumoMensal>,
    val variacao_percentual_total: Double? = null,
)

@Serializable
data class SenhaPadrao(
    val id: Int,
    val valor: String,
    val descricao: String? = null,
)

@Serializable
data class SenhaPadraoCreate(
    val valor: String,
    val descricao: String? = null,
)
