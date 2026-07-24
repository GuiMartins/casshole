package com.faturaapp.parsing

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException

class BancoNaoIdentificadoException(message: String) : Exception(message)
class SenhaIncorretaException(message: String) : Exception(message)

private val PARSERS: List<BankParser> = listOf(NubankParser(), ItauParser(), MercadoPagoParser())

object FaturaDispatcher {

    fun processarFatura(pdfBytes: ByteArray, senhasCandidatas: List<String> = emptyList()): ParsedFatura {
        val senha = encontrarSenhaCorreta(pdfBytes, senhasCandidatas)
        val texto = extrairTextoPdfSimples(pdfBytes, senha)

        for (parser in PARSERS) {
            if (parser.matches(texto)) {
                return parser.parse(texto, pdfBytes, senha)
            }
        }

        throw BancoNaoIdentificadoException(
            "Não foi possível identificar o banco desta fatura. Bancos suportados: Nubank, Itaú, Mercado Pago."
        )
    }

    private fun encontrarSenhaCorreta(pdfBytes: ByteArray, senhasCandidatas: List<String>): String {
        val candidatos = (listOf("") + senhasCandidatas).distinct()
        for (senha in candidatos) {
            try {
                PDDocument.load(pdfBytes, senha).use { return senha }
            } catch (e: InvalidPasswordException) {
                continue
            }
        }
        throw SenhaIncorretaException(
            "Esta fatura está protegida por senha e nenhuma das senhas informadas ou " +
                "cadastradas como padrão conseguiu abri-la."
        )
    }
}
