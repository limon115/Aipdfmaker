import re

with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    content = f.read()

# Update GeminiGenConfig
content = content.replace(
    'val responseSchema: JsonObject? = null)',
    'val responseSchema: JsonObject? = null, val maxOutputTokens: Int? = null)'
)

# Update generateContent signature
content = content.replace(
    'suspend fun generateContent(prompt: String, customSystemPrompt: String? = null, mimeType: String? = null, useDocumentSchema: Boolean = false): String',
    'suspend fun generateContent(prompt: String, customSystemPrompt: String? = null, mimeType: String? = null, useDocumentSchema: Boolean = false, maxTokens: Int? = null): String'
)

# Update sendGeminiRequest signature
content = content.replace(
    'private suspend fun sendGeminiRequest(cleanKey: String, prompt: String, sysPrompt: String? = null, mimeType: String? = null, schema: JsonObject? = null): String',
    'private suspend fun sendGeminiRequest(cleanKey: String, prompt: String, sysPrompt: String? = null, mimeType: String? = null, schema: JsonObject? = null, maxTokens: Int? = null): String'
)

# Update GeminiGenConfig instantiation
content = content.replace(
    'GeminiGenConfig(temperature, mimeType, schema)',
    'GeminiGenConfig(temperature, mimeType, schema, maxTokens)'
)

# Update generateContent calls
content = re.sub(
    r'return sendGeminiRequest\(cleanKey, prompt, customSystemPrompt, mimeType, schema\)',
    'return sendGeminiRequest(cleanKey, prompt, customSystemPrompt, mimeType, schema, maxTokens)',
    content
)

content = re.sub(
    r'return sendKtorRequest\(getOpenAiBaseUrl\(\), cleanKey, model.ifBlank \{ "gpt-4o-mini" \}, messages, temperature\)',
    'return sendKtorRequest(getOpenAiBaseUrl(), cleanKey, model.ifBlank { "gpt-4o-mini" }, messages, temperature, maxTokens)',
    content
)

# Update debugLatex to pass maxTokens = 8192
content = content.replace(
    'val rawResponse = generateContent(userPrompt, customSystemPrompt = systemPrompt)',
    'val rawResponse = generateContent(userPrompt, customSystemPrompt = systemPrompt, maxTokens = 8192)'
)

with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'w') as f:
    f.write(content)

