package com.faturaapp.parsing

private val MESES_ABREV = mapOf(
    "JAN" to 1, "FEV" to 2, "MAR" to 3, "ABR" to 4, "MAI" to 5, "JUN" to 6,
    "JUL" to 7, "AGO" to 8, "SET" to 9, "OUT" to 10, "NOV" to 11, "DEZ" to 12,
)
private val MESES_ALTERNATIVAS = MESES_ABREV.keys.joinToString("|")

private val DATA_VENCIMENTO_RE = Regex(
    "Data de vencimento:\\s*\\d{1,2}\\s+($MESES_ALTERNATIVAS)\\s+(\\d{4})",
    RegexOption.IGNORE_CASE,
)

// Ex: "16 JUN •••• 3333 Pg *Universal Music St - Parcela 2/2 R$ 99,90"
// Ex: "16 JUN KaBuM! - NuPay - Parcela 2/8 R$ 28,00" (sem digitos do cartao)
// Ex: "23 JUN Pagamento em 23 JUN −R$ 1.500,00" (pagamento, sinal negativo)
private val LINHA_TRANSACAO_RE = Regex(
    "^(?<dia>\\d{2})\\s+(?<mes>$MESES_ALTERNATIVAS)\\s+" +
        "(?:•{2,6}\\s*(?<cartao>\\d{3,4})\\s+)?" +
        "(?<descricao>.+?)" +
        "(?:\\s-\\s*Parcela\\s+(?<parcelaAtual>\\d{1,2})/(?<parcelaTotal>\\d{1,2}))?" +
        "\\s+(?<sinal>[−-])?R\\$\\s*(?<valor>\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\s*$",
    RegexOption.IGNORE_CASE,
)

// Linhas de "Pagamentos e Financiamentos" que nao sao gastos reais.
private val DESCRICAO_IGNORAR_RE = Regex("^(pagamento em|saldo restante da fatura)", RegexOption.IGNORE_CASE)

// Cabecalho de secao que marca o titular das compras seguintes, ex:
// "Fulano de Tal R$ 1.111,10" ou "Compras de Beltrana B Souza R$ 2.222,20"
private val TITULAR_HEADER_RE = Regex(
    "^(?:Compras de\\s+)?([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ .]+?)\\s+R\\$\\s*\\d{1,3}(?:\\.\\d{3})*,\\d{2}\\s*$"
)

class NubankParser : BankParser {
    override val banco = "nubank"

    override fun matches(texto: String): Boolean = texto.lowercase().contains("nubank")

    override fun parse(texto: String, pdfBytes: ByteArray, senha: String): ParsedFatura {
        val (mes, ano) = extrairMesAnoReferencia(texto)
        val transacoes = extrairTransacoes(texto, mes, ano)
        return ParsedFatura(banco = banco, mesReferencia = mes, anoReferencia = ano, transacoes = transacoes)
    }

    private fun extrairMesAnoReferencia(texto: String): Pair<Int, Int> {
        val match = DATA_VENCIMENTO_RE.find(texto)
            ?: throw IllegalStateException("Não foi possível identificar o mês/ano de referência da fatura Nubank")
        val mes = MESES_ABREV.getValue(match.groupValues[1].uppercase())
        val ano = match.groupValues[2].toInt()
        return mes to ano
    }

    private fun extrairTransacoes(
        texto: String,
        mesReferencia: Int,
        anoReferencia: Int,
    ): List<ParsedTransacao> {
        val transacoes = mutableListOf<ParsedTransacao>()
        var titularAtual = ""

        for (linha in texto.lines()) {
            val linhaLimpa = linha.trim()
            val match = LINHA_TRANSACAO_RE.matchEntire(linhaLimpa)
            if (match == null) {
                val matchTitular = TITULAR_HEADER_RE.matchEntire(linhaLimpa)
                if (matchTitular != null && !linhaLimpa.lowercase().contains("pagamento")) {
                    titularAtual = matchTitular.groupValues[1].trim()
                }
                continue
            }

            val descricao = match.groups["descricao"]!!.value.trim()
            val sinal = match.groups["sinal"]?.value

            if (sinal != null || DESCRICAO_IGNORAR_RE.containsMatchIn(descricao)) {
                continue
            }

            val mesTransacao = MESES_ABREV.getValue(match.groups["mes"]!!.value.uppercase())
            var anoTransacao = anoReferencia
            if (mesTransacao > mesReferencia) {
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
                    titular = titularAtual,
                    cartao = match.groups["cartao"]?.value ?: "",
                )
            )
        }
        return transacoes
    }
}
