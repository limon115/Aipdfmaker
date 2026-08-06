package com.example.data.network

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
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
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpCallValidator

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Float,
    val max_tokens: Int? = null
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
        expectSuccess = true // this will cause Ktor to throw ClientRequestException on 4xx/5xx
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
                if (apiKey.isNotBlank() && provider.lowercase() != "ollama") {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                }
                setBody(requestPayload)
            }

            val jsonResponse = Json { ignoreUnknownKeys = true }.decodeFromString<OpenAiResponse>(response.bodyAsText())
            jsonResponse.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("Empty response from AI Provider")
        }
    }

    suspend fun testConnection(): Boolean {
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
                    OpenAiMessage(role = "user", content = "Reply with 'Test'")
                ),
                temperature = 0f,
                max_tokens = 5
            )
            val resp: HttpResponse = ktorClient.post(baseUrl) {
                contentType(ContentType.Application.Json)
                if (apiKey.isNotBlank() && provider.lowercase() != "ollama") {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                }
                setBody(requestPayload)
            }
            
            val jsonResponse = Json { ignoreUnknownKeys = true }.decodeFromString<OpenAiResponse>(resp.bodyAsText())
            jsonResponse.choices.firstOrNull()?.message?.content
        }
        return response != null
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
            "ollama" -> "http://10.0.2.2:11434/v1/chat/completions" 
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
            if (apiKey.isNotBlank() && provider.lowercase() != "ollama") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
            }
            setBody(requestPayload)
        }

        val jsonResponse = Json { ignoreUnknownKeys = true }.decodeFromString<OpenAiResponse>(response.bodyAsText())
        return jsonResponse.choices.firstOrNull()?.message?.content 
            ?: throw IllegalStateException("Empty response from AI Provider")
    }
}
