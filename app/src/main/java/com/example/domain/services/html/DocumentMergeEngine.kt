package com.example.domain.services.html

import com.example.data.database.DocumentSnippetDao
import com.example.domain.models.document.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class DocumentMergeEngine(private val documentSnippetDao: DocumentSnippetDao) {
    suspend fun generateMasterJson(projectId: Int, projectTitle: String): String {
        val snippets = documentSnippetDao.getSnippetsForProject(projectId).first()
        
        val jsonFormat = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }
        val mergedBlocks = mutableListOf<DocumentBlock>()
        
        for (snippet in snippets) {
            try {
                val snippetDoc = jsonFormat.decodeFromString<Document>(snippet.jsonContent)
                mergedBlocks.addAll(snippetDoc.blocks)
            } catch (e: Exception) {
                e.printStackTrace()
                // If it fails, fallback to simple text
                mergedBlocks.add(ParagraphBlock(text = snippet.jsonContent))
            }
        }
        
        val masterDocument = Document(
            title = projectTitle,
            blocks = mergedBlocks
        )
        
        return jsonFormat.encodeToString(masterDocument)
    }
}
