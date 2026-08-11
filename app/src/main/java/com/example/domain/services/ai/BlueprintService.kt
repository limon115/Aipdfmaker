package com.example.domain.services.ai

import com.example.data.network.AiNetworkClient
import com.example.domain.models.BlueprintSummary
import kotlinx.serialization.json.Json

class BlueprintService(
    private val aiClient: AiNetworkClient
) {
    suspend fun generateBlueprint(extractedText: String): Result<BlueprintSummary> {
        return try {
            val rawJson = aiClient.generateBlueprint(extractedText)
            
            // Clean up the JSON in case the model ignored instructions and wrapped it in markdown
            val cleanedJson = rawJson.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

            val summary = jsonConfig.decodeFromString<BlueprintSummary>(cleanedJson)
            Result.success(summary)
        } catch (e: Exception) {
            e.printStackTrace()
            com.example.utils.AppLogger.e("BlueprintService", "Blueprint generation failed", e)
            Result.failure(e)
        }
    }
}
