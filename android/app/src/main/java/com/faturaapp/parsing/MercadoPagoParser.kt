package com.faturaapp.parsing

private val VENCIMENTO_RE = Regex("Vencimento:\\s*(\\d{2})/(\\d{2})/(\\d{4})", RegexOption.IGNORE_CASE)
private val CARTAO_RE = Regex("Cart[aã]o\\s+\\w+\\s*\\[\\*+(\\d{4})\\]", RegexOption.IGNORE_CASE)

// Ex: "28/11 MERCADOLIVRE*ESHOPIMPORTA Parcela 19 de 24 R$ 116,21"
// Ex: "15/06 Pagamento da fatura de junho/2026 R$ 1.200,00" (pagamento, ignorar)
private val LINHA_TRANSACAO_RE = Regex(
    "^(?<dia>\\d{2})/(?<mes>\\d{2})\\s+" +
        "(?<descricao>.+?)" +
        "(?:\\s+Parcela\\s+(?<parcelaAtual>\\d{1,2})\\s+de\\s+(?<parcelaTotal>\\d{1,2}))?" +
        "\\s+R\\$\\s*(?<valor>\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\s*$",
    RegexOption.IGNORE_CASE,
)

private val DESCRICAO_IGNORAR_RE = Regex("^pagamento da fatura", RegexOption.IGNORE_CASE)
private val TITULAR_RE = Regex("^(.*?)\\s*Emitida em:", setOf(RegexOption.DOT_MATCHES_ALL))
private val ESPACOS_RE = Regex("\\s+")

class MercadoPagoParser : BankParser {
    override val banco = "mercadopago"

    override fun matches(texto: String): Boolean {
        val textoLower = texto.lowercase()
        return textoLower.contains("mercado pago") || textoLower.contains("mercadopago")
    }

    override fun parse(texto: String, pdfBytes: ByteArray, senha: String): ParsedFatura {
        val (mes, ano) = extrairMesAnoReferencia(texto)
        val cartao = extrairCartao(texto)
        val titular = extrairTitular(texto)
        val transacoes = extrairTransacoes(texto, mes, ano, titular)
        return ParsedFatura(banco = banco, cartao = cartao, mesReferencia = mes, anoReferencia = ano, transacoes = transacoes)
    }

    private fun extrairMesAnoReferencia(texto: String): Pair<Int, Int> {
        val match = VENCIMENTO_RE.find(texto)
            ?: throw IllegalStateException("Não foi possível identificar o mês/ano de referência da fatura Mercado Pago")
        return match.groupValues[2].toInt() to match.groupValues[3].toInt()
    }

    private fun extrairCartao(texto: String): String =
        CARTAO_RE.find(texto)?.groupValues?.get(1) ?: ""

    /**
     * O nome do titular aparece no topo da fatura, antes de 'Emitida em:',
     * as vezes quebrado em mais de uma linha por causa da largura da coluna.
     */
    private fun extrairTitular(texto: String): String {
        val match = TITULAR_RE.find(texto) ?: return ""
        return match.groupValues[1].trim().replace(ESPACOS_RE, " ")
    }

    private fun extrairTransacoes(
        texto: String,
        mesReferencia: Int,
        anoReferencia: Int,
        titular: String,
    ): List<ParsedTransacao> {
        val transacoes = mutableListOf<ParsedTransacao>()
        for (linha in texto.lines()) {
            val match = LINHA_TRANSACAO_RE.matchEntire(linha.trim()) ?: continue
            val descricao = match.groups["descricao"]!!.value.trim()
            if (DESCRICAO_IGNORAR_RE.containsMatchIn(descricao)) continue

            val mesTransacao = match.groups["mes"]!!.value.toInt()
            var anoTransacao = anoReferencia
            if (mesTransacao > mesReferencia) {
                // Compras parceladas mostram a data da compra original, que
                // pode ser de meses ou anos atras (nao a data de cobranca).
                anoTransacao -= 1
            }

            val dia = match.groups["dia"]!!.value.toInt()
            transacoes.add(
                ParsedTransacao(
                    data = "%04d-%02d-%02d".format(anoTransacao, mesTransacao, dia),
                    descricao = descricao,
                    valor = parseValorBr(match.groups["valor"]!!.value),
                    parcelaAtual = match.groups["parcelaAtual"]?.value?.toInt(),
                    parcelaTotal = match.groups["parcelaTotal"]?.value?.toInt(),
                    titular = titular,
                )
            )
        }
        return transacoes
    }
}
