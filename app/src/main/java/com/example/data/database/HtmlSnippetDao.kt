package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HtmlSnippetDao {
    @Query("SELECT * FROM html_snippets WHERE projectId = :projectId ORDER BY orderIndex ASC")
    fun getSnippetsForProject(projectId: Int): Flow<List<HtmlSnippetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnippet(snippet: HtmlSnippetEntity)
}
