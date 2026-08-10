package com.example.domain.services.html

import com.example.domain.models.document.*
import timber.log.Timber

class JsonToHtmlConverter {
    fun convert(document: Document): String {
        Timber.d("JsonToHtmlConverter: Starting conversion for Document. Blocks count: %d", document.blocks.size)
        val htmlBuilder = java.lang.StringBuilder()
        htmlBuilder.append("<!DOCTYPE html>\n<html>\n<head>\n")
        htmlBuilder.append("<meta charset=\"UTF-8\">\n")
        htmlBuilder.append("<title>${document.title}</title>\n")
        
        // Inject KaTeX
        htmlBuilder.append("""
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css" integrity="sha384-GvrOXuhMATgEsSwCs4smul74iXGOixntILdUW9XmUC6+HX0sLNAK3q71bZlF0164" crossorigin="anonymous">
            <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js" integrity="sha384-cpW21h6RZv/phavutF+AuVYrr+dA8xD9zs6FwLpaCct6O9ctzYFfLe4211Ght389" crossorigin="anonymous"></script>
            <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js" integrity="sha384-+VBxd3r6XgURycqtZ117nYw44OOcIax56Z4dCRWbOxyj0O1rxVqTGbT099O5DMC" crossorigin="anonymous"
                onload="renderMathInElement(document.body);"></script>
        """.trimIndent())
        
        // Basic CSS
        htmlBuilder.append("""
            <style>
                body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; line-height: 1.6; margin: 40px; color: #333; }
                h1, h2, h3, h4, h5, h6 { color: #222; }
                h1 { border-bottom: 2px solid #eaecef; padding-bottom: .3em; }
                table { border-collapse: collapse; width: 100%; margin-bottom: 1rem; }
                th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                th { background-color: #f2f2f2; }
                blockquote { border-left: 4px solid #ccc; margin-left: 0; padding-left: 16px; color: #666; }
                .page-break { page-break-after: always; }
            </style>
        """.trimIndent())
        
        htmlBuilder.append("\n</head>\n<body>\n")
        
        if (document.title.isNotEmpty()) {
            htmlBuilder.append("<h1>${escapeHtml(document.title)}</h1>\n")
        }
        if (document.author.isNotEmpty()) {
            htmlBuilder.append("<p><em>By ${escapeHtml(document.author)}</em></p>\n")
        }
        
        for (block in document.blocks) {
            htmlBuilder.append(convertBlock(block))
            htmlBuilder.append("\n")
        }
        
        htmlBuilder.append("</body>\n</html>")
        Timber.d("JsonToHtmlConverter: Conversion complete")
        return htmlBuilder.toString()
    }
    
    private fun convertBlock(block: DocumentBlock): String {
        return when (block) {
            is HeadingBlock -> "<h${block.level}>${convertInline(block.text, block.content)}</h${block.level}>"
            is ParagraphBlock -> "<p>${convertInline(block.text, block.content)}</p>"
            is EquationBlock -> {
                if (block.display) "\\[ ${block.latex} \\]" else "\\( ${block.latex} \\)"
            }
            is BulletListBlock -> {
                val items = block.items.joinToString("\n") { "<li>${escapeHtml(it)}</li>" }
                "<ul>\n$items\n</ul>"
            }
            is NumberedListBlock -> {
                val items = block.items.joinToString("\n") { "<li>${escapeHtml(it)}</li>" }
                "<ol>\n$items\n</ol>"
            }
            is TableBlock -> {
                val builder = StringBuilder("<table>\n")
                if (block.columns.isNotEmpty()) {
                    builder.append("<thead>\n<tr>\n")
                    for (col in block.columns) {
                        builder.append("<th>${escapeHtml(col)}</th>\n")
                    }
                    builder.append("</tr>\n</thead>\n")
                }
                if (block.rows.isNotEmpty()) {
                    builder.append("<tbody>\n")
                    for (row in block.rows) {
                        builder.append("<tr>\n")
                        for (cell in row) {
                            builder.append("<td>${escapeHtml(cell)}</td>\n")
                        }
                        builder.append("</tr>\n")
                    }
                    builder.append("</tbody>\n")
                }
                builder.append("</table>")
                builder.toString()
            }
            is ImageBlock -> "<img src=\"${escapeHtml(block.path)}\" alt=\"Image\" style=\"max-width: 100%; height: auto;\" />"
            is QuoteBlock -> "<blockquote>${escapeHtml(block.text)}</blockquote>"
            is PageBreakBlock -> "<div class=\"page-break\"></div>"
        }
    }
    
    private fun convertInline(fallbackText: String, content: List<InlineElement>?): String {
        if (content == null || content.isEmpty()) {
            return escapeHtml(fallbackText)
        }
        val builder = StringBuilder()
        for (element in content) {
            when (element) {
                is TextElement -> builder.append(escapeHtml(element.value))
                is InlineMathElement -> builder.append("\\( ${element.latex} \\)")
            }
        }
        return builder.toString()
    }
    
    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#039;")
    }
}
