package com.example.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay

@Serializable data class OpenAiMessage(val role: String, val content: String)
@Serializable data class OpenAiRequest(val model: String, val messages: List<OpenAiMessage>, val temperature: Float, val max_tokens: Int? = null)
@Serializable data class OpenAiResponse(val choices: List<Choice>) {
    @Serializable data class Choice(val message: OpenAiMessage)
}

@Serializable data class GeminiPart(val text: String)
@Serializable data class GeminiContent(val role: String = "user", val parts: List<GeminiPart>)
@Serializable data class GeminiSystemInstruction(val parts: List<GeminiPart>)
@Serializable data class GeminiGenConfig(val temperature: Float, val responseMimeType: String? = null)
@Serializable data class GeminiRequest(val contents: List<GeminiContent>, val systemInstruction: GeminiSystemInstruction? = null, val generationConfig: GeminiGenConfig? = null)
@Serializable data class GeminiResponse(val candidates: List<Candidate>? = null) {
    @Serializable data class Candidate(val content: GeminiContent)
}

class AiNetworkClient(private val provider: String, private val apiKey: String, private val model: String, private val temperature: Float) {
    companion object {
        private val requestMutex = Mutex()
        private val requestTimestamps = mutableListOf<Long>()
        private const val MAX_REQUESTS_PER_MINUTE = 14
        private const val ONE_MINUTE_MS = 60_000L
        
        suspend fun enforceRateLimit() {
            requestMutex.withLock {
                val now = System.currentTimeMillis()
                requestTimestamps.removeAll { now - it > ONE_MINUTE_MS }
                
                if (requestTimestamps.size >= MAX_REQUESTS_PER_MINUTE) {
                    val oldest = requestTimestamps.first()
                    val waitTime = ONE_MINUTE_MS - (now - oldest)
                    if (waitTime > 0) {
                        delay(waitTime + 100)
                    }
                }
                
                val newNow = System.currentTimeMillis()
                val timeSinceLast = newNow - (requestTimestamps.lastOrNull() ?: 0L)
                // Enforce minimum 4 seconds between any two requests
                if (timeSinceLast < 4000L) {
                    delay(4000L - timeSinceLast)
                }
                
                requestTimestamps.add(System.currentTimeMillis())
            }
        }
    }

