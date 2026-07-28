package com.moneyhole.parsing

import android.util.Base64
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates the Kotlin parsing engine (PdfBox-Android) against the same real
 * invoices and the same numbers already validated by the original Python
 * parser.
 */
@RunWith(AndroidJUnit4::class)
class InvoiceDispatcherTest {

    private fun readAsset(name: String): ByteArray {
        val context = InstrumentationRegistry.getInstrumentation().context
        return context.assets.open(name).use { it.readBytes() }
    }

    @Test
    fun nubank_matchesThePythonParserBaseline() {
        val invoice = InvoiceDispatcher.processInvoice(readAsset("nubank.pdf"))
        assertEquals("nubank", invoice.bank)
        assertEquals(7, invoice.referenceMonth)
        assertEquals(2026, invoice.referenceYear)
        assertEquals(134, invoice.transactions.size)
        assertEquals(1234.50, invoice.transactions.sumOf { it.amount }, 0.01)
    }

    @Test
    fun itauMastercard_matchesThePythonParserBaseline() {
        val invoice = InvoiceDispatcher.processInvoice(readAsset("itau_mastercard.pdf"))
        assertEquals("itau", invoice.bank)
        assertEquals("2222", invoice.card)
        assertEquals(16, invoice.transactions.size)
        assertEquals(2345.60, invoice.transactions.sumOf { it.amount }, 0.01)
    }

    @Test
    fun itauVisa_matchesThePythonParserBaseline() {
        val invoice = InvoiceDispatcher.processInvoice(readAsset("itau_visa.pdf"))
        assertEquals("itau", invoice.bank)
        assertEquals("4444", invoice.card)
        assertEquals(1, invoice.transactions.size)
        assertEquals(456.70, invoice.transactions.sumOf { it.amount }, 0.01)
    }

    @Test
    fun mercadoPago_matchesThePythonParserBaseline() {
        val invoice = InvoiceDispatcher.processInvoice(readAsset("mercadopago.pdf"))
        assertEquals("mercadopago", invoice.bank)
        assertEquals("1111", invoice.card)
        assertEquals(41, invoice.transactions.size)
        assertEquals(3456.70, invoice.transactions.sumOf { it.amount }, 0.01)
    }

    @Test
    fun wrongPassword_throwsIncorrectPasswordException() {
        val bytes = readAsset("itau_mastercard_protegido.pdf")
        assertThrows(IncorrectPasswordException::class.java) {
            InvoiceDispatcher.processInvoice(bytes, listOf("0000-errada"))
        }
    }

    @Test
    fun correctPasswordAmongSeveralCandidates_opensAndExtractsCorrectly() {
        val bytes = readAsset("itau_mastercard_protegido.pdf")
        // Simulates the "default passwords" flow: tries a wrong one before the right one.
        val invoice = InvoiceDispatcher.processInvoice(bytes, listOf("0000-errada", "12345", "1234"))
        assertEquals("itau", invoice.bank)
        assertEquals("2222", invoice.card)
        assertEquals(16, invoice.transactions.size)
        assertEquals(2345.60, invoice.transactions.sumOf { it.amount }, 0.01)
    }

    // Minimal synthetic (non-real) single-page PDFs, embedded as base64 so this test needs no
    // asset file - unlike the fixtures above, there's no real invoice data to keep out of git here.
    private fun pdfFromBase64(base64: String): ByteArray = Base64.decode(base64, Base64.DEFAULT)

