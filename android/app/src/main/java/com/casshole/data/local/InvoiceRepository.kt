package com.casshole.data.local

import android.content.Context
import androidx.room.withTransaction
import com.casshole.R
import com.casshole.categorizer.categorize
import com.casshole.data.local.entity.CardNicknameEntity
import com.casshole.data.local.entity.CategoryOverrideEntity
import com.casshole.data.local.entity.InvoiceEntity
import com.casshole.data.local.entity.DefaultPasswordEntity
import com.casshole.data.local.entity.TransactionEntity
import com.casshole.parsing.InvoiceDispatcher
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
     * How many stored transactions share a description - i.e. how many rows a
     * retroactive correction would touch. Matching happens in Kotlin, not
     * SQL: SQLite's UPPER() only folds ASCII, so "Açaí" would never match the
     * normalized "AÇAÍ" we store in the overrides table.
     */
    suspend fun countTransactionsWithDescription(description: String): Int =
        countTransactionsByDescription()[normalizeDescription(description)] ?: 0

    /** Same counting, for every normalized description at once - one pass for the whole overrides list. */
    suspend fun countTransactionsByDescription(): Map<String, Int> =
        transactionDao.listAll().groupingBy { normalizeDescription(it.description) }.eachCount()

    /**
     * Updates a transaction's category and "remembers" that correction: next
     * time a transaction with the same description shows up in a new
     * invoice, it already comes categorized the same way automatically.
     *
     * With [applyToPast], the correction is also retroactive - every
     * transaction already stored with the same description (past invoices
     * included) is recategorized too, instead of only the edited one.
     */
    suspend fun updateCategory(transactionId: Long, category: String, applyToPast: Boolean = false) {
        val transaction = transactionDao.findById(transactionId) ?: return
        if (applyToPast) {
            applyCategoryToDescription(transaction.description, category)
        } else {
            transactionDao.updateCategory(transactionId, category)
            categoryOverrideDao.save(
                CategoryOverrideEntity(description = normalizeDescription(transaction.description), category = category)
            )
        }
    }

    /**
     * Recategorizes every stored transaction with this description and stores
     * the override, so future invoices follow it too. Returns how many
     * transactions were updated.
     */
    suspend fun applyCategoryToDescription(description: String, category: String): Int {
        val normalized = normalizeDescription(description)
        val ids = transactionIdsWithDescription(normalized)
        db.withTransaction {
            // Chunked because SQLite caps the number of bound variables in a
            // single statement (999 on older Android versions).
            ids.chunked(500).forEach { chunk -> transactionDao.updateCategoryForIds(chunk, category) }
            categoryOverrideDao.save(CategoryOverrideEntity(description = normalized, category = category))
        }
        return ids.size
    }

    /**
     * Re-runs the categorization rules over every stored transaction, used
     * when the rules themselves change (e.g. food delivery split out of
     * "Alimentação") so past invoices don't keep the old bucket forever.
     *
     * Descriptions the user has corrected by hand are left alone - a manual
     * override always outranks a regex. Returns how many rows changed.
     */
    suspend fun recategorizeStoredTransactions(): Int {
        val overriddenDescriptions = categoryOverrideDao.list().map { it.description }.toSet()
        val newCategoryById = transactionDao.listAll()
            .filter { normalizeDescription(it.description) !in overriddenDescriptions }
            .mapNotNull { transaction ->
                val category = categorize(transaction.description)
                if (category == transaction.category) null else transaction.id to category
            }
        if (newCategoryById.isEmpty()) return 0

        db.withTransaction {
            newCategoryById.groupBy({ it.second }, { it.first }).forEach { (category, ids) ->
                ids.chunked(500).forEach { chunk -> transactionDao.updateCategoryForIds(chunk, category) }
            }
        }
        return newCategoryById.size
    }

    private suspend fun transactionIdsWithDescription(normalizedDescription: String): List<Long> =
        transactionDao.listAll()
            .filter { normalizeDescription(it.description) == normalizedDescription }
            .map { it.id }

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
