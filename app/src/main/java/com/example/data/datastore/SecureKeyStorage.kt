package com.example.data.datastore

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber

/**
 * Secure storage for sensitive API keys and tokens using EncryptedSharedPreferences.
 * Replaces plain text DataStore storage for security-critical data.
 */
class SecureKeyStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // API Keys
    fun setAi1ApiKey(key: String) {
        encryptedPrefs.edit().putString(KEY_AI1, key).apply()
    }

    fun getAi1ApiKey(): String? {
        return encryptedPrefs.getString(KEY_AI1, null)
    }

    fun setAi2ApiKey(key: String) {
        encryptedPrefs.edit().putString(KEY_AI2, key).apply()
    }

    fun getAi2ApiKey(): String? {
        return encryptedPrefs.getString(KEY_AI2, null)
    }

    fun setAi3ApiKey(key: String) {
        encryptedPrefs.edit().putString(KEY_AI3, key).apply()
    }

    fun getAi3ApiKey(): String? {
        return encryptedPrefs.getString(KEY_AI3, null)
    }

    // GitHub Token
    fun setGithubToken(token: String) {
        encryptedPrefs.edit().putString(KEY_GITHUB, token).apply()
    }

    fun getGithubToken(): String? {
        return encryptedPrefs.getString(KEY_GITHUB, null)
    }

    // Migration helper - migrate from unencrypted DataStore
    suspend fun migrateFromDataStore(dataStore: AiSettingsDataStore) {
        try {
            val settings = dataStore.aiSettingsFlow.kotlinx.coroutines.flow.first()

            settings.ai1ApiKey?.let { if (it.isNotBlank()) setAi1ApiKey(it) }
            settings.ai2ApiKey?.let { if (it.isNotBlank()) setAi2ApiKey(it) }
            settings.ai3ApiKey?.let { if (it.isNotBlank()) setAi3ApiKey(it) }
            settings.githubToken?.let { if (it.isNotBlank()) setGithubToken(it) }

            Timber.i("Successfully migrated API keys to encrypted storage")
        } catch (e: Exception) {
            Timber.e(e, "Failed to migrate API keys")
        }
    }

    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_AI1 = "ai1_api_key"
        private const val KEY_AI2 = "ai2_api_key"
        private const val KEY_AI3 = "ai3_api_key"
        private const val KEY_GITHUB = "github_token"

        @Volatile
        private var INSTANCE: SecureKeyStorage? = null

        fun getInstance(context: Context): SecureKeyStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureKeyStorage(context).also { INSTANCE = it }
            }
        }
    }
}
