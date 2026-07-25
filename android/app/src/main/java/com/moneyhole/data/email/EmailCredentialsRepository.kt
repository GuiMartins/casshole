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

    fun save(credentials: EmailCredentials) {
        prefs.edit()
            .putString(KEY_ADDRESS, credentials.address)
            .putString(KEY_APP_PASSWORD, credentials.appPassword)
            .putString(KEY_IMAP_HOST, credentials.imapHost)
            .putInt(KEY_IMAP_PORT, credentials.imapPort)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
