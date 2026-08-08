import sys

# 1. TextChunker
chunker_code = """package com.example.domain.services.ai

data class SourceChunk(
    val id: String,
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
    val sectionTitle: String?,
    val estimatedTokens: Int
)

class TextChunker {
    fun chunkText(text: String, maxTokensPerChunk: Int = 2000, overlapTokens: Int = 200): List<SourceChunk> {
        // Simple heuristic: 1 token ~= 4 characters
        val maxChars = maxTokensPerChunk * 4
        val overlapChars = overlapTokens * 4
        
        val chunks = mutableListOf<SourceChunk>()
        var currentIndex = 0
        var chunkId = 0
        
        while (currentIndex < text.length) {
            var endIndex = currentIndex + maxChars
            if (endIndex > text.length) {
                endIndex = text.length
            } else {
                // Try to find a paragraph break or sentence break to avoid cutting mid-sentence
                val lastNewline = text.lastIndexOf("\\n", endIndex)
                val lastPeriod = text.lastIndexOf(".", endIndex)
                
                if (lastNewline > currentIndex + maxChars / 2) {
                    endIndex = lastNewline + 1
                } else if (lastPeriod > currentIndex + maxChars / 2) {
                    endIndex = lastPeriod + 1
                }
            }
            
            val chunkText = text.substring(currentIndex, endIndex)
            val estimatedTokens = chunkText.length / 4
            
            chunks.add(
                SourceChunk(
                    id = "chunk_$chunkId",
                    text = chunkText,
                    startOffset = currentIndex,
                    endOffset = endIndex,
                    sectionTitle = null,
                    estimatedTokens = estimatedTokens
                )
            )
            
            chunkId++
            currentIndex = endIndex - overlapChars
            if (currentIndex < 0 || endIndex == text.length) {
                if (endIndex == text.length) break
                currentIndex = 0
            }
        }
        
        return chunks
    }
}
"""

with open('/app/applet/app/src/main/java/com/example/domain/services/ai/TextChunker.kt', 'w') as f:
    f.write(chunker_code)


with open('/app/applet/app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt', 'r') as f:
    worker = f.read()

# Make sure we don't have multiple 'relevantContext =' if it's already there. Let's just do a clean replace for the loop.
# I will use a simple regex or just string find/replace on the exact service.generateHtmlForTopic call.

old_call_1 = """                val html = service.generateHtmlForTopic(
                    topicTitle = topic.title,
                    blueprintContext = blueprintJson,
                    relevantContext = relevantContext,
                    ai2Provider = settings.ai2Provider.name,
                    ai2Model = settings.ai2Model.ifBlank { "gemini-2.5-flash" },
                    ai2ApiKey = settings.ai2ApiKey,
                    ai2Temperature = settings.ai2Temperature
                )"""

old_call_2 = """                val html = service.generateHtmlForTopic(
                    topicTitle = topic.title,
                    blueprintContext = blueprintJson,
                    relevantContext = sourceText,
                    ai2Provider = settings.ai2Provider.name,
                    ai2Model = settings.ai2Model.ifBlank { "gemini-2.5-flash" },
                    ai2ApiKey = settings.ai2ApiKey,
                    ai2Temperature = settings.ai2Temperature
                )"""
                
old_call_3 = """                val html = service.generateHtmlForTopic(
                    topicTitle = topic.title,
                    blueprintContext = blueprintJson,
                    sourceText = sourceText,
                    ai2Provider = settings.ai2Provider.name,
                    ai2Model = settings.ai2Model.ifBlank { "gemini-2.5-flash" },
                    ai2ApiKey = settings.ai2ApiKey,
                    ai2Temperature = settings.ai2Temperature
                )"""

new_call = """                // Retrieve only relevant chunks for this topic
                val relevantContextForTopic = retriever.retrieveContext(topic.title, chunks, 8000)
                
                val html = service.generateHtmlForTopic(
                    topicTitle = topic.title,
                    blueprintContext = blueprintJson,
                    relevantContext = relevantContextForTopic,
                    ai2Provider = settings.ai2Provider.name,
                    ai2Model = settings.ai2Model.ifBlank { "gemini-2.5-flash" },
                    ai2ApiKey = settings.ai2ApiKey,
                    ai2Temperature = settings.ai2Temperature
                )"""

# Also ensure chunks are generated
worker = worker.replace("val relevantContext = retriever.retrieveContext(topic.title, chunks, 8000)", "")

worker = worker.replace(old_call_1, new_call).replace(old_call_2, new_call).replace(old_call_3, new_call)

with open('/app/applet/app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt', 'w') as f:
    f.write(worker)

print("Fixed all")
