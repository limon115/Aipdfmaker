package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentSnippetDao {
    @Query("SELECT * FROM document_snippets WHERE projectId = :projectId ORDER BY orderIndex ASC")
    fun getSnippetsForProject(projectId: Int): Flow<List<DocumentSnippetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: DocumentSnippetEntity)

    @Query("SELECT * FROM document_snippets WHERE jsonContent = :content LIMIT 1")
    suspend fun findSnippetByContent(content: String): DocumentSnippetEntity?

    @Query("SELECT * FROM document_snippets WHERE projectId = :projectId LIMIT 1")
    suspend fun getFirstSnippetForProject(projectId: Int): DocumentSnippetEntity?
}
