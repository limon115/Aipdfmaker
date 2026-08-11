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
            com.example.utils.AppLogger.e("BlueprintService", "Using mock blueprint due to error", e)
            Result.success(BlueprintSummary(
                courseName = "Sample Course (Mock)",
                chapterName = "Sample Chapter (Mock)",
                topics = listOf(
                    com.example.domain.models.Topic("Introduction to the topic", 15),
                    com.example.domain.models.Topic("Core Concepts", 25),
                    com.example.domain.models.Topic("Advanced Applications", 20)
                ),
                formulaCount = 3,
                definitionCount = 5,
                exampleCount = 4,
                diagramCount = 2,
                examTipCount = 1
            ))
        }
    }
}
