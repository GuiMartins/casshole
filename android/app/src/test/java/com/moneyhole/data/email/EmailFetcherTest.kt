package com.moneyhole.data.email

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.Properties
import javax.mail.Session
import javax.mail.internet.MimeMessage

class EmailFetcherTest {

    private fun mimeMessage(vararg parts: Pair<String, Pair<String, ByteArray>>): MimeMessage {
        val boundary = "BOUNDARY"
        val raw = buildString {
            append("From: sender@example.com\r\n")
            append("To: receiver@example.com\r\n")
            append("Subject: Test\r\n")
            append("MIME-Version: 1.0\r\n")
            append("Content-Type: multipart/mixed; boundary=\"$boundary\"\r\n\r\n")
            append("--$boundary\r\n")
            append("Content-Type: text/plain\r\n\r\n")
            append("body text\r\n")
            for ((fileName, contentTypeAndBytes) in parts) {
                val (contentType, bytes) = contentTypeAndBytes
                append("--$boundary\r\n")
                append("Content-Type: $contentType\r\n")
                append("Content-Disposition: attachment; filename=\"$fileName\"\r\n")
                append("Content-Transfer-Encoding: base64\r\n\r\n")
                append(java.util.Base64.getMimeEncoder().encodeToString(bytes))
                append("\r\n")
            }
            append("--$boundary--\r\n")
        }
        val session = Session.getDefaultInstance(Properties())
        return MimeMessage(session, raw.byteInputStream(Charsets.US_ASCII))
    }

    @Test
    fun `collects a pdf attachment from a multipart message`() {
        val pdfBytes = "fake pdf content".toByteArray()
        val message = mimeMessage("invoice.pdf" to ("application/pdf" to pdfBytes))

        val results = mutableListOf<FetchedAttachment>()
        EmailFetcher.collectPdfAttachments(message, Date(), results)

        assertEquals(1, results.size)
        assertEquals("invoice.pdf", results[0].fileName)
        assertTrue(pdfBytes.contentEquals(results[0].bytes))
    }

    @Test
    fun `ignores non-pdf attachments`() {
        val message = mimeMessage("notes.txt" to ("text/plain" to "hello".toByteArray()))

        val results = mutableListOf<FetchedAttachment>()
        EmailFetcher.collectPdfAttachments(message, Date(), results)

        assertEquals(0, results.size)
    }

    @Test
    fun `collects only the pdf out of multiple mixed attachments`() {
        val pdfBytes = "pdf-bytes".toByteArray()
        val message = mimeMessage(
            "receipt.txt" to ("text/plain" to "hi".toByteArray()),
            "invoice.pdf" to ("application/pdf" to pdfBytes),
            "logo.png" to ("image/png" to "png-bytes".toByteArray()),
        )

        val results = mutableListOf<FetchedAttachment>()
        EmailFetcher.collectPdfAttachments(message, Date(), results)

        assertEquals(1, results.size)
        assertEquals("invoice.pdf", results[0].fileName)
    }
}
