package com.example.domain.services.ai

import com.example.data.network.AiNetworkClient

class NoteGenerationService(
    private val aiClient: AiNetworkClient
) {
    suspend fun generateHtmlForTopic(
        topicTitle: String,
        blueprintContext: String,
        sourceText: String,
        ai2Provider: String,
        ai2Model: String,
        ai2ApiKey: String,
        ai2Temperature: Float
    ): String {
        // Create an AiNetworkClient specifically for AI #2
        val clientForGeneration = AiNetworkClient(
            provider = ai2Provider,
            apiKey = ai2ApiKey,
            model = ai2Model,
            temperature = ai2Temperature
        )

        val systemPrompt = """
            You are an expert educational writer. Write detailed study notes for the topic: '$topicTitle'. Use the provided source text. You MUST return ONLY valid, semantic HTML5 code (using <section>, <h2>, <h3>, <p>, <ul>, <strong>, <table>). Do NOT wrap the response in markdown blocks (like ```html). Do NOT include <html> or <body> tags, only the inner content.
        """.trimIndent()

        val prompt = """
            BLUEPRINT CONTEXT:
            $blueprintContext
            
            SOURCE TEXT:
            $sourceText
        """.trimIndent()

        val rawResponse = clientForGeneration.generateContent(prompt, systemPrompt)
        
        return rawResponse.trim()
            .removePrefix("```html")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
