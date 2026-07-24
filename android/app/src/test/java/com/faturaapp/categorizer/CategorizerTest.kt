package com.faturaapp.categorizer

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Casos vindos de descricoes reais das 4 faturas ja usadas no projeto
 * inteiro (Nubank, Itau Mastercard, Itau Visa, Mercado Pago) que estavam
 * caindo em "Outros" por falta de palavra-chave, ou pior, caindo na
 * categoria errada por colisao de substring (Hospital das Bonecas).
 */
class CategorizerTest {

    @Test
    fun `nao confunde loja de reparo de bonecas com hospital de verdade`() {
        assertEquals("Outros", categorizar("Hospital das Bonecas"))
    }

    @Test
    fun `ainda reconhece hospital de verdade`() {
        assertEquals("Saúde", categorizar("Hospital Sao Lucas"))
    }

    @Test
    fun `farmacias conhecidas vao pra Saude`() {
        assertEquals("Saúde", categorizar("Raia2674"))
        assertEquals("Saúde", categorizar("Drogarias Pacheco"))
        assertEquals("Saúde", categorizar("Drogariamodernafl"))
    }

    @Test
    fun `mercearia vai pra Alimentacao`() {
        assertEquals("Alimentação", categorizar("Mercearia Esperanca"))
    }

    @Test
    fun `doces sorvetes e salgados vao pra Alimentacao`() {
        assertEquals("Alimentação", categorizar("Novilha Top Gelados"))
        assertEquals("Alimentação", categorizar("Maria Torta"))
        assertEquals("Alimentação", categorizar("Croc Salgaderia"))
        assertEquals("Alimentação", categorizar("BOTECO DO DAIRIO DE JAN"))
    }

    @Test
    fun `pet shops vao pra categoria Pets`() {
        assertEquals("Pets", categorizar("Green Pet Shop"))
        assertEquals("Pets", categorizar("PETSUPERMARK*O"))
    }

    @Test
    fun `lojas de varejo especificas vao pra Compras`() {
        assertEquals("Compras", categorizar("Don Pablo Papelaria"))
        assertEquals("Compras", categorizar("Mls 142 Calcados"))
        assertEquals("Compras", categorizar("Dakotton Confeccoes"))
        assertEquals("Compras", categorizar("Outlet H Estevo"))
        assertEquals("Compras", categorizar("Fama Biju"))
        assertEquals("Compras", categorizar("So Tintas-Sempre A"))
        assertEquals("Compras", categorizar("Unitintas Tintas e Mat"))
        assertEquals("Compras", categorizar("Socartucho"))
        assertEquals("Compras", categorizar("LG ELECTRONICS"))
        assertEquals("Compras", categorizar("SAMSUNG *I"))
    }

    @Test
    fun `pneu vai pra Transporte`() {
        assertEquals("Transporte", categorizar("Padok Pneus"))
    }

    @Test
    fun `wellhub vai pra Streaming-Assinaturas`() {
        assertEquals("Streaming/Assinaturas", categorizar("Wellhub Fulana Silva"))
    }

    @Test
    fun `mercado livre continua batendo antes do mercado generico`() {
        assertEquals("Compras", categorizar("MERCADOLIVRE*MERCADOLIVRE"))
        assertEquals("Alimentação", categorizar("Mercado Extra"))
    }

    @Test
    fun `descricao sem nenhuma palavra-chave cai em Outros`() {
        assertEquals("Outros", categorizar("Barbaramagalhaes"))
    }
}
