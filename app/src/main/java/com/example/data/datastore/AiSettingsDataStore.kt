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
import com.example.domain.models.ThemeMode
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
        val AI3_PROVIDER = stringPreferencesKey("ai3_provider")
        val AI3_MODEL = stringPreferencesKey("ai3_model")
        val AI3_API_KEY = stringPreferencesKey("ai3_api_key")

        val AI2_TEMPERATURE = floatPreferencesKey("ai2_temperature")
        val AI2_MAX_TOKENS = intPreferencesKey("ai2_max_tokens")
        val AI2_TOP_P = floatPreferencesKey("ai2_top_p")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
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
                ai2TopP = preferences[AI2_TOP_P] ?: 1.0f,
                ai3Provider = runCatching { AiProvider.valueOf(preferences[AI3_PROVIDER] ?: AiProvider.GOOGLE_GEMINI.name) }.getOrDefault(AiProvider.GOOGLE_GEMINI),
                ai3Model = preferences[AI3_MODEL] ?: "",
                ai3ApiKey = preferences[AI3_API_KEY] ?: "",
                themeMode = runCatching { ThemeMode.valueOf(preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM),
                customFontPath = preferences[CUSTOM_FONT_PATH] ?: "",
                githubToken = preferences[GITHUB_TOKEN] ?: ""
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
    
    suspend fun updateAi3Provider(provider: AiProvider) {
        context.dataStore.edit { it[AI3_PROVIDER] = provider.name }
    }
    suspend fun updateAi3Model(model: String) {
        context.dataStore.edit { it[AI3_MODEL] = model }
    }
    suspend fun updateAi3ApiKey(apiKey: String) {
        context.dataStore.edit { it[AI3_API_KEY] = apiKey }
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = themeMode.name }
    }

    suspend fun updateCustomFontPath(path: String) {
        context.dataStore.edit { it[CUSTOM_FONT_PATH] = path }
    }

    suspend fun updateGithubToken(token: String) {
        context.dataStore.edit { it[GITHUB_TOKEN] = token }
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
    val ai2TopP: Float = 1.0f,
    val ai3Provider: AiProvider = AiProvider.GOOGLE_GEMINI,
    val ai3Model: String = "",
    val ai3ApiKey: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val customFontPath: String = "",
    val githubToken: String = ""
) {
    fun resolveCandidates(
        primarySlot: Int = 1,
        buildConfigApiKey: String = com.example.BuildConfig.GEMINI_API_KEY
    ): List<AiCandidate> {
        val candidates = mutableListOf<AiCandidate>()

        fun addSlotIfConfigured(slot: Int) {
            when (slot) {
                1 -> {
                    val isLocal = ai1Provider == AiProvider.OLLAMA || ai1Provider == AiProvider.LM_STUDIO
                    val hasKey = ai1ApiKey.isNotBlank() && ai1ApiKey != "placeholder"
                    if (hasKey || isLocal) {
                        candidates.add(
                            AiCandidate(
                                provider = ai1Provider.name,
                                apiKey = if (isLocal && !hasKey) "" else ai1ApiKey,
                                model = ai1Model.ifBlank { defaultModelForProvider(ai1Provider) },
                                temperature = ai2Temperature
                            )
                        )
                    }
                }
                2 -> {
                    val isLocal = ai2Provider == AiProvider.OLLAMA || ai2Provider == AiProvider.LM_STUDIO
                    val hasKey = ai2ApiKey.isNotBlank() && ai2ApiKey != "placeholder"
                    if (hasKey || isLocal) {
                        candidates.add(
                            AiCandidate(
                                provider = ai2Provider.name,
                                apiKey = if (isLocal && !hasKey) "" else ai2ApiKey,
                                model = ai2Model.ifBlank { defaultModelForProvider(ai2Provider) },
                                temperature = ai2Temperature
                            )
                        )
                    }
                }
                3 -> {
                    val isLocal = ai3Provider == AiProvider.OLLAMA || ai3Provider == AiProvider.LM_STUDIO
                    val hasKey = ai3ApiKey.isNotBlank() && ai3ApiKey != "placeholder"
                    if (hasKey || isLocal) {
                        candidates.add(
                            AiCandidate(
                                provider = ai3Provider.name,
                                apiKey = if (isLocal && !hasKey) "" else ai3ApiKey,
                                model = ai3Model.ifBlank { defaultModelForProvider(ai3Provider) },
                                temperature = ai2Temperature
                            )
                        )
                    }
                }
            }
        }

        val slotOrder = when (primarySlot) {
            1 -> listOf(1, 2, 3)
            2 -> listOf(2, 1, 3)
            3 -> listOf(3, 1, 2)
            else -> listOf(1, 2, 3)
        }

        for (slot in slotOrder) {
            addSlotIfConfigured(slot)
        }

        if (buildConfigApiKey.isNotBlank() && buildConfigApiKey != "placeholder") {
            candidates.add(
                AiCandidate(
                    provider = AiProvider.GOOGLE_GEMINI.name,
                    apiKey = buildConfigApiKey,
                    model = "gemini-1.5-flash",
                    temperature = ai2Temperature
                )
            )
        }

        return candidates
    }
}

data class AiCandidate(
    val provider: String,
    val apiKey: String,
    val model: String,
    val temperature: Float
)

fun defaultModelForProvider(provider: AiProvider): String {
    return when (provider) {
        AiProvider.GOOGLE_GEMINI -> "gemini-1.5-flash"
        AiProvider.OPENAI -> "gpt-4o-mini"
        AiProvider.ANTHROPIC_CLAUDE -> "claude-3-5-haiku-20241022"
        AiProvider.OPENROUTER -> "meta-llama/llama-3.1-8b-instruct:free"
        AiProvider.OLLAMA -> "llama3"
        AiProvider.LM_STUDIO -> "local-model"
        AiProvider.CUSTOM -> "gpt-4o-mini"
    }
}

fun resolveCandidates(
    settings: AiSettings,
    primarySlot: Int = 1,
    buildConfigApiKey: String = com.example.BuildConfig.GEMINI_API_KEY
): List<AiCandidate> = settings.resolveCandidates(primarySlot, buildConfigApiKey)