    private val ktorClient = HttpClient(Android) {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
        install(HttpTimeout) { requestTimeoutMillis = 120_000; connectTimeoutMillis = 15_000; socketTimeoutMillis = 120_000 }
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 5)
            retryOnException(maxRetries = 5, retryOnTimeout = true)
            retryIf { request, response ->
                response.status.value == 429
            }
            exponentialDelay()
        }
    }

    private val isGemini = provider.lowercase().contains("gemini") || provider.lowercase().contains("google")

    private val systemPrompt = """
        You are an AI assistant. Read the provided educational text and extract a structured Blueprint. You MUST return ONLY a raw JSON object matching this structure exactly: { "courseName": "String", "chapterName": "String", "topics": [ { "title": "String", "durationMinutes": Int } ], "formulaCount": Int, "definitionCount": Int, "exampleCount": Int }. Do not include markdown code blocks like ```json.
    """.trimIndent()

    suspend fun generateBlueprint(extractedText: String): String {
        val cleanKey = apiKey.replace(" ", "").trim()
        requireKey(cleanKey)
        return if (isGemini) generateWithGemini(extractedText, cleanKey) else generateWithOpenAiCompatible(extractedText, cleanKey)
    }

    suspend fun generateContent(prompt: String, customSystemPrompt: String? = null, mimeType: String? = null): String {
        val cleanKey = apiKey.replace(" ", "").trim()
        requireKey(cleanKey)
        if (isGemini) {
            return sendGeminiRequest(cleanKey, prompt, customSystemPrompt, mimeType)
        } else {
            val messages = mutableListOf<OpenAiMessage>()
            if (customSystemPrompt != null) messages.add(OpenAiMessage("system", customSystemPrompt))
            messages.add(OpenAiMessage("user", prompt))
            return sendKtorRequest(getOpenAiBaseUrl(), cleanKey, model.ifBlank { "gpt-4o-mini" }, messages, temperature)
        }
    }

    suspend fun testConnection(): Boolean {
        val cleanKey = apiKey.replace(" ", "").trim()
        requireKey(cleanKey)
        if (isGemini) {
            sendGeminiRequest(cleanKey, "Reply with 'Test'")
            return true
        } else {
            sendKtorRequest(getOpenAiBaseUrl(), cleanKey, model.ifBlank { "gpt-4o-mini" }, listOf(OpenAiMessage("user", "Reply with 'Test'")), 0f, 5)
            return true
        }
    }

    private fun requireKey(cleanKey: String) {
        if (cleanKey.isEmpty() && !provider.lowercase().contains("ollama") && !provider.lowercase().contains("lm studio")) {
            throw IllegalArgumentException("API Key is empty!")
        }
    }

    private suspend fun generateWithGemini(extractedText: String, cleanKey: String): String {
        return sendGeminiRequest(cleanKey, extractedText, systemPrompt, "application/json")
    }

    private suspend fun generateWithOpenAiCompatible(extractedText: String, cleanKey: String): String {
        val messages = listOf(OpenAiMessage("system", systemPrompt), OpenAiMessage("user", extractedText))
        return sendKtorRequest(getOpenAiBaseUrl(), cleanKey, model, messages, temperature)
    }

    private suspend fun sendGeminiRequest(cleanKey: String, prompt: String, sysPrompt: String? = null, mimeType: String? = null): String {
        enforceRateLimit()
        val targetModel = model.ifBlank { "gemini-2.5-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${targetModel}:generateContent?key=$cleanKey"

        val requestPayload = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
            systemInstruction = sysPrompt?.let { GeminiSystemInstruction(listOf(GeminiPart(it))) },
            generationConfig = GeminiGenConfig(temperature, mimeType)
        )

        try {
            val response: HttpResponse = ktorClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestPayload)
            }
            val jsonResponse = Json { ignoreUnknownKeys = true }.decodeFromString<GeminiResponse>(response.bodyAsText())
            return jsonResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: throw IllegalStateException("Empty response from Gemini")
        } catch (e: ClientRequestException) {
            throw Exception("API Error ${e.response.status.value}: ${e.response.bodyAsText().take(50)}")
        } catch (e: Exception) {
            throw Exception("Network Error: ${e.message?.take(50)}")
        }
    }

    private suspend fun sendKtorRequest(baseUrl: String, cleanKey: String, reqModel: String, messages: List<OpenAiMessage>, temp: Float, maxTokens: Int? = null): String {
        enforceRateLimit()
        val requestPayload = OpenAiRequest(reqModel, messages, temp, maxTokens)
        try {
            val response: HttpResponse = ktorClient.post(baseUrl) {
                contentType(ContentType.Application.Json)
                if (cleanKey.isNotEmpty() && !provider.lowercase().contains("ollama")) header(HttpHeaders.Authorization, "Bearer $cleanKey")
                setBody(requestPayload)
            }
            val jsonResponse = Json { ignoreUnknownKeys = true }.decodeFromString<OpenAiResponse>(response.bodyAsText())
            return jsonResponse.choices.firstOrNull()?.message?.content ?: throw IllegalStateException("Empty response from Provider")
        } catch (e: ClientRequestException) {
            throw Exception("API Error ${e.response.status.value}")
        } catch (e: Exception) {
            throw Exception("Network Error: ${e.message}")
        }
    }

    private fun getOpenAiBaseUrl(): String {
        return when {
            provider.lowercase().contains("openai") -> "https://api.openai.com/v1/chat/completions"
            provider.lowercase().contains("openrouter") -> "https://openrouter.ai/api/v1/chat/completions"
            provider.lowercase().contains("ollama") -> "http://10.0.2.2:11434/v1/chat/completions"
            provider.lowercase().contains("lm studio") -> "http://10.0.2.2:1234/v1/chat/completions"
            else -> "https://api.openai.com/v1/chat/completions"
        }
    }
}
