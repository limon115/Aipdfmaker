import sys

# 1. AiResponseCache.kt
cache_code = """package com.example.data.cache

import android.content.Context
import java.io.File
import java.security.MessageDigest

class AiResponseCache(private val context: Context) {
    private val cacheDir = File(context.cacheDir, "ai_responses").apply { mkdirs() }

    fun get(prompt: String, systemPrompt: String?, model: String): String? {
        val key = generateKey(prompt, systemPrompt, model)
        val file = File(cacheDir, key)
        return if (file.exists()) file.readText() else null
    }

    fun put(prompt: String, systemPrompt: String?, model: String, response: String) {
        val key = generateKey(prompt, systemPrompt, model)
        val file = File(cacheDir, key)
        file.writeText(response)
    }

    private fun generateKey(prompt: String, systemPrompt: String?, model: String): String {
        val input = "$model|${systemPrompt ?: ""}|$prompt"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
"""
import os
os.makedirs('/app/applet/app/src/main/java/com/example/data/cache', exist_ok=True)
with open('/app/applet/app/src/main/java/com/example/data/cache/AiResponseCache.kt', 'w') as f:
    f.write(cache_code)


# 2. AiNetworkClient.kt
with open('/app/applet/app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    client_code = f.read()

old_rate_limit = """    companion object {
        private val requestMutex = Mutex()
        private val requestTimestamps = mutableListOf<Long>()
        private const val MAX_REQUESTS_PER_MINUTE = 14
        private const val ONE_MINUTE_MS = 60_000L
        
        suspend fun enforceRateLimit() {"""

new_rate_limit = """    companion object {
        private val requestMutex = Mutex()
        private val requestTimestamps = mutableListOf<Long>()
        private val tokenUsageHistory = mutableListOf<Pair<Long, Int>>()
        private const val MAX_REQUESTS_PER_MINUTE = 14
        private const val MAX_TOKENS_PER_MINUTE = 800_000
        private const val ONE_MINUTE_MS = 60_000L
        
        suspend fun enforceRateLimit(estimatedTokens: Int = 1000) {"""

client_code = client_code.replace(old_rate_limit, new_rate_limit)

old_limit_impl = """            requestMutex.withLock {
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
            }"""

new_limit_impl = """            requestMutex.withLock {
                var now = System.currentTimeMillis()
                requestTimestamps.removeAll { now - it > ONE_MINUTE_MS }
                tokenUsageHistory.removeAll { now - it.first > ONE_MINUTE_MS }
                
                var currentTokens = tokenUsageHistory.sumOf { it.second }
                while (currentTokens + estimatedTokens > MAX_TOKENS_PER_MINUTE) {
                    val oldestToken = tokenUsageHistory.firstOrNull()
                    if (oldestToken != null) {
                        val waitTime = ONE_MINUTE_MS - (System.currentTimeMillis() - oldestToken.first)
                        if (waitTime > 0) kotlinx.coroutines.delay(waitTime + 100)
                    } else {
                        break
                    }
                    val newNow = System.currentTimeMillis()
                    tokenUsageHistory.removeAll { newNow - it.first > ONE_MINUTE_MS }
                    currentTokens = tokenUsageHistory.sumOf { it.second }
                }

                now = System.currentTimeMillis()
                requestTimestamps.removeAll { now - it > ONE_MINUTE_MS }
                if (requestTimestamps.size >= MAX_REQUESTS_PER_MINUTE) {
                    val oldest = requestTimestamps.first()
                    val waitTime = ONE_MINUTE_MS - (System.currentTimeMillis() - oldest)
                    if (waitTime > 0) {
                        kotlinx.coroutines.delay(waitTime + 100)
                    }
                }
                
                val newNow2 = System.currentTimeMillis()
                val timeSinceLast = newNow2 - (requestTimestamps.lastOrNull() ?: 0L)
                if (timeSinceLast < 4000L) {
                    kotlinx.coroutines.delay(4000L - timeSinceLast)
                }
                
                val finalNow = System.currentTimeMillis()
                requestTimestamps.add(finalNow)
                tokenUsageHistory.add(Pair(finalNow, estimatedTokens))
            }"""

client_code = client_code.replace(old_limit_impl, new_limit_impl)

client_code = client_code.replace(
    "private suspend fun sendGeminiRequest(cleanKey: String, prompt: String, sysPrompt: String? = null, mimeType: String? = null): String {\n        enforceRateLimit()",
    "private suspend fun sendGeminiRequest(cleanKey: String, prompt: String, sysPrompt: String? = null, mimeType: String? = null): String {\n        val estimatedTokens = (prompt.length + (sysPrompt?.length ?: 0)) / 4\n        enforceRateLimit(estimatedTokens)"
)

client_code = client_code.replace(
    "private suspend fun sendKtorRequest(baseUrl: String, cleanKey: String, reqModel: String, messages: List<OpenAiMessage>, temp: Float, maxTokens: Int? = null): String {\n        enforceRateLimit()",
    "private suspend fun sendKtorRequest(baseUrl: String, cleanKey: String, reqModel: String, messages: List<OpenAiMessage>, temp: Float, maxTokens: Int? = null): String {\n        val estimatedTokens = messages.sumOf { (it.content ?: \"\").length } / 4\n        enforceRateLimit(estimatedTokens)"
)

with open('/app/applet/app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'w') as f:
    f.write(client_code)


# 3. NoteGenerationService.kt
with open('/app/applet/app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt', 'r') as f:
    service_code = f.read()

service_code = service_code.replace(
    "class NoteGenerationService(\n    private val aiClient: AiNetworkClient\n)",
    "import com.example.data.cache.AiResponseCache\n\nclass NoteGenerationService(\n    private val aiClient: AiNetworkClient,\n    private val cache: AiResponseCache\n)"
)

old_gen = """        val rawResponse = clientForGeneration.generateContent(prompt, systemPrompt)
        
        return rawResponse.trim()
            .removePrefix("```html")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()"""

new_gen = """        val cachedResponse = cache.get(prompt, systemPrompt, ai2Model)
        if (cachedResponse != null) {
            return cleanHtml(cachedResponse)
        }

        val rawResponse = clientForGeneration.generateContent(prompt, systemPrompt)
        cache.put(prompt, systemPrompt, ai2Model, rawResponse)
        
        return cleanHtml(rawResponse)
    }

    private fun cleanHtml(raw: String): String {
        return raw.trim()
            .removePrefix("```html")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()"""

service_code = service_code.replace(old_gen, new_gen)

with open('/app/applet/app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt', 'w') as f:
    f.write(service_code)


# 4. NoteGenerationWorker.kt
with open('/app/applet/app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt', 'r') as f:
    worker = f.read()

worker = worker.replace("import com.example.domain.services.ai.TopicContextRetriever", "import com.example.domain.services.ai.TopicContextRetriever\nimport com.example.data.cache.AiResponseCache")

worker = worker.replace(
    "val service = NoteGenerationService(dummyClient)",
    "val cache = AiResponseCache(context)\n        val service = NoteGenerationService(dummyClient, cache)"
)

old_loop_start = """            val chunks = chunker.chunkText(sourceText, 3000, 300)
            
            blueprint.topics.forEachIndexed { index, topic ->"""

new_loop_start = """            val chunks = chunker.chunkText(sourceText, 3000, 300)
            
            val existingSnippets = snippetDao.getSnippetsForProject(projectId).first()
            val completedTopics = existingSnippets.map { it.topicTitle }.toSet()
            
            blueprint.topics.forEachIndexed { index, topic ->
                if (completedTopics.contains(topic.title)) {
                    // Skip already generated topic (Resumability)
                    setProgress(workDataOf(PROGRESS to index, TOTAL to totalTopics, CURRENT_TOPIC to "${topic.title} (Cached)"))
                    return@forEachIndexed
                }
"""

worker = worker.replace(old_loop_start, new_loop_start)

with open('/app/applet/app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt', 'w') as f:
    f.write(worker)

print("Updates applied")
