package com.example.domain.services.html

import com.example.domain.models.document.*

class JsonToHtmlConverter {
    fun convert(document: Document): String {
        val htmlBuilder = java.lang.StringBuilder()
        htmlBuilder.append("<!DOCTYPE html>\n<html>\n<head>\n")
        htmlBuilder.append("<meta charset=\"UTF-8\">\n")
        htmlBuilder.append("<title>${document.title}</title>\n")

        // Inject KaTeX
        htmlBuilder.append("""
            <link rel="stylesheet" href="file:///android_asset/katex/katex.min.css">
            <script defer src="file:///android_asset/katex/katex.min.js"></script>
            <script defer src="file:///android_asset/katex/contrib/mhchem.min.js"></script>
            <script defer src="file:///android_asset/katex/contrib/auto-render.min.js"
                onload="renderMathInElement(document.body, {
                    throwOnError: false,
                    errorCallback: function(msg, err) { console.error(msg, err); },
                    delimiters: [
                        {left: '$$', right: '$$', display: true},
                        {left: '\[', right: '\]', display: true},
                        {left: '\(', right: '\)', display: false}
                    ]
                });
                document.body.setAttribute('data-render-complete', 'true');"></script>
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
                
                @page { size: A4; margin: 20mm 15mm; }
                table, tr, img, blockquote, figure, .katex-display {
                    break-inside: avoid;
                    page-break-inside: avoid;
                }
                h1, h2, h3, h4, h5, h6 { break-after: avoid; }
                p, li { orphans: 3; widows: 3; }
                
                /* Extremely wide single equations (long unbroken fraction chains) may still clip at print time; this is a known KaTeX/print limitation. */
                .katex-display { overflow-x: auto; max-width: 100%; }
                
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
        return htmlBuilder.toString()
    }

    private fun convertBlock(block: DocumentBlock): String {
        return when (block) {
            is HeadingBlock -> "<h${block.level}>${convertInline(block.text, block.content)}</h${block.level}>"
            is ParagraphBlock -> "<p>${convertInline(block.text, block.content)}</p>"
            is EquationBlock -> {
                val latex = normalizeLatex(block.latex)
                if (block.display) "\\[ $latex \\]" else "\\( $latex \\)"
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
                is InlineMathElement -> builder.append("\\( ${normalizeLatex(element.latex)} \\)")
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

    private fun normalizeLatex(raw: String): String {
        val sub = mapOf('₀' to '0','₁' to '1','₂' to '2','₃' to '3','₄' to '4','₅' to '5','₆' to '6','₇' to '7','₈' to '8','₉' to '9')
        val sup = mapOf('⁰' to '0','¹' to '1','²' to '2','³' to '3','⁴' to '4','⁵' to '5','⁶' to '6','⁷' to '7','⁸' to '8','⁹' to '9')
        val sb = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            when {
                sub.containsKey(c) -> {
                    val digits = StringBuilder()
                    while (i < raw.length && sub.containsKey(raw[i])) { digits.append(sub[raw[i]]); i++ }
                    sb.append("_{").append(digits).append("}")
                }
                sup.containsKey(c) -> {
                    val digits = StringBuilder()
                    while (i < raw.length && sup.containsKey(raw[i])) { digits.append(sup[raw[i]]); i++ }
                    sb.append("^{").append(digits).append("}")
                }
                c == '⇌' -> { sb.append("\\rightleftharpoons "); i++ }
                c == '→' -> { sb.append("\\rightarrow "); i++ }
                c == '←' -> { sb.append("\\leftarrow "); i++ }
                c == '×' -> { sb.append("\\times "); i++ }
                c == '·' || c == '•' -> { sb.append("\\cdot "); i++ }
                else -> { sb.append(c); i++ }
            }
        }
        return sb.toString()
    }
}
