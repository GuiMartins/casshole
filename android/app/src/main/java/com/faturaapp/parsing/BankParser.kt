package com.faturaapp.parsing

interface BankParser {
    val banco: String

    /** Retorna true se o texto extraido do PDF pertence a este banco. */
    fun matches(texto: String): Boolean

    /**
     * Extrai mes/ano de referencia e lista de transacoes do texto do PDF.
     * pdfBytes/senha sao fornecidos para parsers que precisam reabrir o PDF
     * com uma extracao diferente (ex: layout de duas colunas do Itau).
     */
    fun parse(texto: String, pdfBytes: ByteArray, senha: String): ParsedFatura
}
