package com.moneyhole.data.email

import com.sun.mail.imap.IMAPFolder
import java.util.Calendar
import java.util.Date
import java.util.Properties
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Part
import javax.mail.Session
import javax.mail.UIDFolder
import javax.mail.search.ComparisonTerm
import javax.mail.search.ReceivedDateTerm

class EmailAuthenticationException(message: String, cause: Throwable) : Exception(message, cause)
class EmailConnectionException(message: String, cause: Throwable) : Exception(message, cause)

data class FetchedAttachment(val fileName: String, val bytes: ByteArray, val receivedAt: Date)

/** Attachments found plus the highest IMAP UID seen, so the next fetch can resume from there. */
data class EmailFetchResult(val attachments: List<FetchedAttachment>, val lastUid: Long)

object EmailFetcher {

    /**
     * PDF attachments from messages newer than [lastProcessedUid] (an IMAP UID, exclusive).
     * When [lastProcessedUid] is 0 (never fetched before, or config just changed), falls back
     * to a bounded [sinceDays]-day window instead of scanning the whole mailbox history.
     */
    fun fetchPdfAttachments(
        credentials: EmailCredentials,
        lastProcessedUid: Long = 0,
        sinceDays: Int = 60,
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> },
    ): EmailFetchResult {
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
            val inbox = store.getFolder("INBOX") as IMAPFolder
            inbox.open(Folder.READ_ONLY)
            try {
                val messages = if (lastProcessedUid > 0) {
                    inbox.getMessagesByUID(lastProcessedUid + 1, UIDFolder.LASTUID)
                } else {
                    val since = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -sinceDays) }.time
                    inbox.search(ReceivedDateTerm(ComparisonTerm.GE, since))
                }

                val results = mutableListOf<FetchedAttachment>()
                var maxUid = lastProcessedUid
                onProgress(0, messages.size)
                for ((index, message) in messages.withIndex()) {
                    collectPdfAttachments(message, message.receivedDate ?: Date(), results)
                    val uid = inbox.getUID(message)
                    if (uid > maxUid) maxUid = uid
                    onProgress(index + 1, messages.size)
                }
                return EmailFetchResult(results, maxUid)
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
