package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "document_snippets")
data class DocumentSnippetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val topicTitle: String,
    val jsonContent: String,
    val orderIndex: Int
)
