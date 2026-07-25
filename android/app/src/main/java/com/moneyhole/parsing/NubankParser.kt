package com.moneyhole.parsing

private val MONTH_ABBREVIATIONS = mapOf(
    "JAN" to 1, "FEV" to 2, "MAR" to 3, "ABR" to 4, "MAI" to 5, "JUN" to 6,
    "JUL" to 7, "AGO" to 8, "SET" to 9, "OUT" to 10, "NOV" to 11, "DEZ" to 12,
)
private val MONTH_ALTERNATIVES = MONTH_ABBREVIATIONS.keys.joinToString("|")

private val DUE_DATE_REGEX = Regex(
    "Data de vencimento:\\s*\\d{1,2}\\s+($MONTH_ALTERNATIVES)\\s+(\\d{4})",
    RegexOption.IGNORE_CASE,
)

// Ex: "16 JUN •••• 3333 Pg *Universal Music St - Parcela 2/2 R$ 99,90"
// Ex: "16 JUN KaBuM! - NuPay - Parcela 2/8 R$ 28,00" (no card digits)
// Ex: "23 JUN Pagamento em 23 JUN −R$ 1.500,00" (payment, negative sign)
private val TRANSACTION_LINE_REGEX = Regex(
    "^(?<day>\\d{2})\\s+(?<month>$MONTH_ALTERNATIVES)\\s+" +
        "(?:•{2,6}\\s*(?<card>\\d{3,4})\\s+)?" +
        "(?<description>.+?)" +
        "(?:\\s-\\s*Parcela\\s+(?<currentInstallment>\\d{1,2})/(?<totalInstallments>\\d{1,2}))?" +
        "\\s+(?<sign>[−-])?R\\$\\s*(?<amount>\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\s*$",
    RegexOption.IGNORE_CASE,
)

// "Pagamentos e Financiamentos" lines that aren't real expenses.
private val IGNORED_DESCRIPTION_REGEX = Regex("^(pagamento em|saldo restante da fatura)", RegexOption.IGNORE_CASE)

// Section header marking the cardholder for the following purchases, ex:
// "Fulano de Tal R$ 1.111,10" or "Compras de Beltrana B Souza R$ 2.222,20"
private val CARDHOLDER_HEADER_REGEX = Regex(
    "^(?:Compras de\\s+)?([A-Za-zÀ-ÿ][A-Za-zÀ-ÿ .]+?)\\s+R\\$\\s*\\d{1,3}(?:\\.\\d{3})*,\\d{2}\\s*$"
)

class NubankParser : BankParser {
    override val bank = "nubank"

    override fun matches(text: String): Boolean = text.lowercase().contains("nubank")

    override fun parse(text: String, pdfBytes: ByteArray, password: String): ParsedInvoice {
        val (month, year) = extractReferenceMonthYear(text)
        val transactions = extractTransactions(text, month, year)
        return ParsedInvoice(bank = bank, referenceMonth = month, referenceYear = year, transactions = transactions)
    }

    private fun extractReferenceMonthYear(text: String): Pair<Int, Int> {
        val match = DUE_DATE_REGEX.find(text)
            ?: throw IllegalStateException("Não foi possível identificar o mês/ano de referência da fatura Nubank")
        val month = MONTH_ABBREVIATIONS.getValue(match.groupValues[1].uppercase())
        val year = match.groupValues[2].toInt()
        return month to year
    }

    private fun extractTransactions(
        text: String,
        referenceMonth: Int,
        referenceYear: Int,
    ): List<ParsedTransaction> {
        val transactions = mutableListOf<ParsedTransaction>()
        var currentCardholder = ""

        for (line in text.lines()) {
            val cleanLine = line.trim()
            val match = TRANSACTION_LINE_REGEX.matchEntire(cleanLine)
            if (match == null) {
                val cardholderMatch = CARDHOLDER_HEADER_REGEX.matchEntire(cleanLine)
                if (cardholderMatch != null && !cleanLine.lowercase().contains("pagamento")) {
                    currentCardholder = cardholderMatch.groupValues[1].trim()
                }
                continue
            }

            val description = match.groups["description"]!!.value.trim()
            val sign = match.groups["sign"]?.value

            if (sign != null || IGNORED_DESCRIPTION_REGEX.containsMatchIn(description)) {
                continue
            }

            val transactionMonth = MONTH_ABBREVIATIONS.getValue(match.groups["month"]!!.value.uppercase())
            var transactionYear = referenceYear
            if (transactionMonth > referenceMonth) {
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
                    cardholder = currentCardholder,
                    card = match.groups["card"]?.value ?: "",
                )
            )
        }
        return transactions
    }
}
