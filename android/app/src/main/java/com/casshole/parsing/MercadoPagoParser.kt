package com.casshole.parsing

private val DUE_DATE_REGEX = Regex("Vencimento:\\s*(\\d{2})/(\\d{2})/(\\d{4})", RegexOption.IGNORE_CASE)
private val CARD_REGEX = Regex("Cart[aã]o\\s+\\w+\\s*\\[\\*+(\\d{4})\\]", RegexOption.IGNORE_CASE)

// Ex: "28/11 MERCADOLIVRE*ESHOPIMPORTA Parcela 19 de 24 R$ 116,21"
// Ex: "15/06 Pagamento da fatura de junho/2026 R$ 1.200,00" (payment, ignore)
private val TRANSACTION_LINE_REGEX = Regex(
    "^(?<day>\\d{2})/(?<month>\\d{2})\\s+" +
        "(?<description>.+?)" +
        "(?:\\s+Parcela\\s+(?<currentInstallment>\\d{1,2})\\s+de\\s+(?<totalInstallments>\\d{1,2}))?" +
        "\\s+R\\$\\s*(?<amount>\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\s*$",
    RegexOption.IGNORE_CASE,
)

private val IGNORED_DESCRIPTION_REGEX = Regex("^pagamento da fatura", RegexOption.IGNORE_CASE)
private val CARDHOLDER_REGEX = Regex("^(.*?)\\s*Emitida em:", setOf(RegexOption.DOT_MATCHES_ALL))
private val WHITESPACE_REGEX = Regex("\\s+")

class MercadoPagoParser : BankParser {
    override val bank = "mercadopago"

    override fun matches(text: String): Boolean {
        val textLower = text.lowercase()
        return textLower.contains("mercado pago") || textLower.contains("mercadopago")
    }

    override fun parse(text: String, pdfBytes: ByteArray, password: String): ParsedInvoice {
        val (month, year) = extractReferenceMonthYear(text)
        val card = extractCard(text)
        val cardholder = extractCardholder(text)
        val transactions = extractTransactions(text, month, year, cardholder)
        return ParsedInvoice(bank = bank, card = card, referenceMonth = month, referenceYear = year, transactions = transactions)
    }

    private fun extractReferenceMonthYear(text: String): Pair<Int, Int> {
        val match = DUE_DATE_REGEX.find(text)
            ?: throw IllegalStateException("Não foi possível identificar o mês/ano de referência da fatura Mercado Pago")
        return match.groupValues[2].toInt() to match.groupValues[3].toInt()
    }

    private fun extractCard(text: String): String =
        CARD_REGEX.find(text)?.groupValues?.get(1) ?: ""

    /**
     * The cardholder's name appears at the top of the invoice, before
     * 'Emitida em:', sometimes wrapped across more than one line because of
     * column width.
     */
    private fun extractCardholder(text: String): String {
        val match = CARDHOLDER_REGEX.find(text) ?: return ""
        return match.groupValues[1].trim().replace(WHITESPACE_REGEX, " ")
    }

    private fun extractTransactions(
        text: String,
        referenceMonth: Int,
        referenceYear: Int,
        cardholder: String,
    ): List<ParsedTransaction> {
        val transactions = mutableListOf<ParsedTransaction>()
        for (line in text.lines()) {
            val match = TRANSACTION_LINE_REGEX.matchEntire(line.trim()) ?: continue
            val description = match.groups["description"]!!.value.trim()
            if (IGNORED_DESCRIPTION_REGEX.containsMatchIn(description)) continue

            val transactionMonth = match.groups["month"]!!.value.toInt()
            var transactionYear = referenceYear
            if (transactionMonth > referenceMonth) {
                // Installment purchases show the original purchase date, which
                // can be months or years in the past (not the billing date).
                transactionYear -= 1
            }

            val day = match.groups["day"]!!.value.toInt()
            transactions.add(
                ParsedTransaction(
                    date = "%04d-%02d-%02d".format(transactionYear, transactionMonth, day),
                    description = description,
                    amount = parseBrazilianAmount(match.groups["amount"]!!.value),
                    currentInstallment = match.groups["currentInstallment"]?.value?.toInt(),
                    totalInstallments = match.groups["totalInstallments"]?.value?.toInt(),
                    cardholder = cardholder,
                )
            )
        }
        return transactions
    }
}
