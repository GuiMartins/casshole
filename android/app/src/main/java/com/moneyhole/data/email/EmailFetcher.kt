package com.moneyhole.data.email

import java.util.Calendar
import java.util.Date
import java.util.Properties
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.search.ComparisonTerm
import javax.mail.search.ReceivedDateTerm

class EmailAuthenticationException(message: String, cause: Throwable) : Exception(message, cause)
class EmailConnectionException(message: String, cause: Throwable) : Exception(message, cause)

data class FetchedAttachment(val fileName: String, val bytes: ByteArray, val receivedAt: Date)

object EmailFetcher {

    /** Every PDF attachment found on messages received in the last [sinceDays] days. */
    fun fetchPdfAttachments(
        credentials: EmailCredentials,
        sinceDays: Int = 60,
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> },
    ): List<FetchedAttachment> {
        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", credentials.imapHost)
            put("mail.imaps.port", credentials.imapPort.toString())
            put("mail.imaps.ssl.trust", credentials.imapHost)
            put("mail.imaps.connectiontimeout", "15000")
            put("mail.imaps.timeout", "15000")
        }
        val session = Session.getInstance(props)
        val store = session.getStore("imaps")

        try {
            store.connect(credentials.imapHost, credentials.address, credentials.appPassword)
        } catch (e: javax.mail.AuthenticationFailedException) {
            throw EmailAuthenticationException("Falha de autenticação - confira o e-mail e a senha de app", e)
        } catch (e: Exception) {
            throw EmailConnectionException("Não foi possível conectar ao servidor IMAP", e)
        }

        try {
            val inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_ONLY)
            try {
                val since = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -sinceDays) }.time
                val messages = inbox.search(ReceivedDateTerm(ComparisonTerm.GE, since))

                val results = mutableListOf<FetchedAttachment>()
                onProgress(0, messages.size)
                for ((index, message) in messages.withIndex()) {
                    collectPdfAttachments(message, message.receivedDate ?: Date(), results)
                    onProgress(index + 1, messages.size)
                }
                return results
            } finally {
                inbox.close(false)
            }
        } finally {
            store.close()
        }
    }

    /** Walks a message's MIME tree (recursing into nested multiparts) collecting PDF attachments. */
    internal fun collectPdfAttachments(part: Part, receivedAt: Date, results: MutableList<FetchedAttachment>) {
        if (part.isMimeType("multipart/*")) {
            val multipart = part.content as Multipart
            for (i in 0 until multipart.count) {
                collectPdfAttachments(multipart.getBodyPart(i), receivedAt, results)
            }
            return
        }

        val fileName = part.fileName
        val looksLikeAttachment = Part.ATTACHMENT.equals(part.disposition, ignoreCase = true) || fileName != null
        if (looksLikeAttachment && fileName != null && fileName.endsWith(".pdf", ignoreCase = true)) {
            val bytes = part.inputStream.use { it.readBytes() }
            results.add(FetchedAttachment(fileName, bytes, receivedAt))
        }
    }
}
