package com.example.domain.services.html

import com.example.data.database.DocumentSnippetDao
import com.example.domain.models.document.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class DocumentMergeEngine(private val documentSnippetDao: DocumentSnippetDao) {
    suspend fun generateMasterJson(projectId: Int, projectTitle: String): String {
        val snippets = documentSnippetDao.getSnippetsForProject(projectId).first()
        val jsonFormat = Json { ignoreUnknownKeys = true; classDiscriminator = "type"; isLenient = true }
        val mergedBlocks = mutableListOf<DocumentBlock>()

        for (snippet in snippets) {
            val rawJson = snippet.jsonContent.trim()
            try {
                // Try parsing as full Document
                val snippetDoc = jsonFormat.decodeFromString<Document>(rawJson)
                mergedBlocks.addAll(snippetDoc.blocks)
            } catch (e: Exception) {
                try {
                    // Fallback 1: Maybe it is an array of blocks?
                    val blockList = jsonFormat.decodeFromString<List<DocumentBlock>>(rawJson)
                    mergedBlocks.addAll(blockList)
                } catch (e2: Exception) {
                    // 🛡️ THE FIX: If parsing completely fails, clean the JSON symbols out!
                    val cleanText = rawJson.replace(Regex("[{}\"\\[\\]]"), "").trim()
                    mergedBlocks.add(ParagraphBlock(text = "Recovered Text: $cleanText"))
                }
            }
        }

        val masterDocument = Document(title = projectTitle, blocks = mergedBlocks)
        return jsonFormat.encodeToString(masterDocument)
    }
}
