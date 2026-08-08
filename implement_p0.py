import os

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
                val lastNewline = text.lastIndexOf('\n', endIndex)
                val lastPeriod = text.lastIndexOf('.', endIndex)
                
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

# 2. TopicContextRetriever
retriever_code = """package com.example.domain.services.ai

class TopicContextRetriever {
    fun retrieveContext(topicTitle: String, chunks: List<SourceChunk>, maxTokens: Int = 6000): String {
        // Very basic keyword matching for now
        val keywords = topicTitle.lowercase().split(Regex("\\\\s+")).filter { it.length > 3 }
        
        val scoredChunks = chunks.map { chunk ->
            val lowerText = chunk.text.lowercase()
            var score = 0
            for (keyword in keywords) {
                var index = lowerText.indexOf(keyword)
                while (index >= 0) {
                    score++
                    index = lowerText.indexOf(keyword, index + keyword.length)
                }
            }
            Pair(chunk, score)
        }
        
        // If no keywords found, maybe just return the first few chunks or spread evenly
        val sortedChunks = if (scoredChunks.all { it.second == 0 }) {
            chunks.take((maxTokens / 2000).coerceAtLeast(1)).map { Pair(it, 1) }
        } else {
            scoredChunks.sortedByDescending { it.second }.filter { it.second > 0 }
        }
        
        val selectedChunks = mutableListOf<SourceChunk>()
        var currentTokens = 0
        
        for ((chunk, _) in sortedChunks) {
            if (currentTokens + chunk.estimatedTokens <= maxTokens) {
                selectedChunks.add(chunk)
                currentTokens += chunk.estimatedTokens
            } else {
                break
            }
        }
        
        // Sort back by original order for readability
        return selectedChunks.sortedBy { it.startOffset }.joinToString("\\n\\n...\\n\\n") { it.text }
    }
}
"""

with open('/app/applet/app/src/main/java/com/example/domain/services/ai/TopicContextRetriever.kt', 'w') as f:
    f.write(retriever_code)

print("Created TextChunker and TopicContextRetriever")
