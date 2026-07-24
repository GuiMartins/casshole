package com.faturaapp.parsing

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Valida o motor de parsing Kotlin (PdfBox-Android) contra as mesmas faturas
 * reais e os mesmos numeros ja validados pelo parser Python original.
 */
@RunWith(AndroidJUnit4::class)
class FaturaDispatcherTest {

    private fun lerAsset(nome: String): ByteArray {
        val context = InstrumentationRegistry.getInstrumentation().context
        return context.assets.open(nome).use { it.readBytes() }
    }

    @Test
    fun nubank_bateComOGabaritoDoParserPython() {
        val fatura = FaturaDispatcher.processarFatura(lerAsset("nubank.pdf"))
        assertEquals("nubank", fatura.banco)
        assertEquals(7, fatura.mesReferencia)
        assertEquals(2026, fatura.anoReferencia)
        assertEquals(134, fatura.transacoes.size)
        assertEquals(1234.50, fatura.transacoes.sumOf { it.valor }, 0.01)
    }

    @Test
    fun itauMastercard_bateComOGabaritoDoParserPython() {
        val fatura = FaturaDispatcher.processarFatura(lerAsset("itau_mastercard.pdf"))
        assertEquals("itau", fatura.banco)
        assertEquals("2222", fatura.cartao)
        assertEquals(16, fatura.transacoes.size)
        assertEquals(2345.60, fatura.transacoes.sumOf { it.valor }, 0.01)
    }

    @Test
    fun itauVisa_bateComOGabaritoDoParserPython() {
        val fatura = FaturaDispatcher.processarFatura(lerAsset("itau_visa.pdf"))
        assertEquals("itau", fatura.banco)
        assertEquals("4444", fatura.cartao)
        assertEquals(1, fatura.transacoes.size)
        assertEquals(456.70, fatura.transacoes.sumOf { it.valor }, 0.01)
    }

    @Test
    fun mercadoPago_bateComOGabaritoDoParserPython() {
        val fatura = FaturaDispatcher.processarFatura(lerAsset("mercadopago.pdf"))
        assertEquals("mercadopago", fatura.banco)
        assertEquals("1111", fatura.cartao)
        assertEquals(41, fatura.transacoes.size)
        assertEquals(3456.70, fatura.transacoes.sumOf { it.valor }, 0.01)
    }

    @Test
    fun senhaErrada_lancaSenhaIncorretaException() {
        val bytes = lerAsset("itau_mastercard_protegido.pdf")
        assertThrows(SenhaIncorretaException::class.java) {
            FaturaDispatcher.processarFatura(bytes, listOf("0000-errada"))
        }
    }

    @Test
    fun senhaCorretaEntreVariasCandidatas_abreEExtraiCorretamente() {
        val bytes = lerAsset("itau_mastercard_protegido.pdf")
        // Simula o fluxo de "senhas padrao": tenta uma errada antes da certa.
        val fatura = FaturaDispatcher.processarFatura(bytes, listOf("0000-errada", "12345", "1234"))
        assertEquals("itau", fatura.banco)
        assertEquals("2222", fatura.cartao)
        assertEquals(16, fatura.transacoes.size)
        assertEquals(2345.60, fatura.transacoes.sumOf { it.valor }, 0.01)
    }
}
