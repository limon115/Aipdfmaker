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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
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
@Serializable data class GeminiGenConfig(val temperature: Float, val responseMimeType: String? = null, val responseSchema: JsonObject? = null, val maxOutputTokens: Int? = null)
@Serializable data class GeminiRequest(val contents: List<GeminiContent>, val systemInstruction: GeminiSystemInstruction? = null, val generationConfig: GeminiGenConfig? = null)
@Serializable data class GeminiResponse(val candidates: List<Candidate>? = null) {
    @Serializable data class Candidate(val content: GeminiContent)
}

class AiNetworkClient(private val provider: String, private val apiKey: String, private val model: String, private val temperature: Float, private val featureName: String = "AI 1 - Blueprint") {
    private val jsonFormat = Json { ignoreUnknownKeys = true }
    companion object {
        private val requestMutex = Mutex()
        private val requestTimestamps = mutableListOf<Long>()
        private val tokenUsageHistory = mutableListOf<Pair<Long, Int>>()
        private const val MAX_REQUESTS_PER_MINUTE = 14
        private const val MAX_TOKENS_PER_MINUTE = 800_000
        private const val ONE_MINUTE_MS = 60_000L
        
        suspend fun enforceRateLimit(estimatedTokens: Int = 1000) {
            var delayMs = 0L
            requestMutex.withLock {
                val now = System.currentTimeMillis()
                requestTimestamps.removeAll { now - it > ONE_MINUTE_MS }
                tokenUsageHistory.removeAll { now - it.first > ONE_MINUTE_MS }

                val currentTokens = tokenUsageHistory.sumOf { it.second }
                
                var tokensToWait = 0L
                if (currentTokens + estimatedTokens > MAX_TOKENS_PER_MINUTE) {
                    val oldestToken = tokenUsageHistory.firstOrNull()
                    if (oldestToken != null) {
                        tokensToWait = ONE_MINUTE_MS - (now - oldestToken.first) + 100
                    }
                }

                var requestsToWait = 0L
                if (requestTimestamps.size >= MAX_REQUESTS_PER_MINUTE) {
                    val oldest = requestTimestamps.firstOrNull()
                    if (oldest != null) {
                        requestsToWait = ONE_MINUTE_MS - (now - oldest) + 100
                    }
                }

                val timeSinceLast = now - (requestTimestamps.lastOrNull() ?: 0L)
                var bufferWait = 0L
                if (timeSinceLast < 4000L && requestTimestamps.isNotEmpty()) {
                    bufferWait = 4000L - timeSinceLast
                }

                delayMs = maxOf(tokensToWait, requestsToWait, bufferWait)

                if (delayMs <= 0L) {
                    val finalNow = System.currentTimeMillis()
                    requestTimestamps.add(finalNow)
                    tokenUsageHistory.add(Pair(finalNow, estimatedTokens))
                }
            }

            // 🛡️ FIX 2: Call delay OUTSIDE the Mutex lock so we don't freeze other workers!
            if (delayMs > 0L) {
                kotlinx.coroutines.delay(delayMs)
                enforceRateLimit(estimatedTokens)
            }
        }
    }

    private val ktorClient = HttpClient(Android) {
        expectSuccess = true
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
        install(HttpTimeout) { requestTimeoutMillis = 300_000; connectTimeoutMillis = 30_000; socketTimeoutMillis = 300_000 }
        install(HttpRequestRetry) {
            maxRetries = 5
            retryIf { request, response ->
                if (response.status.value == 429) {
                    com.example.domain.services.ai.AiUsageTracker.trackRateLimitError()
                }
                response.status.value == 429 || response.status.value >= 500
            }
            retryOnException(maxRetries = 5, retryOnTimeout = true)
            delayMillis { retry ->
                // Calculate exponential backoff with jitter (10s, 20s, 40s, 80s...)
                val baseDelay = (10000L * Math.pow(2.0, (retry - 1).toDouble())).toLong()
                val jitter = (Math.random() * 2000).toLong() // 0-2s jitter
                
                // If the server provides a Retry-After header, respect it
                var retryAfterMs = 0L
                try {
                    val retryAfterStr = response?.headers?.get(io.ktor.http.HttpHeaders.RetryAfter)
                    if (retryAfterStr != null) {
                        retryAfterMs = retryAfterStr.toLongOrNull()?.times(1000L) ?: 0L
                    }
                } catch(e: Exception) {}
                
                if (retryAfterMs > 0) retryAfterMs + jitter else baseDelay + jitter
            }
        }
    }

