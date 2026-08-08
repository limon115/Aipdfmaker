import sys

# 1. Update NoteGenerationService.kt
service_code = """package com.example.domain.services.ai

import com.example.data.network.AiNetworkClient

class NoteGenerationService(
    private val aiClient: AiNetworkClient
) {
    suspend fun generateHtmlForTopic(
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
        val systemPrompt = \"\"\"
            You are an expert educational writer. Write detailed study notes for the topic: '$topicTitle'. Use the provided source text. 
            CRITICAL LANGUAGE RULE: You MUST write the entire output, headings, explanations, and terms in the EXACT SAME LANGUAGE as the provided source text (e.g., if the source text is in Bangla, write the notes entirely in fluent, academic Bangla).
            You MUST return ONLY valid, semantic HTML5 code (using <section>, <h2>, <h3>, <p>, <ul>, <strong>, <table>). Do NOT wrap the response in markdown blocks (like ```html). Do NOT include <html> or <body> tags, only the inner content.
        \"\"\".trimIndent()

        val prompt = \"\"\"
            BLUEPRINT CONTEXT:
            $blueprintContext
            
            RELEVANT SOURCE TEXT FOR THIS TOPIC:
            $relevantContext
        \"\"\".trimIndent()

        val rawResponse = clientForGeneration.generateContent(prompt, systemPrompt)
        
        return rawResponse.trim()
            .removePrefix("```html")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
"""

with open('/app/applet/app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt', 'w') as f:
    f.write(service_code)


# 2. Update NoteGenerationWorker.kt
with open('/app/applet/app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt', 'r') as f:
    worker = f.read()

# Add imports for chunking
if "import com.example.domain.services.ai.TextChunker" not in worker:
    worker = worker.replace("import com.example.domain.services.ai.NoteGenerationService", "import com.example.domain.services.ai.NoteGenerationService\nimport com.example.domain.services.ai.TextChunker\nimport com.example.domain.services.ai.TopicContextRetriever")

# Fix loop in worker
old_loop = """            setForeground(createForegroundInfo("Starting generation...", 0, totalTopics))
            
            blueprint.topics.forEachIndexed { index, topic ->
                setProgress(workDataOf(PROGRESS to index, TOTAL to totalTopics, CURRENT_TOPIC to topic.title))
                setForeground(createForegroundInfo("Generating: ${topic.title}", index, totalTopics))
                
                val html = service.generateHtmlForTopic(
                    topicTitle = topic.title,
                    blueprintContext = blueprintJson,
                    sourceText = sourceText,
                    ai2Provider = settings.ai2Provider.name,
                    ai2Model = settings.ai2Model.ifBlank { "gemini-2.5-flash" },
                    ai2ApiKey = settings.ai2ApiKey,
                    ai2Temperature = settings.ai2Temperature
                )"""

new_loop = """            setForeground(createForegroundInfo("Starting generation...", 0, totalTopics))
            
            val chunker = TextChunker()
            val retriever = TopicContextRetriever()
            val chunks = chunker.chunkText(sourceText, 3000, 300) // Chunk document once!
            
            blueprint.topics.forEachIndexed { index, topic ->
                setProgress(workDataOf(PROGRESS to index, TOTAL to totalTopics, CURRENT_TOPIC to topic.title))
                setForeground(createForegroundInfo("Generating: ${topic.title}", index, totalTopics))
                
                // Retrieve only relevant chunks for this topic
                val relevantContext = retriever.retrieveContext(topic.title, chunks, 8000)
                
                val html = service.generateHtmlForTopic(
                    topicTitle = topic.title,
                    blueprintContext = blueprintJson,
                    relevantContext = relevantContext,
                    ai2Provider = settings.ai2Provider.name,
                    ai2Model = settings.ai2Model.ifBlank { "gemini-2.5-flash" },
                    ai2ApiKey = settings.ai2ApiKey,
                    ai2Temperature = settings.ai2Temperature
                )"""

worker = worker.replace(old_loop, new_loop)

with open('/app/applet/app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt', 'w') as f:
    f.write(worker)

print("Updated NoteGenerationWorker and NoteGenerationService")
