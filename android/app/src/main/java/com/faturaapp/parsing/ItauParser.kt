package com.faturaapp.parsing

private val VENCIMENTO_RE = Regex("Vencimento:\\s*(\\d{2})/(\\d{2})/(\\d{4})", RegexOption.IGNORE_CASE)
private val CARTAO_RE = Regex("Cart[aã]o\\s+\\d{4}\\.XXXX\\.XXXX\\.(\\d{4})", RegexOption.IGNORE_CASE)

// Ex: "30/12 SHOPEE *LOJAPI 07/07 65,58"
// Ex: "07/06 IFD*SORVETERIA EXEMPLO 51,88" (sem parcela)
// Ex: "16/06 PAGAMENTO -1.100,00" (pagamento, sinal negativo, deve ser ignorado)
private val LINHA_TRANSACAO_RE = Regex(
    "^(?<dia>\\d{2})/(?<mes>\\d{2})\\s+" +
        "(?<descricao>.+?)" +
        "(?:\\s+(?<parcelaAtual>\\d{2})/(?<parcelaTotal>\\d{2}))?" +
        "\\s+(?<sinal>[-−])?(?:R\\$\\s*)?(?<valor>\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\s*$"
)

private val TITULAR_RE = Regex("^[A-ZÀ-Ú][A-ZÀ-Ú ]+$")

class ItauParser : BankParser {
    override val banco = "itau"

    override fun matches(texto: String): Boolean {
        val textoLower = texto.lowercase()
        return textoLower.contains("itau") || textoLower.contains("itaú")
    }

    override fun parse(texto: String, pdfBytes: ByteArray, senha: String): ParsedFatura {
        val (mes, ano) = extrairMesAnoReferencia(texto)
        val cartao = extrairCartao(texto)
        val textoColunas = if (pdfBytes.isNotEmpty()) {
            extrairTextoPdfPorColunas(pdfBytes, senha, fracaoCorte = 0.57f)
        } else {
            texto
        }
        val titular = extrairTitular(textoColunas)
        val transacoes = extrairTransacoes(textoColunas, mes, ano, titular)
        return ParsedFatura(banco = banco, cartao = cartao, mesReferencia = mes, anoReferencia = ano, transacoes = transacoes)
    }

    private fun extrairMesAnoReferencia(texto: String): Pair<Int, Int> {
        val match = VENCIMENTO_RE.find(texto)
            ?: throw IllegalStateException("Não foi possível identificar o mês/ano de referência da fatura Itaú")
        return match.groupValues[2].toInt() to match.groupValues[3].toInt()
    }

    private fun extrairCartao(texto: String): String =
        CARTAO_RE.find(texto)?.groupValues?.get(1) ?: ""

    private fun extrairTitular(texto: String): String {
        val header = Regex("Lançamentos:\\s*compras e saques", RegexOption.IGNORE_CASE).find(texto) ?: return ""
        for (linha in texto.substring(header.range.last + 1).lines()) {
            val linhaLimpa = linha.trim()
            if (linhaLimpa.isNotEmpty() && TITULAR_RE.matches(linhaLimpa)) {
                return tituloSimples(linhaLimpa)
            }
        }
        return ""
    }

    private fun extrairSecaoLancamentosAtuais(texto: String): String {
        val inicio = Regex("Lançamentos:\\s*compras e saques", RegexOption.IGNORE_CASE).find(texto) ?: return texto
        val fim = Regex("Total dos lançamentos atuais", RegexOption.IGNORE_CASE).find(texto)
        val fimPos = fim?.range?.first ?: texto.length
        return texto.substring(inicio.range.first, fimPos)
    }

    private fun extrairCidade(proximaLinha: String): String {
        val linhaLimpa = proximaLinha.trim()
        if (linhaLimpa.isEmpty() || LINHA_TRANSACAO_RE.matchEntire(linhaLimpa) != null) {
            return ""
        }
        val palavras = linhaLimpa.split(" ", limit = 2)
        if (palavras.size == 2 && palavras[0] == palavras[0].lowercase()) {
            return palavras[1].trim()
        }
        return linhaLimpa
    }

    private fun extrairTransacoes(
        texto: String,
        mesReferencia: Int,
        anoReferencia: Int,
        titular: String,
    ): List<ParsedTransacao> {
        val secao = extrairSecaoLancamentosAtuais(texto)
        val linhas = secao.lines()
        val transacoes = mutableListOf<ParsedTransacao>()

        for (indice in linhas.indices) {
            val match = LINHA_TRANSACAO_RE.matchEntire(linhas[indice].trim()) ?: continue
            if (match.groups["sinal"] != null) continue

            val mesTransacao = match.groups["mes"]!!.value.toInt()
            var anoTransacao = anoReferencia
            if (mesTransacao > mesReferencia) {
                anoTransacao -= 1
            }

            val cidade = if (indice + 1 < linhas.size) extrairCidade(linhas[indice + 1]) else ""
            val dia = match.groups["dia"]!!.value.toInt()

            transacoes.add(
                ParsedTransacao(
                    data = "%04d-%02d-%02d".format(anoTransacao, mesTransacao, dia),
                    descricao = match.groups["descricao"]!!.value.trim(),
                    valor = parseValorBr(match.groups["valor"]!!.value),
                    parcelaAtual = match.groups["parcelaAtual"]?.value?.toInt(),
                    parcelaTotal = match.groups["parcelaTotal"]?.value?.toInt(),
                    titular = titular,
                    cidade = cidade,
                )
            )
        }
        return transacoes
    }
}

/** Equivalente ao str.title() do Python: capitaliza a primeira letra de cada palavra. */
private fun tituloSimples(texto: String): String =
    texto.lowercase().split(" ").joinToString(" ") { palavra ->
        palavra.replaceFirstChar { it.uppercase() }
    }
