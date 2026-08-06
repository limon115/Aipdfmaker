package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val course: String,
    val chapter: String,
    val noteStyle: String,
    val outputFormat: String,
    val status: String,
    val pageCount: Int,
    val lastUpdated: Long,
    val sourceText: String = ""
)
