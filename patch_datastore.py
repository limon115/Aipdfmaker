import re

with open('app/src/main/java/com/example/data/datastore/AiSettingsDataStore.kt', 'r') as f:
    content = f.read()

# Add to AiSettings data class
content = re.sub(
    r'val ai2TopP: Float = 1\.0f,',
    r'val ai2TopP: Float = 1.0f,\n    val ai3Provider: AiProvider = AiProvider.GOOGLE_GEMINI,\n    val ai3Model: String = "",\n    val ai3ApiKey: String = "",',
    content
)

# Add to aiSettingsFlow
content = re.sub(
    r'ai2TopP = preferences\[AI2_TOP_P\] \?: 1\.0f,',
    r'ai2TopP = preferences[AI2_TOP_P] ?: 1.0f,\n                ai3Provider = runCatching { AiProvider.valueOf(preferences[AI3_PROVIDER] ?: AiProvider.GOOGLE_GEMINI.name) }.getOrDefault(AiProvider.GOOGLE_GEMINI),\n                ai3Model = preferences[AI3_MODEL] ?: "",\n                ai3ApiKey = preferences[AI3_API_KEY] ?: "",',
    content
)

# Add update methods
methods = """
    suspend fun updateAi3Provider(provider: AiProvider) {
        context.dataStore.edit { it[AI3_PROVIDER] = provider.name }
    }
    suspend fun updateAi3Model(model: String) {
        context.dataStore.edit { it[AI3_MODEL] = model }
    }
    suspend fun updateAi3ApiKey(apiKey: String) {
        context.dataStore.edit { it[AI3_API_KEY] = apiKey }
    }
"""

content = re.sub(r'suspend fun updateThemeMode', methods + '\n    suspend fun updateThemeMode', content)

with open('app/src/main/java/com/example/data/datastore/AiSettingsDataStore.kt', 'w') as f:
    f.write(content)

