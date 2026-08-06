package com.example.data.network

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.ai.client.generativeai.type.content
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
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

@Serializable
data class OpenAiMessage(val role: String, val content: String)

@Serializable
data class OpenAiRequest(val model: String, val messages: List<OpenAiMessage>, val temperature: Float, val max_tokens: Int? = null)

@Serializable
data class OpenAiResponse(val choices: List<Choice>) {
    @Serializable
    data class Choice(val message: OpenAiMessage)
}

class AiNetworkClient(
    private val provider: String,
    private val apiKey: String,
    private val model: String,
    private val temperature: Float
) {
    private val ktorClient = HttpClient(Android) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 120_000
        }
    }

    private val isGemini = provider.lowercase().contains("gemini") || provider.lowercase().contains("google")

    private val systemPrompt = """
        You are an AI assistant. Read the provided educational text and extract a structured Blueprint. You MUST return ONLY a raw JSON object matching this structure exactly: { "courseName": "String", "chapterName": "String", "topics": [ { "title": "String", "durationMinutes": Int } ], "formulaCount": Int, "definitionCount": Int, "exampleCount": Int }. Do not include markdown code blocks like ```json.
    """.trimIndent()

    suspend fun generateBlueprint(extractedText: String): String {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty() && !provider.lowercase().contains("ollama") && !provider.lowercase().contains("lm studio")) {
            throw IllegalArgumentException("API Key is empty!")
        }
        return if (isGemini) generateWithGemini(extractedText, cleanKey) else generateWithOpenAiCompatible(extractedText, cleanKey)
    }

    suspend fun generateContent(prompt: String, customSystemPrompt: String? = null, mimeType: String? = null): String {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty() && !provider.lowercase().contains("ollama") && !provider.lowercase().contains("lm studio")) {
            throw IllegalArgumentException("API Key is empty!")
        }

        if (isGemini) {
            return try {
                val generativeModel = GenerativeModel(
                    modelName = getSafeGeminiModel(model),
                    apiKey = cleanKey,
                    generationConfig = generationConfig {
                        this.temperature = this@AiNetworkClient.temperature
                        if (mimeType != null) { this.responseMimeType = mimeType }
                    },
                    systemInstruction = customSystemPrompt?.let { content { text(it) } }
                )
                val response = generativeModel.generateContent(prompt)
                response.text ?: throw IllegalStateException("Empty response from Gemini")
            } catch (e: Exception) {
                throw Exception("Gemini Error: ${e.message}")
            }
        } else {
            val baseUrl = getOpenAiBaseUrl()
            val messages = mutableListOf<OpenAiMessage>()
            if (customSystemPrompt != null) messages.add(OpenAiMessage(role = "system", content = customSystemPrompt))
            messages.add(OpenAiMessage(role = "user", content = prompt))

            return sendKtorRequest(baseUrl, cleanKey, model.ifBlank { "gpt-4o-mini" }, messages, temperature)
        }
    }

    suspend fun testConnection(): Boolean {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty() && !provider.lowercase().contains("ollama") && !provider.lowercase().contains("lm studio")) {
            throw IllegalArgumentException("API Key is empty!")
        }

        if (isGemini) {
            try {
                val generativeModel = GenerativeModel(
                    modelName = getSafeGeminiModel(model),
                    apiKey = cleanKey,
                    generationConfig = generationConfig { this.temperature = 0f }
                )
                val response = generativeModel.generateContent("Reply with the word 'Test'")
                return response.text != null
            } catch (e: Exception) {
                throw Exception("Gemini Error: ${e.message}")
            }
        } else {
            val baseUrl = getOpenAiBaseUrl()
            val messages = listOf(OpenAiMessage(role = "user", content = "Reply with 'Test'"))
            sendKtorRequest(baseUrl, cleanKey, model.ifBlank { "gpt-4o-mini" }, messages, 0f, 5)
            return true
        }
    }

    private suspend fun generateWithGemini(extractedText: String, cleanKey: String): String {
        return try {
            val generativeModel = GenerativeModel(
                modelName = getSafeGeminiModel(model),
                apiKey = cleanKey,
                generationConfig = generationConfig {
                    this.temperature = this@AiNetworkClient.temperature
                    this.responseMimeType = "application/json"
                },
                systemInstruction = content { text(systemPrompt) }
            )
            val response = generativeModel.generateContent(extractedText)
            response.text ?: throw IllegalStateException("Empty response from Gemini")
        } catch (e: Exception) {
            throw Exception("Gemini Error: ${e.message}")
        }
    }

    private suspend fun generateWithOpenAiCompatible(extractedText: String, cleanKey: String): String {
        val baseUrl = getOpenAiBaseUrl()
        val messages = listOf(
            OpenAiMessage(role = "system", content = systemPrompt),
            OpenAiMessage(role = "user", content = extractedText)
        )
        return sendKtorRequest(baseUrl, cleanKey, model, messages, temperature)
    }

    private suspend fun sendKtorRequest(baseUrl: String, cleanKey: String, reqModel: String, messages: List<OpenAiMessage>, temp: Float, maxTokens: Int? = null): String {
        val requestPayload = OpenAiRequest(model = reqModel, messages = messages, temperature = temp, max_tokens = maxTokens)
        try {
            val response: HttpResponse = ktorClient.post(baseUrl) {
                contentType(ContentType.Application.Json)
                if (cleanKey.isNotEmpty() && !provider.lowercase().contains("ollama")) {
                    header(HttpHeaders.Authorization, "Bearer $cleanKey")
                }
                setBody(requestPayload)
            }
            val jsonResponse = Json { ignoreUnknownKeys = true }.decodeFromString<OpenAiResponse>(response.bodyAsText())
            return jsonResponse.choices.firstOrNull()?.message?.content ?: throw IllegalStateException("Empty response from Provider")
        } catch (e: ClientRequestException) {
            throw Exception("${e.response.status.value}: ${e.response.bodyAsText()}")
        } catch (e: Exception) {
            throw Exception("Network Error: ${e.message}")
        }
    }

    private fun getOpenAiBaseUrl(): String {
        return when {
            provider.lowercase().contains("openai") -> "[https://api.openai.com/v1/chat/completions](https://api.openai.com/v1/chat/completions)"
            provider.lowercase().contains("openrouter") -> "[https://openrouter.ai/api/v1/chat/completions](https://openrouter.ai/api/v1/chat/completions)"
            provider.lowercase().contains("ollama") -> "[http://10.0.2.2:11434/v1/chat/completions](http://10.0.2.2:11434/v1/chat/completions)"
            provider.lowercase().contains("lm studio") -> "[http://10.0.2.2:1234/v1/chat/completions](http://10.0.2.2:1234/v1/chat/completions)"
            else -> "[https://api.openai.com/v1/chat/completions](https://api.openai.com/v1/chat/completions)"
        }
    }
    
    private fun getSafeGeminiModel(inputModel: String): String {
        // If the user typed "gemini-1.5-flash", upgrade it because Google disabled it in 2026.
        if (inputModel.contains("1.5-flash") || inputModel.isBlank()) {
            return "gemini-3.5-flash" 
        }
        return inputModel
    }
}
