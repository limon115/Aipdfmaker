package com.example.domain.services.ai

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
                val lastNewline = text.lastIndexOf("\n", endIndex)
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
