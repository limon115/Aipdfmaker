package com.example.domain.services.ai

class TopicContextRetriever {
    fun retrieveContext(topicTitle: String, chunks: List<SourceChunk>, maxTokens: Int = 6000): String {
        // Very basic keyword matching for now
        val keywords = topicTitle.lowercase().split(Regex("\\s+")).filter { it.length > 3 }
        
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
        return selectedChunks.sortedBy { it.startOffset }.joinToString("\n\n...\n\n") { it.text }
    }
}
