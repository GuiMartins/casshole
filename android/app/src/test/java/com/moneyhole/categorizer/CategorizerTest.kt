package com.moneyhole.categorizer

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Cases coming from real descriptions in the 4 invoices already used
 * throughout the project (Nubank, Itau Mastercard, Itau Visa, Mercado Pago)
 * that were falling into "Outros" for lack of a keyword, or worse, falling
 * into the wrong category due to a substring collision (Hospital das
 * Bonecas).
 */
class CategorizerTest {

    @Test
    fun `does not confuse a doll repair shop with a real hospital`() {
        assertEquals("Outros", categorize("Hospital das Bonecas"))
    }

    @Test
    fun `still recognizes a real hospital`() {
        assertEquals("Saúde", categorize("Hospital Sao Lucas"))
    }

    @Test
    fun `known pharmacies go to Saude`() {
        assertEquals("Saúde", categorize("Raia2674"))
        assertEquals("Saúde", categorize("Drogarias Pacheco"))
        assertEquals("Saúde", categorize("Drogariamodernafl"))
    }

    @Test
    fun `mercearia goes to Alimentacao`() {
        assertEquals("Alimentação", categorize("Mercearia Esperanca"))
    }

    @Test
    fun `sweets ice cream and salty snacks go to Alimentacao`() {
        assertEquals("Alimentação", categorize("Novilha Top Gelados"))
        assertEquals("Alimentação", categorize("Maria Torta"))
        assertEquals("Alimentação", categorize("Croc Salgaderia"))
        assertEquals("Alimentação", categorize("BOTECO DO DAIRIO DE JAN"))
    }

    @Test
    fun `pet shops go to the Pets category`() {
        assertEquals("Pets", categorize("Green Pet Shop"))
        assertEquals("Pets", categorize("PETSUPERMARK*O"))
    }

    @Test
    fun `specific retail stores go to Compras`() {
        assertEquals("Compras", categorize("Don Pablo Papelaria"))
        assertEquals("Compras", categorize("Mls 142 Calcados"))
        assertEquals("Compras", categorize("Dakotton Confeccoes"))
        assertEquals("Compras", categorize("Outlet H Estevo"))
        assertEquals("Compras", categorize("Fama Biju"))
        assertEquals("Compras", categorize("So Tintas-Sempre A"))
        assertEquals("Compras", categorize("Unitintas Tintas e Mat"))
        assertEquals("Compras", categorize("Socartucho"))
        assertEquals("Compras", categorize("LG ELECTRONICS"))
        assertEquals("Compras", categorize("SAMSUNG *I"))
    }

    @Test
    fun `tire shop goes to Transporte`() {
        assertEquals("Transporte", categorize("Padok Pneus"))
    }

    @Test
    fun `wellhub goes to Streaming-Assinaturas`() {
        assertEquals("Streaming/Assinaturas", categorize("Wellhub Fulana Silva"))
    }

    @Test
    fun `mercado livre still matches before the generic mercado`() {
        assertEquals("Compras", categorize("MERCADOLIVRE*MERCADOLIVRE"))
        assertEquals("Alimentação", categorize("Mercado Extra"))
    }

    @Test
    fun `description with no matching keyword falls into Outros`() {
        assertEquals("Outros", categorize("Barbaramagalhaes"))
    }
}
