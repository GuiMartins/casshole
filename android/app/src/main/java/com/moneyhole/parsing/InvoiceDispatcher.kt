package com.moneyhole.parsing

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException

class UnidentifiedBankException(message: String) : Exception(message)
class IncorrectPasswordException(message: String) : Exception(message)

private val PARSERS: List<BankParser> = listOf(NubankParser(), ItauParser(), MercadoPagoParser())

object InvoiceDispatcher {

    fun processInvoice(pdfBytes: ByteArray, candidatePasswords: List<String> = emptyList()): ParsedInvoice {
        val password = findCorrectPassword(pdfBytes, candidatePasswords)
        val text = extractSimplePdfText(pdfBytes, password)

        for (parser in PARSERS) {
            if (parser.matches(text)) {
                return parser.parse(text, pdfBytes, password)
            }
        }

        throw UnidentifiedBankException(
            "Não foi possível identificar o banco desta fatura. Bancos suportados: Nubank, Itaú, Mercado Pago."
        )
    }

    private fun findCorrectPassword(pdfBytes: ByteArray, candidatePasswords: List<String>): String {
        val candidates = (listOf("") + candidatePasswords).distinct()
        for (password in candidates) {
            try {
                PDDocument.load(pdfBytes, password).use { return password }
            } catch (e: InvalidPasswordException) {
                continue
            }
        }
        throw IncorrectPasswordException(
            "Esta fatura está protegida por senha e nenhuma das senhas informadas ou " +
                "cadastradas como padrão conseguiu abri-la."
        )
    }
}