    private val isGemini = provider.lowercase().contains("gemini") || provider.lowercase().contains("google")


    private val blueprintSchema = kotlinx.serialization.json.buildJsonObject {
        put("type", kotlinx.serialization.json.JsonPrimitive("OBJECT"))
        put("properties", kotlinx.serialization.json.buildJsonObject {
            put("courseName", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
            put("chapterName", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
            put("topics", kotlinx.serialization.json.buildJsonObject {
                put("type", kotlinx.serialization.json.JsonPrimitive("ARRAY"))
                put("items", kotlinx.serialization.json.buildJsonObject {
                    put("type", kotlinx.serialization.json.JsonPrimitive("OBJECT"))
                    put("properties", kotlinx.serialization.json.buildJsonObject {
                        put("title", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
                        put("durationMinutes", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("INTEGER")) })
                    })
                })
            })
            put("formulaCount", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("INTEGER")) })
            put("definitionCount", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("INTEGER")) })
            put("exampleCount", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("INTEGER")) })
            put("diagramCount", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("INTEGER")) })
            put("examTipCount", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("INTEGER")) })
        })
        put("required", kotlinx.serialization.json.buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("courseName"))
            add(kotlinx.serialization.json.JsonPrimitive("chapterName"))
            add(kotlinx.serialization.json.JsonPrimitive("topics"))
            add(kotlinx.serialization.json.JsonPrimitive("formulaCount"))
            add(kotlinx.serialization.json.JsonPrimitive("definitionCount"))
            add(kotlinx.serialization.json.JsonPrimitive("exampleCount"))
            add(kotlinx.serialization.json.JsonPrimitive("diagramCount"))
            add(kotlinx.serialization.json.JsonPrimitive("examTipCount"))
        })
    }

    private val documentSchema = kotlinx.serialization.json.buildJsonObject {
        put("type", kotlinx.serialization.json.JsonPrimitive("OBJECT"))
        put("properties", kotlinx.serialization.json.buildJsonObject {
            put("schemaVersion", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("INTEGER")) })
            put("title", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
            put("author", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
            put("language", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
            put("blocks", kotlinx.serialization.json.buildJsonObject {
                put("type", kotlinx.serialization.json.JsonPrimitive("ARRAY"))
                put("items", kotlinx.serialization.json.buildJsonObject {
                    put("type", kotlinx.serialization.json.JsonPrimitive("OBJECT"))
                    put("properties", kotlinx.serialization.json.buildJsonObject {
                        put("type", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
                        put("level", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("INTEGER")) })
                        put("text", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
                        put("latex", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
                        put("display", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("BOOLEAN")) })
                        put("items", kotlinx.serialization.json.buildJsonObject {
                            put("type", kotlinx.serialization.json.JsonPrimitive("ARRAY"))
                            put("items", kotlinx.serialization.json.buildJsonObject {
                                put("type", kotlinx.serialization.json.JsonPrimitive("STRING"))
                            })
                        })
                        put("path", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
                        put("columns", kotlinx.serialization.json.buildJsonObject {
                            put("type", kotlinx.serialization.json.JsonPrimitive("ARRAY"))
                            put("items", kotlinx.serialization.json.buildJsonObject {
                                put("type", kotlinx.serialization.json.JsonPrimitive("STRING"))
                            })
                        })
                        put("rows", kotlinx.serialization.json.buildJsonObject {
                            put("type", kotlinx.serialization.json.JsonPrimitive("ARRAY"))
                            put("items", kotlinx.serialization.json.buildJsonObject {
                                put("type", kotlinx.serialization.json.JsonPrimitive("ARRAY"))
                                put("items", kotlinx.serialization.json.buildJsonObject {
                                    put("type", kotlinx.serialization.json.JsonPrimitive("STRING"))
                                })
                            })
                        })
                        put("content", kotlinx.serialization.json.buildJsonObject {
                            put("type", kotlinx.serialization.json.JsonPrimitive("ARRAY"))
                            put("items", kotlinx.serialization.json.buildJsonObject {
                                put("type", kotlinx.serialization.json.JsonPrimitive("OBJECT"))
                                put("properties", kotlinx.serialization.json.buildJsonObject {
                                    put("type", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
                                    put("value", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
                                    put("latex", kotlinx.serialization.json.buildJsonObject { put("type", kotlinx.serialization.json.JsonPrimitive("STRING")) })
                                })
                            })
                        })
                    })
                    put("required", kotlinx.serialization.json.buildJsonArray {
                        add(kotlinx.serialization.json.JsonPrimitive("type"))
                    })
                })
            })
        })
        put("required", kotlinx.serialization.json.buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("schemaVersion"))
            add(kotlinx.serialization.json.JsonPrimitive("title"))
            add(kotlinx.serialization.json.JsonPrimitive("blocks"))
        })
    }

    private val systemPrompt = """
        You are an elite educational architect designing a master textbook syllabus. Read the provided text and extract a deeply structured Blueprint.
        
        RULES:
        1. Break the content into highly granular sub-topics that require rigorous explanation.
        2. Prescribe a high number of formulas, rigorous proofs, and visual diagrams (especially TikZ graphs or circuitikz) where relevant.
        3. You MUST return ONLY a raw JSON object matching this structure exactly: { "courseName": "String", "chapterName": "String", "topics": [ { "title": "String", "durationMinutes": Int } ], "formulaCount": Int, "definitionCount": Int, "exampleCount": Int, "diagramCount": Int, "examTipCount": Int }. Do not include markdown code blocks like ```json.
    """.trimIndent()

    
    suspend fun debugLatex(latexCode: String, logContent: String): String {
        val systemPrompt = """You are an automated Python script generator for patching LaTeX files.
The Python script MUST read the file path from `sys.argv[1]`, fix the LaTeX errors via string replacement or regex, and overwrite the file.

CRITICAL RULES:
1. You MUST respond with ONLY the raw Python code, wrapped in a ```python markdown block.
2. Do NOT output JSON.
3. The python script MUST be a valid, raw Python 3 script (starting with import sys).
""".trimIndent()

        val userPrompt = """LaTeX Code:
$latexCode

Compiler Log:
$logContent""".trimIndent()

        val rawResponse = generateContent(
            prompt = userPrompt, 
            customSystemPrompt = systemPrompt, 
            mimeType = "text/plain",
            maxTokens = 8192
        )

        val codeBlockRegex = Regex("```(?:python)?(.*?)```", RegexOption.DOT_MATCHES_ALL)
        val matchResult = codeBlockRegex.find(rawResponse)
        return if (matchResult != null) {
            matchResult.groupValues[1].trim()
        } else {
            rawResponse.trim()
        }
    }

    suspend fun generateBlueprint(extractedText: String): String {
        val cleanKey = apiKey.replace(" ", "").trim()
        requireKey(cleanKey)
        return if (isGemini) generateWithGemini(extractedText, cleanKey) else generateWithOpenAiCompatible(extractedText, cleanKey)
    }

    suspend fun generateContent(prompt: String, customSystemPrompt: String? = null, mimeType: String? = null, useDocumentSchema: Boolean = false, maxTokens: Int? = null): String {
        val cleanKey = apiKey.replace(" ", "").trim()
        requireKey(cleanKey)
        if (isGemini) {
            val schema = null // if (useDocumentSchema) documentSchema else null
            return sendGeminiRequest(cleanKey, prompt, customSystemPrompt, mimeType, schema, maxTokens)
        } else {
            val messages = mutableListOf<OpenAiMessage>()
            if (customSystemPrompt != null) messages.add(OpenAiMessage("system", customSystemPrompt))
            messages.add(OpenAiMessage("user", prompt))
            return sendKtorRequest(getOpenAiBaseUrl(), cleanKey, model.ifBlank { "gpt-4o-mini" }, messages, temperature, maxTokens)
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
        return sendGeminiRequest(cleanKey, extractedText, systemPrompt, "application/json", null)
    }

    private suspend fun generateWithOpenAiCompatible(extractedText: String, cleanKey: String): String {
        val messages = listOf(OpenAiMessage("system", systemPrompt), OpenAiMessage("user", extractedText))
        return sendKtorRequest(getOpenAiBaseUrl(), cleanKey, model, messages, temperature)
    }

    private suspend fun sendGeminiRequest(cleanKey: String, prompt: String, sysPrompt: String? = null, mimeType: String? = null, schema: JsonObject? = null, maxTokens: Int? = null): String {
        val estimatedTokens = (prompt.length + (sysPrompt?.length ?: 0)) / 4
        enforceRateLimit(estimatedTokens)
        com.example.domain.services.ai.AiUsageTracker.trackRequest(featureName, estimatedTokens)
        val targetModel = model.ifBlank { "gemini-1.5-flash" }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/${targetModel}:generateContent?key=$cleanKey"

        val requestPayload = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(prompt)))),
            systemInstruction = sysPrompt?.let { GeminiSystemInstruction(listOf(GeminiPart(it))) },
            generationConfig = GeminiGenConfig(temperature, mimeType, schema, maxTokens)
        )

        com.example.utils.AppLogger.d("AiNetwork", "Sending Gemini request to $targetModel (${prompt.length} chars)")
        try {
            val response: HttpResponse = ktorClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(requestPayload)
            }
            val jsonResponse = jsonFormat.decodeFromString<GeminiResponse>(response.bodyAsText())
            com.example.utils.AppLogger.d("AiNetwork", "Gemini request successful")
            val rawText = jsonResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: throw IllegalStateException("Empty response from Gemini")
            return extractJson(rawText)
        } catch (e: ClientRequestException) {
            val err = "API Error ${e.response.status.value}: ${e.response.bodyAsText().take(50)}"
            com.example.utils.AppLogger.e("AiNetwork", err, e)
            throw Exception(err)
        } catch (e: Exception) {
            val err = "Network Error: ${e.message?.take(50)}"
            com.example.utils.AppLogger.e("AiNetwork", err, e)
            throw Exception(err)
        }
    }

    private suspend fun sendKtorRequest(baseUrl: String, cleanKey: String, reqModel: String, messages: List<OpenAiMessage>, temp: Float, maxTokens: Int? = null): String {
        val estimatedTokens = messages.sumOf { (it.content ?: "").length } / 4
        enforceRateLimit(estimatedTokens)
        com.example.domain.services.ai.AiUsageTracker.trackRequest(featureName, estimatedTokens)
        val requestPayload = OpenAiRequest(reqModel, messages, temp, maxTokens)
        com.example.utils.AppLogger.d("AiNetwork", "Sending OpenAI request to $reqModel (${messages.size} messages)")
        try {
            val response: HttpResponse = ktorClient.post(baseUrl) {
                contentType(ContentType.Application.Json)
                if (cleanKey.isNotEmpty() && !provider.lowercase().contains("ollama")) header(HttpHeaders.Authorization, "Bearer $cleanKey")
                setBody(requestPayload)
            }
            val jsonResponse = jsonFormat.decodeFromString<OpenAiResponse>(response.bodyAsText())
            com.example.utils.AppLogger.d("AiNetwork", "OpenAI request successful")
            val rawText = jsonResponse.choices.firstOrNull()?.message?.content ?: throw IllegalStateException("Empty response from Provider")
            return extractJson(rawText)
        } catch (e: ClientRequestException) {
            val err = "API Error ${e.response.status.value}"
            com.example.utils.AppLogger.e("AiNetwork", err, e)
            throw Exception(err)
        } catch (e: Exception) {
            val err = "Network Error: ${e.message}"
            com.example.utils.AppLogger.e("AiNetwork", err, e)
            throw Exception(err)
        }
    }

    
    private fun extractJson(rawText: String): String {
        val startIndex = rawText.indexOfFirst { it == '{' || it == '[' }
        val endIndex = rawText.indexOfLast { it == '}' || it == ']' }
        if (startIndex != -1 && endIndex != -1 && startIndex <= endIndex) {
            return rawText.substring(startIndex..endIndex)
        }
        return rawText.trim()
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
