package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "html_snippets")
data class HtmlSnippetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val topicTitle: String,
    val htmlContent: String,
    val orderIndex: Int
)
