package com.example.data.cache

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
