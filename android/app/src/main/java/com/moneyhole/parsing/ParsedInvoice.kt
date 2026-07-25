package com.moneyhole.parsing

data class ParsedInvoice(
    val bank: String,
    val referenceMonth: Int,
    val referenceYear: Int,
    val transactions: List<ParsedTransaction>,
    val card: String = "",
)
