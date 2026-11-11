package com.casshole.parsing

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Synthetic invoice text (no real data) checking that transactions are tagged
 * with the cardholder of the block they're in - account holder vs. additional
 * cards. An empty pdfBytes makes the parsers work on the given text directly.
 */
class CardholderParsingTest {

    @Test
    fun itau_splitsTransactionsByCardBlock() {
        val text = """
            Itaú Cartões
            Vencimento: 10/07/2026
            Cartão 5555.XXXX.XXXX.2222
            Lançamentos: compras e saques
            GUILHERME M MARTINS (final 2222)
            02/06 PADARIA EXEMPLO 25,00
            SAO PAULO
            05/06 LOJA EXEMPLO 03/10 100,00
            Lançamentos no cartão (final 2222) 125,00
            MARIA S MARTINS (final 3333)
            07/06 FARMACIA EXEMPLO 40,00
            CAMPINAS
            Lançamentos no cartão (final 3333) 40,00
            Total dos lançamentos atuais 165,00
        """.trimIndent()

        val invoice = ItauParser().parse(text, ByteArray(0), "")

        assertEquals("2222", invoice.card)
        assertEquals(
            listOf("Guilherme M Martins", "Guilherme M Martins", "Maria S Martins"),
            invoice.transactions.map { it.cardholder },
        )
        assertEquals(listOf("2222", "2222", "3333"), invoice.transactions.map { it.card })
        assertEquals("CAMPINAS", invoice.transactions.last().city)
        assertEquals("SAO PAULO", invoice.transactions.first().city)
        assertEquals("", invoice.transactions[1].city)
    }

    @Test
    fun itau_handlesTheFinalDigitsOnTheLineAfterTheName() {
        val text = """
            Itaú Cartões
            Vencimento: 10/07/2026
            Lançamentos: compras e saques
            GUILHERME M MARTINS
            (final 2222)
            02/06 PADARIA EXEMPLO 25,00
            SAO PAULO
            MARIA S MARTINS
            (final 3333)
            07/06 FARMACIA EXEMPLO 40,00
            Total dos lançamentos atuais 65,00
        """.trimIndent()

        val invoice = ItauParser().parse(text, ByteArray(0), "")

        assertEquals(listOf("Guilherme M Martins", "Maria S Martins"), invoice.transactions.map { it.cardholder })
        assertEquals(listOf("2222", "3333"), invoice.transactions.map { it.card })
    }

    @Test
    fun itau_singleCardholderWithoutCardBlocksKeepsTheOldBehavior() {
        val text = """
            Itaú Cartões
            Vencimento: 10/07/2026
            Lançamentos: compras e saques
            GUILHERME M MARTINS
            02/06 PADARIA EXEMPLO 25,00
            SAO PAULO
            03/06 MERCADO EXEMPLO 10,00
            Total dos lançamentos atuais 35,00
        """.trimIndent()

        val invoice = ItauParser().parse(text, ByteArray(0), "")

        assertEquals(listOf("Guilherme M Martins", "Guilherme M Martins"), invoice.transactions.map { it.cardholder })
        assertEquals(listOf("", ""), invoice.transactions.map { it.card })
    }

    @Test
    fun nubank_tagsEachSectionWithItsCardholder() {
        val text = """
            Nubank
            Data de vencimento: 10 JUL 2026
            Guilherme M Martins R$ 125,00
            16 JUN •••• 2222 Padaria Exemplo R$ 25,00
            17 JUN •••• 2222 Loja Exemplo - Parcela 3/10 R$ 100,00
            Compras de Maria S Martins R$ 40,00
            18 JUN •••• 3333 Farmacia Exemplo R$ 40,00
        """.trimIndent()

        val invoice = NubankParser().parse(text, ByteArray(0), "")

        assertEquals(
            listOf("Guilherme M Martins", "Guilherme M Martins", "Maria S Martins"),
            invoice.transactions.map { it.cardholder },
        )
        assertEquals(listOf("2222", "2222", "3333"), invoice.transactions.map { it.card })
    }
}
