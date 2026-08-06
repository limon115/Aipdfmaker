package com.example.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class BlueprintSummary(
    val courseName: String,
    val chapterName: String,
    val topicCount: Int,
    val formulaCount: Int,
    val definitionCount: Int,
    val exampleCount: Int,
    val diagramCount: Int,
    val examTipCount: Int
)
