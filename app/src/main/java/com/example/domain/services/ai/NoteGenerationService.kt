package com.example.domain.services.ai

import com.example.data.network.AiNetworkClient

import com.example.data.cache.AiResponseCache

class NoteGenerationService(
    private val aiClient: AiNetworkClient,
    private val cache: AiResponseCache
) {
    suspend fun generateDocumentForTopic(
        topicTitle: String,
        blueprintContext: String,
        relevantContext: String,
        ai2Provider: String,
        ai2Model: String,
        ai2ApiKey: String,
        ai2Temperature: Float
    ): String {
        val clientForGeneration = AiNetworkClient(
            provider = ai2Provider,
            apiKey = ai2ApiKey,
            model = ai2Model,
            temperature = ai2Temperature
        )
        val systemPrompt = """
            You are an expert educational writer. Write detailed study notes for the topic: '$topicTitle'. Use the provided source text. 
            CRITICAL LANGUAGE RULE: You MUST write the entire output, headings, explanations, and terms in the EXACT SAME LANGUAGE as the provided source text (e.g., if the source text is in Bangla, write the notes entirely in fluent, academic Bangla).
            You MUST return ONLY valid JSON matching the following schema. Do not use Markdown. Do not use HTML. Do not wrap the JSON in ```json fences.
            CRITICAL JSON RULES:
            1. For "inline_math", you MUST use the key "latex", NEVER "value".
            2. You MUST double-escape ALL backslashes in LaTeX equations so the JSON does not crash (e.g., write \\frac instead of \frac).

            {
              "schemaVersion": 1,
              "title": "Document Title",
              "author": "",
              "language": "bn",
              "blocks": [
                {
                  "type": "heading",
                  "level": 1,
                  "text": "Heading Text"
                },
                {
                  "type": "paragraph",
                  "content": [
                    { "type": "text", "value": "Regular text " },
                    { "type": "inline_math", "latex": "n = \\frac{w}{M}" }
                  ]
                },
                {
                  "type": "equation",
                  "latex": "E = mc^2",
                  "display": true
                },
                {
                  "type": "bullet_list",
                  "items": [
                    "Item 1",
                    "Item 2"
                  ]
                },
                {
                  "type": "table",
                  "columns": [ "Col 1", "Col 2" ],
                  "rows": [ [ "Val 1", "Val 2" ] ]
                }
              ]
            }
        """.trimIndent()

        val prompt = """
            BLUEPRINT CONTEXT:
            $blueprintContext
            
            RELEVANT SOURCE TEXT FOR THIS TOPIC:
            $relevantContext
        """.trimIndent()

        val cachedResponse = cache.get(prompt, systemPrompt, ai2Model)
        if (cachedResponse != null) {
            com.example.domain.services.ai.AiUsageTracker.trackCacheHit()
            return cleanJson(cachedResponse)
        }
        
        com.example.domain.services.ai.AiUsageTracker.trackRequest((prompt.length + systemPrompt.length) / 4)
        val rawResponse = clientForGeneration.generateContent(prompt, systemPrompt, "application/json", true)
        cache.put(prompt, systemPrompt, ai2Model, rawResponse)
        
        return cleanJson(rawResponse)
    }

    private fun cleanJson(raw: String): String {
        return raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
