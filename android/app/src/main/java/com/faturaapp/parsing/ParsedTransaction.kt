package com.faturaapp.parsing

data class ParsedTransaction(
    val date: String,
    val description: String,
    val amount: Double,
    val currentInstallment: Int? = null,
    val totalInstallments: Int? = null,
    val cardholder: String = "",
    val city: String = "",
    val card: String = "",
)
