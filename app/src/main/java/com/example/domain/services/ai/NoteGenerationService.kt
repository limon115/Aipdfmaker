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
            CRITICAL LANGUAGE RULE: You MUST write the entire output, headings, explanations, and terms in the EXACT SAME LANGUAGE as the provided source text.
            You MUST return ONLY valid LaTeX (.tex) code. Do not use Markdown. Do not wrap the LaTeX in ```latex fences.
            CRITICAL LATEX RULES:
            1. Provide ONLY the document body content, assuming it will be included in a larger LaTeX document. Do NOT include \documentclass or \begin{document}.
            2. Use standard LaTeX commands like \section, \subsection, \textbf, \textit, \begin{itemize}, \begin{equation}, etc.
            3. Ensure all equations are properly formatted in LaTeX math mode ($$ or \begin{equation}).
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
            return cleanLatex(cachedResponse)
        }
        
        com.example.domain.services.ai.AiUsageTracker.trackRequest((prompt.length + systemPrompt.length) / 4)
        val rawResponse = try {
            clientForGeneration.generateContent(prompt, systemPrompt, "text/plain", true)
        } catch (e: Exception) {
            e.printStackTrace()
            com.example.utils.AppLogger.e("NoteGenerationService", "Using mock note due to error", e)
            """
            \section{Mock Notes for $topicTitle}
            This is a mock note generated because the AI API call failed or the API key was invalid.
            """.trimIndent()
        }

        cache.put(prompt, systemPrompt, ai2Model, rawResponse)
        
        return cleanLatex(rawResponse)
    }

    private fun cleanLatex(raw: String): String {
        return raw.trim()
            .removePrefix("```latex")
            .removePrefix("```tex")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
