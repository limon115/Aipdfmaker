package com.example.domain.services.ai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*

@Serializable
data class GeminiModel(
    val name: String,
    val version: String,
    val displayName: String = "",
    val description: String = "",
    val inputTokenLimit: Int = 0,
    val outputTokenLimit: Int = 0,
    val supportedGenerationMethods: List<String> = emptyList()
)

@Serializable
data class ModelsResponse(
    val models: List<GeminiModel>
)

class GeminiApiTester {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun getAvailableModels(apiKey: String): Result<List<GeminiModel>> {
        return try {
            val response = client.get("https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}")
            if (response.status.isSuccess()) {
                val modelsResponse = response.body<ModelsResponse>()
                val generateContentModels = modelsResponse.models.filter { 
                    it.supportedGenerationMethods.contains("generateContent") 
                }
                Result.success(generateContentModels)
            } else {
                Result.failure(Exception(formatHttpError(response.status.value)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun testModel(apiKey: String, modelName: String, prompt: String): Result<String> {
        return try {
            val requestBody = buildJsonObject {
                put("contents", buildJsonArray {
                    add(buildJsonObject {
                        put("parts", buildJsonArray {
                            add(buildJsonObject { put("text", prompt) })
                        })
                    })
                })
            }
            
            // Note: modelName typically comes back as "models/gemini-1.5-flash", we use it in the URL
            val modelId = if (modelName.startsWith("models/")) modelName else "models/$modelName"
            
            val response = client.post("https://generativelanguage.googleapis.com/v1beta/${modelId}:generateContent?key=${apiKey}") {
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }
            
            if (response.status.isSuccess()) {
                val responseBody = response.bodyAsText()
                val jsonTree = Json.parseToJsonElement(responseBody).jsonObject
                val candidates = jsonTree["candidates"]?.jsonArray
                val text = candidates?.firstOrNull()?.jsonObject
                    ?.get("content")?.jsonObject
                    ?.get("parts")?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("text")?.jsonPrimitive?.content ?: "No response text found"
                Result.success(text)
            } else {
                Result.failure(Exception(formatHttpError(response.status.value)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun formatHttpError(code: Int): String {
        return when (code) {
            401 -> "401"
            403 -> "403"
            429 -> "429"
            else -> "$code"
        }
    }
}