    @Test
    fun receiptWithNoInvoiceWording_throwsNotABankInvoiceException() {
        // "Recibo de compra na Loja Exemplo. Obrigado pela preferencia. Total pago: R$ 45,00."
        val bytes = pdfFromBase64(
            "JVBERi0xLjQKMSAwIG9iago8PCAvVHlwZSAvQ2F0YWxvZyAvUGFnZXMgMiAwIFIgPj4KZW5kb2JqCjIgMCBvYmoKPDwgL1R5cGUgL1BhZ2VzIC9LaWRzIFszIDAgUl0gL0NvdW50IDEgPj4KZW5kb2JqCjMgMCBvYmoKPDwgL1R5cGUgL1BhZ2UgL1BhcmVudCAyIDAgUiAvUmVzb3VyY2VzIDw8IC9Gb250IDw8IC9GMSA0IDAgUiA+PiA+PiAvTWVkaWFCb3ggWzAgMCA2MTIgNzkyXSAvQ29udGVudHMgNSAwIFIgPj4KZW5kb2JqCjQgMCBvYmoKPDwgL1R5cGUgL0ZvbnQgL1N1YnR5cGUgL1R5cGUxIC9CYXNlRm9udCAvSGVsdmV0aWNhID4+CmVuZG9iago1IDAgb2JqCjw8IC9MZW5ndGggMTEzID4+CnN0cmVhbQpCVCAvRjEgMTIgVGYgNzIgNzIwIFRkIChSZWNpYm8gZGUgY29tcHJhIG5hIExvamEgRXhlbXBsby4gT2JyaWdhZG8gcGVsYSBwcmVmZXJlbmNpYS4gVG90YWwgcGFnbzogUiQgNDUsMDAuKSBUaiBFVAplbmRzdHJlYW0KZW5kb2JqCnhyZWYKMCA2CjAwMDAwMDAwMDAgNjU1MzUgZiAKMDAwMDAwMDAwOSAwMDAwMCBuIAowMDAwMDAwMDU4IDAwMDAwIG4gCjAwMDAwMDAxMTUgMDAwMDAgbiAKMDAwMDAwMDI0MSAwMDAwMCBuIAowMDAwMDAwMzExIDAwMDAwIG4gCnRyYWlsZXIKPDwgL1NpemUgNiAvUm9vdCAxIDAgUiA+PgpzdGFydHhyZWYKNDc1CiUlRU9GCg=="
        )
        assertThrows(NotABankInvoiceException::class.java) {
            InvoiceDispatcher.processInvoice(bytes)
        }
    }

    @Test
    fun invoiceFromKnownUnsupportedBank_throwsUnsupportedBankExceptionWithBankName() {
        // "Fatura do cartao de credito. Vencimento 10/08/2026. Banco Bradesco S.A. ..."
        val bytes = pdfFromBase64(
            "JVBERi0xLjQKMSAwIG9iago8PCAvVHlwZSAvQ2F0YWxvZyAvUGFnZXMgMiAwIFIgPj4KZW5kb2JqCjIgMCBvYmoKPDwgL1R5cGUgL1BhZ2VzIC9LaWRzIFszIDAgUl0gL0NvdW50IDEgPj4KZW5kb2JqCjMgMCBvYmoKPDwgL1R5cGUgL1BhZ2UgL1BhcmVudCAyIDAgUiAvUmVzb3VyY2VzIDw8IC9Gb250IDw8IC9GMSA0IDAgUiA+PiA+PiAvTWVkaWFCb3ggWzAgMCA2MTIgNzkyXSAvQ29udGVudHMgNSAwIFIgPj4KZW5kb2JqCjQgMCBvYmoKPDwgL1R5cGUgL0ZvbnQgL1N1YnR5cGUgL1R5cGUxIC9CYXNlRm9udCAvSGVsdmV0aWNhID4+CmVuZG9iago1IDAgb2JqCjw8IC9MZW5ndGggMTMyID4+CnN0cmVhbQpCVCAvRjEgMTIgVGYgNzIgNzIwIFRkIChGYXR1cmEgZG8gY2FydGFvIGRlIGNyZWRpdG8uIFZlbmNpbWVudG8gMTAvMDgvMjAyNi4gQmFuY28gQnJhZGVzY28gUy5BLiBMaW1pdGUgZGUgY3JlZGl0byBkaXNwb25pdmVsLikgVGogRVQKZW5kc3RyZWFtCmVuZG9iagp4cmVmCjAgNgowMDAwMDAwMDAwIDY1NTM1IGYgCjAwMDAwMDAwMDkgMDAwMDAgbiAKMDAwMDAwMDA1OCAwMDAwMCBuIAowMDAwMDAwMTE1IDAwMDAwIG4gCjAwMDAwMDAyNDEgMDAwMDAgbiAKMDAwMDAwMDMxMSAwMDAwMCBuIAp0cmFpbGVyCjw8IC9TaXplIDYgL1Jvb3QgMSAwIFIgPj4Kc3RhcnR4cmVmCjQ5NAolJUVPRgo="
        )
        val exception = assertThrows(UnsupportedBankException::class.java) {
            InvoiceDispatcher.processInvoice(bytes)
        }
        assertEquals("bradesco", exception.bankName)
    }
}
