package com.moneyhole.data.local

import android.content.Context
import androidx.room.withTransaction
import com.moneyhole.R
import com.moneyhole.categorizer.categorize
import com.moneyhole.data.local.entity.CardNicknameEntity
import com.moneyhole.data.local.entity.CategoryOverrideEntity
import com.moneyhole.data.local.entity.InvoiceEntity
import com.moneyhole.data.local.entity.DefaultPasswordEntity
import com.moneyhole.data.local.entity.TransactionEntity
import com.moneyhole.parsing.InvoiceDispatcher
import java.security.MessageDigest
import java.time.Instant

class DuplicateFileException(message: String) : Exception(message)
class DuplicatePeriodException(message: String) : Exception(message)

/** Normalizes a description for comparison, tolerant to case/whitespace. */
private fun normalizeDescription(description: String): String = description.trim().uppercase()

/**
 * [database] defaults to the app-wide singleton but can be overridden — the
 * hook that lets tests pass an in-memory Room database instead of touching
 * the real on-disk one.
 */
class InvoiceRepository(
    private val context: Context,
    database: AppDatabase = DatabaseProvider.getDatabase(context),
) {
    private val db = database
    private val invoiceDao = db.invoiceDao()
    private val transactionDao = db.transactionDao()
    private val defaultPasswordDao = db.defaultPasswordDao()
    private val categoryOverrideDao = db.categoryOverrideDao()
    private val cardNicknameDao = db.cardNicknameDao()

    suspend fun hasDefaultPasswordsRegistered(): Boolean = defaultPasswordDao.list().isNotEmpty()

    suspend fun listInvoices(): List<InvoiceWithTransactions> = invoiceDao.listWithTransactions()

    suspend fun getInvoice(id: Long): InvoiceWithTransactions? = invoiceDao.getWithTransactions(id)

    suspend fun listCardNicknames(): List<CardNicknameEntity> = cardNicknameDao.list()

    /** Setting a blank nickname removes it - there's no separate "clear" action in the UI. */
    suspend fun setCardNickname(bank: String, card: String, nickname: String) {
        val trimmed = nickname.trim()
        if (trimmed.isBlank()) {
            cardNicknameDao.remove(bank, card)
        } else {
            cardNicknameDao.save(CardNicknameEntity(bank = bank, card = card, nickname = trimmed))
        }
    }

    suspend fun listCategoryOverrides(): List<CategoryOverrideEntity> = categoryOverrideDao.list()

    suspend fun removeCategoryOverride(id: Long) {
        categoryOverrideDao.remove(id)
    }

    /**
     * Updates a transaction's category and "remembers" that correction: next
     * time a transaction with the same description shows up in a new
     * invoice, it already comes categorized the same way automatically.
     */
    suspend fun updateCategory(transactionId: Long, category: String) {
        transactionDao.updateCategory(transactionId, category)
        val transaction = transactionDao.findById(transactionId) ?: return
        categoryOverrideDao.save(
            CategoryOverrideEntity(description = normalizeDescription(transaction.description), category = category)
        )
    }

    suspend fun listDefaultPasswords(): List<DefaultPasswordEntity> = defaultPasswordDao.list()

    suspend fun addDefaultPassword(value: String): DefaultPasswordEntity {
        val id = defaultPasswordDao.insert(DefaultPasswordEntity(value = value))
        return DefaultPasswordEntity(id = id, value = value)
    }

    suspend fun removeDefaultPassword(id: Long) {
        defaultPasswordDao.remove(id)
    }

    suspend fun transactionsByPeriod(month: Int, year: Int): List<TransactionEntity> =
        transactionDao.listByPeriod(month, year)

    suspend fun processAndStore(bytes: ByteArray, enteredPassword: String?): InvoiceWithTransactions {
        val hash = calculateHash(bytes)
        if (invoiceDao.findByHash(hash) != null) {
            throw DuplicateFileException(context.getString(R.string.error_invoice_already_processed))
        }

        val defaultPasswords = defaultPasswordDao.list().map { it.value }
        val cleanedEnteredPassword = enteredPassword?.trim()?.ifBlank { null }
        val candidates = listOfNotNull(cleanedEnteredPassword) + defaultPasswords
        val parsedInvoice = InvoiceDispatcher.processInvoice(bytes, candidates)

        val existingInvoice = invoiceDao.findByPeriod(
            parsedInvoice.bank,
            parsedInvoice.card,
            parsedInvoice.referenceMonth,
            parsedInvoice.referenceYear,
        )
        if (existingInvoice != null) {
            val cardDetail = if (parsedInvoice.card.isNotBlank()) {
                context.getString(R.string.error_invoice_duplicate_period_card_detail, parsedInvoice.card)
            } else {
                ""
            }
            throw DuplicatePeriodException(
                context.getString(
                    R.string.error_invoice_duplicate_period,
                    parsedInvoice.bank, cardDetail, parsedInvoice.referenceMonth, parsedInvoice.referenceYear,
                )
            )
        }

        val overrides = categoryOverrideDao.list().associate { it.description to it.category }

        val invoiceId = db.withTransaction {
            val id = invoiceDao.insert(
                InvoiceEntity(
                    bank = parsedInvoice.bank,
                    card = parsedInvoice.card,
                    referenceMonth = parsedInvoice.referenceMonth,
                    referenceYear = parsedInvoice.referenceYear,
                    fileHash = hash,
                    processedAt = Instant.now().toString(),
                )
            )
            transactionDao.insertAll(
                parsedInvoice.transactions.map { transaction ->
                    val category = overrides[normalizeDescription(transaction.description)]
                        ?: categorize(transaction.description)
                    TransactionEntity(
                        invoiceId = id,
                        date = transaction.date,
                        description = transaction.description,
                        amount = transaction.amount,
                        category = category,
                        currentInstallment = transaction.currentInstallment,
                        totalInstallments = transaction.totalInstallments,
                        cardholder = transaction.cardholder,
                        city = transaction.city,
                        card = transaction.card,
                    )
                }
            )
            id
        }

        return invoiceDao.getWithTransactions(invoiceId)!!
    }

    private fun calculateHash(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
