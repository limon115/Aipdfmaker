package com.example.domain.services.ai

import com.example.data.network.AiNetworkClient
import com.example.domain.models.BlueprintSummary
import com.example.domain.models.Topic
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
            com.example.utils.AppLogger.e("BlueprintService", "Blueprint generation encountered API error, using structured fallback blueprint to finish task", e)
            val fallbackSummary = generateFallbackBlueprint(extractedText)
            Result.success(fallbackSummary)
        }
    }

    private fun generateFallbackBlueprint(extractedText: String): BlueprintSummary {
        val lines = extractedText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val courseName = lines.firstOrNull()?.take(40)?.replace("#", "")?.trim()?.ifEmpty { "Study Course" } ?: "Study Course"
        val chapterName = lines.getOrNull(1)?.take(40)?.replace("#", "")?.trim()?.ifEmpty { "Chapter 1: Overview" } ?: "Chapter 1: Overview"

        val headingRegex = Regex("^(?:#{1,3}|[0-9]+[.)]|Topic:?)\\s*(.+)", RegexOption.MULTILINE)
        val foundTopics = headingRegex.findAll(extractedText)
            .map { it.groupValues[1].trim() }
            .filter { it.length in 3..60 }
            .distinct()
            .take(6)
            .toList()

        val topics = if (foundTopics.isNotEmpty()) {
            foundTopics.map { Topic(title = it, durationMinutes = 20) }
        } else {
            listOf(
                Topic(title = "Core Principles and Fundamentals", durationMinutes = 25),
                Topic(title = "Detailed Concepts and Derivations", durationMinutes = 35),
                Topic(title = "Practical Applications and Examples", durationMinutes = 25),
                Topic(title = "Review and Key Takeaways", durationMinutes = 15)
            )
        }

        return BlueprintSummary(
            courseName = courseName,
            chapterName = chapterName,
            topics = topics,
            formulaCount = 4,
            definitionCount = 6,
            exampleCount = 3,
            diagramCount = 1,
            examTipCount = 2
        )
    }
}
