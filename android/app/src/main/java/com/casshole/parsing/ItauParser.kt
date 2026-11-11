package com.casshole.parsing

private val DUE_DATE_REGEX = Regex("Vencimento:\\s*(\\d{2})/(\\d{2})/(\\d{4})", RegexOption.IGNORE_CASE)
private val CARD_REGEX = Regex("Cart[aã]o\\s+\\d{4}\\.XXXX\\.XXXX\\.(\\d{4})", RegexOption.IGNORE_CASE)

// Ex: "30/12 SHOPEE *LOJAPI 07/07 65,58"
// Ex: "07/06 IFD*SORVETERIA EXEMPLO 51,88" (no installment)
// Ex: "16/06 PAGAMENTO -1.100,00" (payment, negative sign, must be ignored)
private val TRANSACTION_LINE_REGEX = Regex(
    "^(?<day>\\d{2})/(?<month>\\d{2})\\s+" +
        "(?<description>.+?)" +
        "(?:\\s+(?<currentInstallment>\\d{2})/(?<totalInstallments>\\d{2}))?" +
        "\\s+(?<sign>[-−])?(?:R\\$\\s*)?(?<amount>\\d{1,3}(?:\\.\\d{3})*,\\d{2})\\s*$"
)

private val CARDHOLDER_REGEX = Regex("^[A-ZÀ-Ú][A-ZÀ-Ú ]+$")

// Itaú opens one block per card inside "Lançamentos: compras e saques", the
// holder first and the additional cards after it, ex:
// "GUILHERME M MARTINS (final 2222)" ... "MARIA S MARTINS (final 3333)".
// Depending on the column extraction the "(final NNNN)" part can also land
// on its own line right after the name - both shapes are handled.
private val CARDHOLDER_WITH_CARD_REGEX = Regex(
    "^(?<name>[A-ZÀ-Ú][A-ZÀ-Ú ]+?)\\s*\\(?\\s*[Ff]inal\\s+(?<card>\\d{4})\\s*\\)?$",
)
private val CARD_ONLY_REGEX = Regex("^\\(?\\s*[Ff]inal\\s+(?<card>\\d{4})\\s*\\)?$")
// "Lançamentos no cartão (final 2222) 1.234,56" closes a card block - never a holder line.
private val CARD_BLOCK_TOTAL_REGEX = Regex("^lan[çc]amentos no cart[aã]o", RegexOption.IGNORE_CASE)

class ItauParser : BankParser {
    override val bank = "itau"

    override fun matches(text: String): Boolean {
        val textLower = text.lowercase()
        return textLower.contains("itau") || textLower.contains("itaú")
    }

    override fun parse(text: String, pdfBytes: ByteArray, password: String): ParsedInvoice {
        val (month, year) = extractReferenceMonthYear(text)
        val card = extractCard(text)
        val columnText = if (pdfBytes.isNotEmpty()) {
            extractPdfTextByColumns(pdfBytes, password, cutFraction = 0.57f)
        } else {
            text
        }
        val cardholder = extractCardholder(columnText)
        val transactions = extractTransactions(columnText, month, year, cardholder)
        return ParsedInvoice(bank = bank, card = card, referenceMonth = month, referenceYear = year, transactions = transactions)
    }

    private fun extractReferenceMonthYear(text: String): Pair<Int, Int> {
        val match = DUE_DATE_REGEX.find(text)
            ?: throw IllegalStateException("Não foi possível identificar o mês/ano de referência da fatura Itaú")
        return match.groupValues[2].toInt() to match.groupValues[3].toInt()
    }

    private fun extractCard(text: String): String =
        CARD_REGEX.find(text)?.groupValues?.get(1) ?: ""

    private fun extractCardholder(text: String): String {
        val header = Regex("Lançamentos:\\s*compras e saques", RegexOption.IGNORE_CASE).find(text) ?: return ""
        for (line in text.substring(header.range.last + 1).lines()) {
            val cleanLine = line.trim()
            CARDHOLDER_WITH_CARD_REGEX.matchEntire(cleanLine)?.let {
                return simpleTitleCase(it.groups["name"]!!.value.trim())
            }
            if (cleanLine.isNotEmpty() && CARDHOLDER_REGEX.matches(cleanLine)) {
                return simpleTitleCase(cleanLine)
            }
        }
        return ""
    }

