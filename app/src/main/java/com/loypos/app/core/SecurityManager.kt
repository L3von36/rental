package com.loypos.app.core

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * SecurityManager class utilizing EncryptedSharedPreferences from androidx.security.crypto
 * to securely store and retrieve sensitive tokens and API credentials at runtime.
 * Backed by hardware-level Android Keystore AES-256 GCM encryption.
 */
object SecurityManager {
    private const val PREF_NAME = "loypos_secure_vault"
    private const val KEY_PLESK_TOKEN = "plesk_sync_token"
    private const val KEY_CUSTOM_API_CREDENTIAL = "custom_api_credential"

    fun saveSecureToken(context: Context, token: String) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.edit().putString(KEY_PLESK_TOKEN, token.trim()).apply()
        } catch (e: Exception) {
            // Handle secure storage exception gracefully
        }
    }

    fun getSecureToken(context: Context): String {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.getString(KEY_PLESK_TOKEN, "").orEmpty()
        } catch (e: Exception) {
            ""
        }
    }

    fun saveCredential(context: Context, key: String, value: String) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.edit().putString(key, value.trim()).apply()
        } catch (e: Exception) {
            // Handle exception
        }
    }

    fun getCredential(context: Context, key: String): String {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.getString(key, "").orEmpty()
        } catch (e: Exception) {
            ""
        }
    }
}
