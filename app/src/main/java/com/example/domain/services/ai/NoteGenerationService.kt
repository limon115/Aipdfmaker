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
            You are an expert textbook author and university professor. Write EXHAUSTIVE, rigorous study notes for the topic: '$topicTitle'.
            
            CRITICAL PEDAGOGY RULES:
            1. Use fundamental core formulas and first-principles derivations. NO derived memory tricks unless explicitly stated as a helpful shortcut after the proof.
            2. Keep the math strictly separate and clean from descriptions. State the procedural steps explicitly BEFORE doing the math.
            3. Write exhaustive explanations with college-level depth.

            CRITICAL LATEX RULES:
            1. Return ONLY valid LaTeX code for the document body. Do NOT include \documentclass or \begin{document}.
            2. ALWAYS include relevant visual aids if applicable:
               - Use \usepackage{tikz} or \usepackage{pgfplots} for math graphs and geometry.
               - Use \usepackage{circuitikz} for physics circuit diagrams.
               - Use \usepackage[version=4]{mhchem} for chemical equations.
            3. Format tables using \begin{table} and \toprule, \midrule, \bottomrule from the booktabs package.
            
            LANGUAGE RULE: You MUST write the entire output in the EXACT SAME LANGUAGE as the provided source text.
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