    private fun extractCurrentEntriesSection(text: String): String {
        val start = Regex("Lançamentos:\\s*compras e saques", RegexOption.IGNORE_CASE).find(text) ?: return text
        val end = Regex("Total dos lançamentos atuais", RegexOption.IGNORE_CASE).find(text)
        val endPos = end?.range?.first ?: text.length
        return text.substring(start.range.first, endPos)
    }

    private fun extractCity(nextLine: String): String {
        val cleanLine = nextLine.trim()
        if (cleanLine.isEmpty() || TRANSACTION_LINE_REGEX.matchEntire(cleanLine) != null ||
            CARD_BLOCK_TOTAL_REGEX.containsMatchIn(cleanLine) ||
            CARDHOLDER_WITH_CARD_REGEX.matches(cleanLine) || CARD_ONLY_REGEX.matches(cleanLine)
        ) {
            return ""
        }
        val words = cleanLine.split(" ", limit = 2)
        if (words.size == 2 && words[0] == words[0].lowercase()) {
            return words[1].trim()
        }
        return cleanLine
    }

    private fun extractTransactions(
        text: String,
        referenceMonth: Int,
        referenceYear: Int,
        cardholder: String,
    ): List<ParsedTransaction> {
        val section = extractCurrentEntriesSection(text)
        val lines = section.lines()
        val transactions = mutableListOf<ParsedTransaction>()
        var currentCardholder = cardholder
        var currentCard = ""
        // Last all-caps line seen - becomes the holder if a lone "(final NNNN)" follows it.
        var pendingName: String? = null

        for (index in lines.indices) {
            val cleanLine = lines[index].trim()
            val match = TRANSACTION_LINE_REGEX.matchEntire(cleanLine)
            if (match == null) {
                if (cleanLine.isEmpty() || CARD_BLOCK_TOTAL_REGEX.containsMatchIn(cleanLine)) continue
                val withCard = CARDHOLDER_WITH_CARD_REGEX.matchEntire(cleanLine)
                val cardOnly = CARD_ONLY_REGEX.matchEntire(cleanLine)
                when {
                    withCard != null -> {
                        currentCardholder = simpleTitleCase(withCard.groups["name"]!!.value.trim())
                        currentCard = withCard.groups["card"]!!.value
                        pendingName = null
                    }
                    cardOnly != null -> {
                        pendingName?.let { currentCardholder = simpleTitleCase(it) }
                        currentCard = cardOnly.groups["card"]!!.value
                        pendingName = null
                    }
                    CARDHOLDER_REGEX.matches(cleanLine) -> pendingName = cleanLine
                }
                continue
            }
            pendingName = null
            if (match.groups["sign"] != null) continue

            val transactionMonth = match.groups["month"]!!.value.toInt()
            var transactionYear = referenceYear
            if (transactionMonth > referenceMonth) {
                transactionYear -= 1
            }

            val city = if (index + 1 < lines.size) extractCity(lines[index + 1]) else ""
            val day = match.groups["day"]!!.value.toInt()

            transactions.add(
                ParsedTransaction(
                    date = "%04d-%02d-%02d".format(transactionYear, transactionMonth, day),
                    description = match.groups["description"]!!.value.trim(),
                    amount = parseBrazilianAmount(match.groups["amount"]!!.value),
                    currentInstallment = match.groups["currentInstallment"]?.value?.toInt(),
                    totalInstallments = match.groups["totalInstallments"]?.value?.toInt(),
                    cardholder = currentCardholder,
                    city = city,
                    card = currentCard,
                )
            )
        }
        return transactions
    }
}

/** Equivalent to Python's str.title(): capitalizes the first letter of each word. */
private fun simpleTitleCase(text: String): String =
    text.lowercase().split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { it.uppercase() }
    }
