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
            var safeJson = snippet.jsonContent.trim()

            // 🛡️ SURGICAL FIX 1: Auto-correct the AI hallucinating "value" instead of "latex"
            safeJson = safeJson.replace(Regex("\"type\"\\s*:\\s*\"inline_math\"\\s*,\\s*\"value\""), "\"type\":\"inline_math\",\"latex\"")
            
            // 🛡️ SURGICAL FIX 2: Auto-escape LaTeX commands so \f and \t don't crash the JSON parser
            val latexCommands = listOf("\\frac", "\\times", "\\text", "\\rightarrow", "\\leftarrow", "\\approx", "\\mu", "\\Delta", "\\[", "\\]", "\\{", "\\}", "\\sigma", "\\alpha", "\\quad", "\\implies")
            for (cmd in latexCommands) {
                safeJson = safeJson.replace(cmd, "\\" + cmd)
            }

            try {
                // Try parsing as full Document
                val snippetDoc = jsonFormat.decodeFromString<Document>(safeJson)
                mergedBlocks.addAll(snippetDoc.blocks)
            } catch (e: Exception) {
                try {
                    // Fallback 1: Maybe it is an array of blocks?
                    val blockList = jsonFormat.decodeFromString<List<DocumentBlock>>(safeJson)
                    mergedBlocks.addAll(blockList)
                } catch (e2: Exception) {
                    // 🛡️ SURGICAL FIX 3: Intelligent Regex Extraction
                    // Do not dump raw schema text. Extract only the readable content.
                    val contentRegex = Regex("\"(?:text|latex|value)\"\s*:\s*\"(.*?)\"")
                    val matches = contentRegex.findAll(safeJson)
                    
                    if (matches.any()) {
                        val extracted = matches.joinToString(" ") { it.groupValues[1] }
                        mergedBlocks.add(ParagraphBlock(text = extracted))
                    } else {
                        mergedBlocks.add(ParagraphBlock(text = "⚠️ [Data Extraction Error] The AI generated invalid mathematical JSON that could not be parsed."))
                    }
                }\"\\[\\]]"), "").trim()
                    mergedBlocks.add(ParagraphBlock(text = "Recovered Text: $cleanText"))
                }
            }
        }

        val masterDocument = Document(title = projectTitle, blocks = mergedBlocks)
        return jsonFormat.encodeToString(masterDocument)
    }
}
