package com.example.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class BlueprintSummary(
    val courseName: String,
    val chapterName: String,
    val topics: List<Topic> = emptyList(),
    val formulaCount: Int = 0,
    val definitionCount: Int = 0,
    val exampleCount: Int = 0,
    val diagramCount: Int = 0,
    val examTipCount: Int = 0
)

@Serializable
data class Topic(
    val title: String,
    val durationMinutes: Int = 0
)
