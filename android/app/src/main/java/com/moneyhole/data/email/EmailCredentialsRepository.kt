package com.moneyhole.data.email

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class EmailCredentials(
    val address: String,
    val appPassword: String,
    val imapHost: String,
    val imapPort: Int,
)

/**
 * The email app-password is a real account credential (unlike the low-stakes
 * PDF-open passwords stored in plain Room) - EncryptedSharedPreferences is
 * the officially recommended storage for that, not DataStore.
 */
class EmailCredentialsRepository(context: Context) {

    companion object {
        const val DEFAULT_IMAP_HOST = "imap.gmail.com"
        const val DEFAULT_IMAP_PORT = 993

        private const val PREFS_NAME = "email_credentials"
        private const val KEY_ADDRESS = "address"
        private const val KEY_APP_PASSWORD = "app_password"
        private const val KEY_IMAP_HOST = "imap_host"
        private const val KEY_IMAP_PORT = "imap_port"
        private const val KEY_LAST_PROCESSED_UID = "last_processed_uid"
        private const val KEY_FAILED_UIDS = "failed_uids"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun get(): EmailCredentials? {
        val address = prefs.getString(KEY_ADDRESS, null) ?: return null
        val appPassword = prefs.getString(KEY_APP_PASSWORD, null) ?: return null
        val host = prefs.getString(KEY_IMAP_HOST, DEFAULT_IMAP_HOST) ?: DEFAULT_IMAP_HOST
        val port = prefs.getInt(KEY_IMAP_PORT, DEFAULT_IMAP_PORT)
        return EmailCredentials(address, appPassword, host, port)
    }

    /**
     * Only resets the last-processed UID and retry list when the account/host actually changed -
     * "Save" and "Fetch now" are the same button (see EmailSettingsScreen), so this runs on every
     * fetch click even when nothing was edited. Resetting unconditionally would throw away the
     * incremental sync cursor and retry list on every single fetch, forcing a full mailbox
     * re-scan every time instead of only when switching accounts.
     */
    fun save(credentials: EmailCredentials) {
        val previous = get()
        val accountChanged = previous == null ||
            previous.address != credentials.address ||
            previous.imapHost != credentials.imapHost
        val editor = prefs.edit()
            .putString(KEY_ADDRESS, credentials.address)
            .putString(KEY_APP_PASSWORD, credentials.appPassword)
            .putString(KEY_IMAP_HOST, credentials.imapHost)
            .putInt(KEY_IMAP_PORT, credentials.imapPort)
        if (accountChanged) {
            editor.putLong(KEY_LAST_PROCESSED_UID, 0L)
            editor.putString(KEY_FAILED_UIDS, "")
        }
        editor.apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun getLastProcessedUid(): Long = prefs.getLong(KEY_LAST_PROCESSED_UID, 0L)

    fun setLastProcessedUid(uid: Long) {
        prefs.edit().putLong(KEY_LAST_PROCESSED_UID, uid).apply()
    }

    /** UIDs of messages whose PDF failed to import last time - retried on every fetch until
     * they succeed (e.g. once the right default password is registered), instead of the main
     * cursor getting stuck behind them or silently skipping them forever. */
    fun getFailedUids(): Set<Long> {
        val raw = prefs.getString(KEY_FAILED_UIDS, "") ?: ""
        return raw.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
    }

    fun setFailedUids(uids: Set<Long>) {
        prefs.edit().putString(KEY_FAILED_UIDS, uids.joinToString(",")).apply()
    }
}
