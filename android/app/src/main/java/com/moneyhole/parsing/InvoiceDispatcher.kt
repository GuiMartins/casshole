package com.moneyhole.parsing

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException

class UnidentifiedBankException(message: String) : Exception(message)
/** Text looks like a bank invoice (matched [INVOICE_KEYWORDS]) but the bank isn't one we
 * recognize by name at all - kept apart from [UnsupportedBankException] since we can't even
 * name the bank to the user here, just that it doesn't look like Nubank/Itau/Mercado Pago. */
class NotABankInvoiceException(message: String) : Exception(message)
/** Bank identified by name ([bankName]) but no parser implemented for it yet - distinct from
 * [UnidentifiedBankException] so callers can show a "não suportado ainda" message instead of a
 * generic failure. */
class UnsupportedBankException(val bankName: String, message: String) : Exception(message)
class IncorrectPasswordException(message: String) : Exception(message)

private val PARSERS: List<BankParser> = listOf(NubankParser(), ItauParser(), MercadoPagoParser())

/** Generic credit-card-invoice vocabulary, independent of any specific bank's wording - used to
 * tell "not a bank invoice at all" (e.g. a random PDF attachment in an email) apart from "a bank
 * invoice we don't have a parser for yet". Deliberately broad: false positives here just fall
 * through to [UnsupportedBankException]/[UnidentifiedBankException], which is harmless, while
 * false negatives would wrongly hide a real invoice as [NotABankInvoiceException]. */
private val INVOICE_KEYWORDS = listOf(
    "fatura", "cartão de crédito", "cartao de credito", "vencimento",
    "limite de crédito", "limite de credito", "pagamento mínimo", "pagamento minimo",
)

/** Bank names we recognize but don't have a parser for - used only to give a more specific
 * "banco X ainda não suportado" message instead of the generic "banco não identificado" one. Not
 * an exhaustive list of every Brazilian bank, just the common ones worth naming explicitly. */
private val KNOWN_UNSUPPORTED_BANKS = listOf(
    "bradesco", "santander", "banco do brasil", "caixa econômica", "caixa economica",
    "banco inter", "c6 bank", "picpay", "btg pactual", "banco original",
    "pagbank", "pagseguro", "banco neon", "banco next", "sicoob", "sicredi", "banco safra",
)

object InvoiceDispatcher {

    fun processInvoice(pdfBytes: ByteArray, candidatePasswords: List<String> = emptyList()): ParsedInvoice {
        val password = findCorrectPassword(pdfBytes, candidatePasswords)
        val text = extractSimplePdfText(pdfBytes, password)

        for (parser in PARSERS) {
            if (parser.matches(text)) {
                return parser.parse(text, pdfBytes, password)
            }
        }

        val textLower = text.lowercase()
        if (INVOICE_KEYWORDS.none { textLower.contains(it) }) {
            throw NotABankInvoiceException("Este PDF não parece ser uma fatura de cartão de crédito.")
        }

        val recognizedBank = KNOWN_UNSUPPORTED_BANKS.firstOrNull { textLower.contains(it) }
        if (recognizedBank != null) {
            throw UnsupportedBankException(
                recognizedBank,
                "Fatura do banco \"$recognizedBank\" identificada, mas esse banco ainda não é suportado. " +
                    "Bancos suportados: Nubank, Itaú, Mercado Pago.",
            )
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
