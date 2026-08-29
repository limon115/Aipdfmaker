package com.example.domain.services.ai

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AiUsageStats(
    val requests: Int = 0,
    val estimatedTokens: Int = 0,
    val cacheHits: Int = 0,
    val rateLimitErrors: Int = 0,
    val tokensByFeature: Map<String, Int> = emptyMap(),
    val requestsByFeature: Map<String, Int> = emptyMap()
)

object AiUsageTracker {
    private val _stats = MutableStateFlow(AiUsageStats())
    val stats: StateFlow<AiUsageStats> = _stats.asStateFlow()

    fun trackRequest(feature: String, tokens: Int) {
        _stats.update { current ->
            current.copy(
                requests = current.requests + 1,
                estimatedTokens = current.estimatedTokens + tokens,
                tokensByFeature = current.tokensByFeature.toMutableMap().apply { put(feature, (get(feature) ?: 0) + tokens) },
                requestsByFeature = current.requestsByFeature.toMutableMap().apply { put(feature, (get(feature) ?: 0) + 1) }
            )
        }
    }

    fun trackCacheHit() {
        _stats.update { it.copy(cacheHits = it.cacheHits + 1) }
    }

    fun trackRateLimitError() {
        _stats.update { it.copy(rateLimitErrors = it.rateLimitErrors + 1) }
    }
}
