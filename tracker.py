import sys

tracker = """package com.example.domain.services.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AiUsageStats(
    val requests: Int = 0,
    val estimatedTokens: Int = 0,
    val cacheHits: Int = 0,
    val rateLimitErrors: Int = 0
)

object AiUsageTracker {
    private val _stats = MutableStateFlow(AiUsageStats())
    val stats: StateFlow<AiUsageStats> = _stats.asStateFlow()

    fun trackRequest(tokens: Int) {
        _stats.update { it.copy(requests = it.requests + 1, estimatedTokens = it.estimatedTokens + tokens) }
    }

    fun trackCacheHit() {
        _stats.update { it.copy(cacheHits = it.cacheHits + 1) }
    }

    fun trackRateLimitError() {
        _stats.update { it.copy(rateLimitErrors = it.rateLimitErrors + 1) }
    }
}
"""

with open('/app/applet/app/src/main/java/com/example/domain/services/ai/AiUsageTracker.kt', 'w') as f:
    f.write(tracker)
print("Created Tracker")
