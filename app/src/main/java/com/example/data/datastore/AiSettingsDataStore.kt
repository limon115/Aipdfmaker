package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
        val AI1_TEMPERATURE = floatPreferencesKey("ai1_temperature")
        val AI1_MAX_TOKENS = intPreferencesKey("ai1_max_tokens")
        val AI1_TOP_P = floatPreferencesKey("ai1_top_p")

        val AI2_PROVIDER = stringPreferencesKey("ai2_provider")
        val AI2_MODEL = stringPreferencesKey("ai2_model")
        val AI2_API_KEY = stringPreferencesKey("ai2_api_key")
        val AI2_TEMPERATURE = floatPreferencesKey("ai2_temperature")
        val AI2_MAX_TOKENS = intPreferencesKey("ai2_max_tokens")
        val AI2_TOP_P = floatPreferencesKey("ai2_top_p")

        val AI3_PROVIDER = stringPreferencesKey("ai3_provider")
        val AI3_MODEL = stringPreferencesKey("ai3_model")
        val AI3_API_KEY = stringPreferencesKey("ai3_api_key")
        val AI3_TEMPERATURE = floatPreferencesKey("ai3_temperature")
        val AI3_MAX_TOKENS = intPreferencesKey("ai3_max_tokens")
        val AI3_TOP_P = floatPreferencesKey("ai3_top_p")

        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")
        val GITHUB_TOKEN = stringPreferencesKey("github_token")

        val STREAMING_ENABLED = booleanPreferencesKey("streaming_enabled")
        val CACHE_ENABLED = booleanPreferencesKey("cache_enabled")
        val CONTEXT_PRUNING_ENABLED = booleanPreferencesKey("context_pruning_enabled")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val PRE_RELEASE_UPDATES = booleanPreferencesKey("pre_release_updates")
        val PROMPT_NOTE_GENERATION = stringPreferencesKey("prompt_note_generation")
        val PROMPT_MATH_SOLVER = stringPreferencesKey("prompt_math_solver")
        val PROMPT_LATEX_CHAT = stringPreferencesKey("prompt_latex_chat")
    }

    val aiSettingsFlow: Flow<AiSettings> = context.dataStore.data
        .map { preferences ->
            AiSettings(
                ai1Provider = runCatching { AiProvider.valueOf(preferences[AI1_PROVIDER] ?: AiProvider.GOOGLE_GEMINI.name) }.getOrDefault(AiProvider.GOOGLE_GEMINI),
                ai1Model = preferences[AI1_MODEL] ?: "",
                ai1ApiKey = preferences[AI1_API_KEY] ?: "",
                ai1Temperature = preferences[AI1_TEMPERATURE] ?: 0.5f,
                ai1MaxTokens = preferences[AI1_MAX_TOKENS] ?: 2048,
                ai1TopP = preferences[AI1_TOP_P] ?: 0.95f,

                ai2Provider = runCatching { AiProvider.valueOf(preferences[AI2_PROVIDER] ?: AiProvider.GOOGLE_GEMINI.name) }.getOrDefault(AiProvider.GOOGLE_GEMINI),
                ai2Model = preferences[AI2_MODEL] ?: "",
                ai2ApiKey = preferences[AI2_API_KEY] ?: "",
                ai2Temperature = preferences[AI2_TEMPERATURE] ?: 0.7f,
                ai2MaxTokens = preferences[AI2_MAX_TOKENS] ?: 2048,
                ai2TopP = preferences[AI2_TOP_P] ?: 1.0f,

                ai3Provider = runCatching { AiProvider.valueOf(preferences[AI3_PROVIDER] ?: AiProvider.GOOGLE_GEMINI.name) }.getOrDefault(AiProvider.GOOGLE_GEMINI),
                ai3Model = preferences[AI3_MODEL] ?: "",
                ai3ApiKey = preferences[AI3_API_KEY] ?: "",
                ai3Temperature = preferences[AI3_TEMPERATURE] ?: 0.2f,
                ai3MaxTokens = preferences[AI3_MAX_TOKENS] ?: 2048,
                ai3TopP = preferences[AI3_TOP_P] ?: 0.9f,

                themeMode = runCatching { ThemeMode.valueOf(preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM),
                customFontPath = preferences[CUSTOM_FONT_PATH] ?: "",
                githubToken = preferences[GITHUB_TOKEN] ?: "",
                streamingEnabled = preferences[STREAMING_ENABLED] ?: true,
                cacheEnabled = preferences[CACHE_ENABLED] ?: true,
                contextPruningEnabled = preferences[CONTEXT_PRUNING_ENABLED] ?: true,
                systemPrompt = preferences[SYSTEM_PROMPT] ?: "",
                autoCheckUpdates = preferences[AUTO_CHECK_UPDATES] ?: true,
                preReleaseUpdates = preferences[PRE_RELEASE_UPDATES] ?: false,
                promptNoteGeneration = preferences[PROMPT_NOTE_GENERATION]?.ifBlank { null } ?: com.example.domain.models.DefaultPrompts.DEFAULT_NOTE_GENERATION_PROMPT,
                promptMathSolver = preferences[PROMPT_MATH_SOLVER]?.ifBlank { null } ?: com.example.domain.models.DefaultPrompts.DEFAULT_MATH_SOLVER_PROMPT,
                promptLatexChat = preferences[PROMPT_LATEX_CHAT]?.ifBlank { null } ?: com.example.domain.models.DefaultPrompts.DEFAULT_LATEX_CHAT_PROMPT
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

    suspend fun updateAi1Advanced(temperature: Float, maxTokens: Int, topP: Float) {
        context.dataStore.edit {
            it[AI1_TEMPERATURE] = temperature
            it[AI1_MAX_TOKENS] = maxTokens
            it[AI1_TOP_P] = topP
        }
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

    suspend fun updateAi3Advanced(temperature: Float, maxTokens: Int, topP: Float) {
        context.dataStore.edit {
            it[AI3_TEMPERATURE] = temperature
            it[AI3_MAX_TOKENS] = maxTokens
            it[AI3_TOP_P] = topP
        }
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

    suspend fun updateStreamingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[STREAMING_ENABLED] = enabled }
    }

    suspend fun updateCacheEnabled(enabled: Boolean) {
        context.dataStore.edit { it[CACHE_ENABLED] = enabled }
    }

    suspend fun updateContextPruningEnabled(enabled: Boolean) {
        context.dataStore.edit { it[CONTEXT_PRUNING_ENABLED] = enabled }
    }

    suspend fun updateSystemPrompt(prompt: String) {
        context.dataStore.edit { it[SYSTEM_PROMPT] = prompt }
    }

    suspend fun updateAutoCheckUpdates(enabled: Boolean) {
        context.dataStore.edit { it[AUTO_CHECK_UPDATES] = enabled }
    }

    suspend fun updatePreReleaseUpdates(enabled: Boolean) {
        context.dataStore.edit { it[PRE_RELEASE_UPDATES] = enabled }
    }

    suspend fun updatePromptNoteGeneration(prompt: String) {
        context.dataStore.edit { it[PROMPT_NOTE_GENERATION] = prompt }
    }

    suspend fun updatePromptMathSolver(prompt: String) {
        context.dataStore.edit { it[PROMPT_MATH_SOLVER] = prompt }
    }

    suspend fun updatePromptLatexChat(prompt: String) {
        context.dataStore.edit { it[PROMPT_LATEX_CHAT] = prompt }
    }

    suspend fun resetPromptsToDefault() {
        context.dataStore.edit {
            it.remove(PROMPT_NOTE_GENERATION)
            it.remove(PROMPT_MATH_SOLVER)
            it.remove(PROMPT_LATEX_CHAT)
        }
    }
}

data class AiSettings(
    val ai1Provider: AiProvider = AiProvider.GOOGLE_GEMINI,
    val ai1Model: String = "",
    val ai1ApiKey: String = "",
    val ai1Temperature: Float = 0.5f,
    val ai1MaxTokens: Int = 2048,
    val ai1TopP: Float = 0.95f,

    val ai2Provider: AiProvider = AiProvider.GOOGLE_GEMINI,
    val ai2Model: String = "",
    val ai2ApiKey: String = "",
    val ai2Temperature: Float = 0.7f,
    val ai2MaxTokens: Int = 2048,
    val ai2TopP: Float = 1.0f,

    val ai3Provider: AiProvider = AiProvider.GOOGLE_GEMINI,
    val ai3Model: String = "",
    val ai3ApiKey: String = "",
    val ai3Temperature: Float = 0.2f,
    val ai3MaxTokens: Int = 2048,
    val ai3TopP: Float = 0.9f,

    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val customFontPath: String = "",
    val githubToken: String = "",
    val streamingEnabled: Boolean = true,
    val cacheEnabled: Boolean = true,
    val contextPruningEnabled: Boolean = true,
    val systemPrompt: String = "",
    val autoCheckUpdates: Boolean = true,
    val preReleaseUpdates: Boolean = false,
    val promptNoteGeneration: String = com.example.domain.models.DefaultPrompts.DEFAULT_NOTE_GENERATION_PROMPT,
    val promptMathSolver: String = com.example.domain.models.DefaultPrompts.DEFAULT_MATH_SOLVER_PROMPT,
    val promptLatexChat: String = com.example.domain.models.DefaultPrompts.DEFAULT_LATEX_CHAT_PROMPT
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
                                temperature = ai1Temperature
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
                                temperature = ai3Temperature
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


