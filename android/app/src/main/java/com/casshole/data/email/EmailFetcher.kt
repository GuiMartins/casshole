package com.casshole.data.email

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

/** [messageUid] is the IMAP UID of the message this attachment came from - lets the caller
 * figure out which messages actually got processed (vs. failed) for the next fetch's cursor. */
data class FetchedAttachment(val fileName: String, val bytes: ByteArray, val receivedAt: Date, val messageUid: Long)

/** Attachments found plus the highest IMAP UID among ALL messages examined (with or without a
 * PDF) - the caller decides how much of that to actually commit to as the next fetch's cursor,
 * since a message with a PDF that failed to import shouldn't be skipped next time. */
data class EmailFetchResult(val attachments: List<FetchedAttachment>, val highestUidSeen: Long)

object EmailFetcher {

    /**
     * PDF attachments from messages newer than [lastProcessedUid] (an IMAP UID, exclusive),
     * plus [retryUids] - specific older messages (already behind the cursor) whose PDF failed to
     * import last time and are worth trying again. When [lastProcessedUid] is 0 (never fetched
     * before, or config just changed), falls back to a bounded [sinceDays]-day window instead of
     * scanning the whole mailbox history.
     */
    fun fetchPdfAttachments(
        credentials: EmailCredentials,
        lastProcessedUid: Long = 0,
        retryUids: Set<Long> = emptySet(),
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
                val newMessages = if (lastProcessedUid > 0) {
                    inbox.getMessagesByUID(lastProcessedUid + 1, UIDFolder.LASTUID)
                } else {
                    val since = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -sinceDays) }.time
                    inbox.search(ReceivedDateTerm(ComparisonTerm.GE, since))
                }
                // Retry messages are already behind the cursor - fetched separately by exact UID
                // and kept out of the highestUidSeen calculation below, since they don't move the
                // cursor forward on their own.
                val retryMessages = if (retryUids.isNotEmpty()) {
                    inbox.getMessagesByUID(retryUids.toLongArray()).filterNotNull()
                } else {
                    emptyList()
                }

                val results = mutableListOf<FetchedAttachment>()
                var maxUid = lastProcessedUid
                val total = retryMessages.size + newMessages.size
                var processed = 0
                onProgress(0, total)

                for (message in retryMessages) {
                    val uid = inbox.getUID(message)
                    collectPdfAttachments(message, message.receivedDate ?: Date(), uid, results)
                    onProgress(++processed, total)
                }
                for (message in newMessages) {
                    val uid = inbox.getUID(message)
                    collectPdfAttachments(message, message.receivedDate ?: Date(), uid, results)
                    if (uid > maxUid) maxUid = uid
                    onProgress(++processed, total)
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
    internal fun collectPdfAttachments(
        part: Part,
        receivedAt: Date,
        messageUid: Long,
        results: MutableList<FetchedAttachment>,
    ) {
        if (part.isMimeType("multipart/*")) {
            val multipart = part.content as Multipart
            for (i in 0 until multipart.count) {
                collectPdfAttachments(multipart.getBodyPart(i), receivedAt, messageUid, results)
            }
            return
        }

        val fileName = part.fileName
        val looksLikeAttachment = Part.ATTACHMENT.equals(part.disposition, ignoreCase = true) || fileName != null
        if (looksLikeAttachment && fileName != null && fileName.endsWith(".pdf", ignoreCase = true)) {
            val bytes = part.inputStream.use { it.readBytes() }
            results.add(FetchedAttachment(fileName, bytes, receivedAt, messageUid))
        }
    }
}
