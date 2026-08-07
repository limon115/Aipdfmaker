package com.example.domain.services.html

import com.example.data.database.HtmlSnippetDao
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import kotlinx.coroutines.flow.first

class HtmlMergeEngine(private val htmlSnippetDao: HtmlSnippetDao) {

    suspend fun generateMasterHtml(projectId: Int): String {
        val snippets = htmlSnippetDao.getSnippetsForProject(projectId).first()
        
        val doc: Document = Jsoup.parse("<html><head><meta charset=\"UTF-8\"></head><body></body></html>")
        
        doc.head().append("""
            <style>
                body {
                    font-family: 'Times New Roman', serif;
                    line-height: 1.6;
                    margin: 40px auto;
                    max-width: 800px;
                    color: #333;
                    padding: 0 20px;
                }
                h1, h2, h3, h4 {
                    color: #2c3e50;
                    margin-top: 1.5em;
                    margin-bottom: 0.5em;
                }
                p {
                    margin-bottom: 1em;
                }
                ul, ol {
                    margin-bottom: 1em;
                }
            </style>
        """.trimIndent())
        
        val body = doc.body()
        for (snippet in snippets) {
            body.append(snippet.htmlContent)
        }
        
        return doc.outerHtml()
    }
}
