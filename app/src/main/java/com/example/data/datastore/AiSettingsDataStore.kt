package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.models.AiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_settings")

class AiSettingsDataStore(private val context: Context) {

    companion object {
        val AI1_PROVIDER = stringPreferencesKey("ai1_provider")
        val AI1_MODEL = stringPreferencesKey("ai1_model")
        val AI1_API_KEY = stringPreferencesKey("ai1_api_key")

        val AI2_PROVIDER = stringPreferencesKey("ai2_provider")
        val AI2_MODEL = stringPreferencesKey("ai2_model")
        val AI2_API_KEY = stringPreferencesKey("ai2_api_key")

        val AI2_TEMPERATURE = floatPreferencesKey("ai2_temperature")
        val AI2_MAX_TOKENS = intPreferencesKey("ai2_max_tokens")
        val AI2_TOP_P = floatPreferencesKey("ai2_top_p")
    }

    val aiSettingsFlow: Flow<AiSettings> = context.dataStore.data
        .map { preferences ->
            AiSettings(
                ai1Provider = runCatching { AiProvider.valueOf(preferences[AI1_PROVIDER] ?: AiProvider.GOOGLE_GEMINI.name) }.getOrDefault(AiProvider.GOOGLE_GEMINI),
                ai1Model = preferences[AI1_MODEL] ?: "",
                ai1ApiKey = preferences[AI1_API_KEY] ?: "",
                ai2Provider = runCatching { AiProvider.valueOf(preferences[AI2_PROVIDER] ?: AiProvider.GOOGLE_GEMINI.name) }.getOrDefault(AiProvider.GOOGLE_GEMINI),
                ai2Model = preferences[AI2_MODEL] ?: "",
                ai2ApiKey = preferences[AI2_API_KEY] ?: "",
                ai2Temperature = preferences[AI2_TEMPERATURE] ?: 0.7f,
                ai2MaxTokens = preferences[AI2_MAX_TOKENS] ?: 2048,
                ai2TopP = preferences[AI2_TOP_P] ?: 1.0f
            )
        }

    suspend fun updateAi1Provider(provider: AiProvider) {
        context.dataStore.edit { it[AI1_PROVIDER] = provider.name }
    }

    suspend fun updateAi1Model(model: String) {
        context.dataStore.edit { it[AI1_MODEL] = model }
    }

    suspend fun updateAi1ApiKey(apiKey: String) {
        context.dataStore.edit { it[AI1_API_KEY] = apiKey }
    }

    suspend fun updateAi2Provider(provider: AiProvider) {
        context.dataStore.edit { it[AI2_PROVIDER] = provider.name }
    }

    suspend fun updateAi2Model(model: String) {
        context.dataStore.edit { it[AI2_MODEL] = model }
    }

    suspend fun updateAi2ApiKey(apiKey: String) {
        context.dataStore.edit { it[AI2_API_KEY] = apiKey }
    }

    suspend fun updateAi2Advanced(temperature: Float, maxTokens: Int, topP: Float) {
        context.dataStore.edit {
            it[AI2_TEMPERATURE] = temperature
            it[AI2_MAX_TOKENS] = maxTokens
            it[AI2_TOP_P] = topP
        }
    }
}

data class AiSettings(
    val ai1Provider: AiProvider = AiProvider.GOOGLE_GEMINI,
    val ai1Model: String = "",
    val ai1ApiKey: String = "",
    val ai2Provider: AiProvider = AiProvider.GOOGLE_GEMINI,
    val ai2Model: String = "",
    val ai2ApiKey: String = "",
    val ai2Temperature: Float = 0.7f,
    val ai2MaxTokens: Int = 2048,
    val ai2TopP: Float = 1.0f
)
