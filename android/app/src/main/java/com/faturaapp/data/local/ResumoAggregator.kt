package com.faturaapp.data.local

import com.faturaapp.data.local.entity.TransacaoEntity

data class ResumoCategoria(val categoria: String, val total: Double)

data class ResumoMensal(
    val mesReferencia: Int,
    val anoReferencia: Int,
    val totalGasto: Double,
    val porCategoria: List<ResumoCategoria>,
)

data class ComparacaoMensal(
    val meses: List<ResumoMensal>,
    val variacaoPercentualTotal: Double?,
)

object ResumoAggregator {

    fun montarResumoMensal(transacoes: List<TransacaoEntity>, mes: Int, ano: Int): ResumoMensal? {
        if (transacoes.isEmpty()) return null

        val porCategoria = transacoes
            .groupBy { it.categoria }
            .mapValues { (_, itens) -> itens.sumOf { it.valor } }
            .toList()
            .sortedByDescending { (_, total) -> total }
            .map { (categoria, total) -> ResumoCategoria(categoria, arredondar(total)) }

        return ResumoMensal(
            mesReferencia = mes,
            anoReferencia = ano,
            totalGasto = arredondar(transacoes.sumOf { it.valor }),
            porCategoria = porCategoria,
        )
    }

    fun compararMeses(periodos: List<Triple<Int, Int, List<TransacaoEntity>>>): ComparacaoMensal {
        val resumos = periodos.mapNotNull { (mes, ano, transacoes) -> montarResumoMensal(transacoes, mes, ano) }

        val variacao = if (resumos.size >= 2 && resumos.first().totalGasto > 0) {
            arredondar(
                (resumos.last().totalGasto - resumos.first().totalGasto) / resumos.first().totalGasto * 100
            )
        } else {
            null
        }

        return ComparacaoMensal(meses = resumos, variacaoPercentualTotal = variacao)
    }

    private fun arredondar(valor: Double): Double = Math.round(valor * 100) / 100.0
}
