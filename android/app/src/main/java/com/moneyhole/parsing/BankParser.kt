package com.moneyhole.parsing

interface BankParser {
    val bank: String

    /** Returns true if the text extracted from the PDF belongs to this bank. */
    fun matches(text: String): Boolean

    /**
     * Extracts the reference month/year and the list of transactions from the
     * PDF text. pdfBytes/password are provided for parsers that need to
     * reopen the PDF with a different extraction (e.g. Itau's two-column
     * layout).
     */
    fun parse(text: String, pdfBytes: ByteArray, password: String): ParsedInvoice
}
