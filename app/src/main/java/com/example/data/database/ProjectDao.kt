package com.example.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastUpdated DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Query("SELECT * FROM html_snippets WHERE projectId = :projectId ORDER BY displayOrder ASC")
    suspend fun getHtmlSnippetsForProject(projectId: Int): List<HtmlSnippetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHtmlSnippet(snippet: HtmlSnippetEntity)
}
