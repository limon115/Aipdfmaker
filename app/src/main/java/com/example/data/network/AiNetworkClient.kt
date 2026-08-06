package com.example.data.network

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Float
)

@Serializable
data class OpenAiResponse(
    val choices: List<Choice>
) {
    @Serializable
    data class Choice(
        val message: OpenAiMessage
    )
}

class AiNetworkClient(
    private val provider: String,
    private val apiKey: String,
    private val model: String,
    private val temperature: Float
) {
    private val ktorClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 120_000
        }
    }

    private val systemPrompt = """
        You are an AI assistant. Read the provided educational text and extract a structured Blueprint. You MUST return ONLY a raw JSON object matching this structure exactly: { "courseName": "String", "chapterName": "String", "topics": [ { "title": "String", "durationMinutes": Int } ], "formulaCount": Int, "definitionCount": Int, "exampleCount": Int }. Do not include markdown code blocks like ```json.
    """.trimIndent()

    suspend fun generateBlueprint(extractedText: String): String {
        return if (provider.equals("Gemini", ignoreCase = true) || provider.equals("Google Gemini", ignoreCase = true)) {
            generateWithGemini(extractedText)
        } else {
            generateWithOpenAiCompatible(extractedText)
        }
    }

    suspend fun generateContent(prompt: String, customSystemPrompt: String? = null, mimeType: String? = null): String {
        return if (provider.equals("Gemini", ignoreCase = true) || provider.equals("Google Gemini", ignoreCase = true)) {
            val generativeModel = GenerativeModel(
                modelName = model,
                apiKey = apiKey,
                generationConfig = generationConfig {
                    this.temperature = this@AiNetworkClient.temperature
                    if (mimeType != null) {
                        this.responseMimeType = mimeType
                    }
                },
                systemInstruction = customSystemPrompt?.let { com.google.ai.client.generativeai.type.content { text(it) } }
            )
            val response = generativeModel.generateContent(prompt)
            response.text ?: throw IllegalStateException("Empty response from Gemini")
        } else {
            val baseUrl = when (provider.lowercase()) {
                "openai" -> "https://api.openai.com/v1/chat/completions"
                "openrouter" -> "https://openrouter.ai/api/v1/chat/completions"
                "ollama" -> "http://10.0.2.2:11434/v1/chat/completions"
                "lm studio" -> "http://10.0.2.2:1234/v1/chat/completions"
                else -> "https://api.openai.com/v1/chat/completions"
            }
            val messages = mutableListOf<OpenAiMessage>()
            if (customSystemPrompt != null) {
                messages.add(OpenAiMessage(role = "system", content = customSystemPrompt))
            }
            messages.add(OpenAiMessage(role = "user", content = prompt))

            val requestPayload = OpenAiRequest(
                model = model,
                messages = messages,
                temperature = temperature
            )
            val response: HttpResponse = ktorClient.post(baseUrl) {
                contentType(ContentType.Application.Json)
                if (apiKey.isNotBlank()) {
                    bearerAuth(apiKey)
                }
                setBody(requestPayload)
            }
            if (!response.status.isSuccess()) {
                throw IllegalStateException("API error: ${response.status.value} - ${response.bodyAsText()}")
            }
            val jsonResponse = Json { ignoreUnknownKeys = true }.decodeFromString<OpenAiResponse>(response.bodyAsText())
            jsonResponse.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("Empty response from AI Provider")
        }
    }

    suspend fun testConnection(): Boolean {
        return try {
            val response = if (provider.equals("Gemini", ignoreCase = true) || provider.equals("Google Gemini", ignoreCase = true)) {
                val generativeModel = GenerativeModel(
                    modelName = model.ifBlank { "gemini-1.5-flash" },
                    apiKey = apiKey,
                    generationConfig = generationConfig {
                        this.temperature = 0f
                    }
                )
                generativeModel.generateContent("Reply with the word 'Test'").text
            } else {
                val baseUrl = when (provider.lowercase()) {
                    "openai" -> "https://api.openai.com/v1/chat/completions"
                    "openrouter" -> "https://openrouter.ai/api/v1/chat/completions"
                    "ollama" -> "http://10.0.2.2:11434/v1/chat/completions"
                    "lm studio" -> "http://10.0.2.2:1234/v1/chat/completions"
                    else -> "https://api.openai.com/v1/chat/completions"
                }
                val requestPayload = OpenAiRequest(
                    model = model.ifBlank { "gpt-3.5-turbo" },
                    messages = listOf(
                        OpenAiMessage(role = "user", content = "Reply with the word 'Test'")
                    ),
                    temperature = 0f
                )
                val resp: HttpResponse = ktorClient.post(baseUrl) {
                    contentType(ContentType.Application.Json)
                    if (apiKey.isNotBlank()) {
                        bearerAuth(apiKey)
                    }
                    setBody(requestPayload)
                }
                if (!resp.status.isSuccess()) {
                    throw IllegalStateException("API error: ${resp.status.value} - ${resp.bodyAsText()}")
                }
                val jsonResponse = Json { ignoreUnknownKeys = true }.decodeFromString<OpenAiResponse>(resp.bodyAsText())
                jsonResponse.choices.firstOrNull()?.message?.content
            }
            response != null
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun generateWithGemini(extractedText: String): String {
        val generativeModel = GenerativeModel(
            modelName = model,
            apiKey = apiKey,
            generationConfig = generationConfig {
                this.temperature = this@AiNetworkClient.temperature
                this.responseMimeType = "application/json"
            },
            systemInstruction = com.google.ai.client.generativeai.type.content { text(systemPrompt) }
        )
        
        val response = generativeModel.generateContent(extractedText)
        return response.text ?: throw IllegalStateException("Empty response from Gemini")
    }

    private suspend fun generateWithOpenAiCompatible(extractedText: String): String {
        val baseUrl = when (provider.lowercase()) {
            "openai" -> "https://api.openai.com/v1/chat/completions"
            "openrouter" -> "https://openrouter.ai/api/v1/chat/completions"
            "ollama" -> "http://10.0.2.2:11434/v1/chat/completions" // typical for local emulator testing
            "lm studio" -> "http://10.0.2.2:1234/v1/chat/completions"
            else -> "https://api.openai.com/v1/chat/completions"
        }

        val requestPayload = OpenAiRequest(
            model = model,
            messages = listOf(
                OpenAiMessage(role = "system", content = systemPrompt),
                OpenAiMessage(role = "user", content = extractedText)
            ),
            temperature = temperature
        )

        val response: HttpResponse = ktorClient.post(baseUrl) {
            contentType(ContentType.Application.Json)
            if (apiKey.isNotBlank()) {
                bearerAuth(apiKey)
            }
            setBody(requestPayload)
        }

        if (!response.status.isSuccess()) {
            throw IllegalStateException("API error: ${response.status.value} - ${response.bodyAsText()}")
        }

        val jsonResponse = Json { ignoreUnknownKeys = true }.decodeFromString<OpenAiResponse>(response.bodyAsText())
        return jsonResponse.choices.firstOrNull()?.message?.content 
            ?: throw IllegalStateException("Empty response from AI Provider")
    }
}
