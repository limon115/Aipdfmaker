package com.example.domain.services.chat

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.data.datastore.AiSettingsDataStore
import com.example.data.network.AiNetworkClient
import com.example.domain.models.chat.ChatMessage
import com.example.domain.services.pdf.TermuxXeLaTeXBridge
import com.example.utils.AppLogger
import com.example.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LatexChatService(private val context: Context) {

    data class GenerationResult(
        val conversationalText: String,
        val latexCode: String?,
        val suggestedTitle: String
    )

    suspend fun generateChatResponse(
        history: List<ChatMessage>,
        userPrompt: String
    ): GenerationResult = withContext(Dispatchers.IO) {
        val dataStore = AiSettingsDataStore(context)
        val settings = dataStore.aiSettingsFlow.first()
        val candidates = settings.resolveCandidates(primarySlot = 2)

        if (candidates.isEmpty()) {
            throw IllegalStateException("No AI API key found. Please configure your API key in Settings > AI Configuration.")
        }

        // Build recent conversation context (last 6 messages for token efficiency)
        val recentHistory = history.takeLast(6)
        val historyContext = if (recentHistory.isNotEmpty()) {
            val sb = StringBuilder("Previous conversation context:\n")
            for (msg in recentHistory) {
                val role = if (msg.isUser) "User" else "Assistant"
                val snippet = if (!msg.latexCode.isNullOrBlank()) {
                    "${msg.text.take(150)}... [Generated LaTeX Included]"
                } else {
                    msg.text.take(200)
                }
                sb.append("$role: $snippet\n")
            }
            sb.append("\n")
            sb.toString()
        } else {
            ""
        }

        val systemPrompt = """
            You are the DocMorph LaTeX & PDF AI Expert Assistant.
            Your role is to understand any user request—including creating documents, cheat sheets, math formulas, physics proofs, resume/CV, assignments, letters, research articles, exam questions, tables, or TikZ diagrams—and provide both a helpful conversational explanation AND a complete, clean, compilable LaTeX document to be converted into a PDF.

            $historyContext
            User's Current Request:
            $userPrompt

            MANDATORY INSTRUCTIONS & FORMATTING RULES:
            1. Friendly Conversational Response:
               - First, provide a clear, friendly overview explaining what you created, how it is organized, or how any custom adjustments were applied.
               - Match the language of the user (if user wrote in Bengali, reply in Bengali; if English, reply in English).

            2. Complete LaTeX Document Code:
               - Enclose the complete, compilable LaTeX code in a single markdown code fence starting with ```latex and ending with ```.
               - DO NOT omit necessary packages. Include standard preamble or body code.
               - You may include \documentclass{article} or document body. The app will ensure all required packages (geometry, amsmath, fontspec, polyglossia, tikz, circuitikz, pgfplots, booktabs, tcolorbox) are available.

            3. STRICT XELATEX & POLYGLOSSIA RULES:
               - Bengali font is 'solaiman.ttf', English font is 'DejaVu Serif'.
               - The main document language is Bengali.
               - For multi-line English sections or paragraphs, wrap them in:
                 \begin{english} ... \end{english}
               - For inline English words or phrases, wrap in \textenglish{...}.
               - When writing Bengali inside math mode, wrap in \text{\bengalifont ...}.
               - Keep numerical values and formula symbols in standard ASCII digits (0-9).
               - Avoid Unicode em-dashes (—); use standard LaTeX dashes (--- or --).
               - Never wrap standard body text or paragraphs inside \text{} or \mbox{}. Standard text must flow naturally.
               - Ensure every \begin{...} has a matching \end{...}.
               - If TikZ is used, wrap inside \begin{tikzpicture} ... \end{tikzpicture}.
               - If tables are used, use \begin{tabular}{...} ... \end{tabular} with \toprule, \midrule, and \bottomrule.

            Now generate your helpful response with the complete ```latex ... ``` block.
        """.trimIndent()

        var rawResponse: String? = null
        var lastError: Exception? = null

        for (candidate in candidates) {
            try {
                AppLogger.i("LatexChatService", "Trying candidate ${candidate.provider} (${candidate.model})")
                val client = AiNetworkClient(
                    provider = candidate.provider,
                    apiKey = candidate.apiKey,
                    model = candidate.model,
                    temperature = candidate.temperature,
                    featureName = "LaTeX AI ChatBot"
                )
                val response = client.generateContent(systemPrompt)
                if (response.isNotBlank()) {
                    rawResponse = response
                    break
                }
            } catch (e: Exception) {
                lastError = e
                AppLogger.w("LatexChatService", "Candidate ${candidate.provider} failed: ${e.message}")
            }
        }

        if (rawResponse.isNullOrBlank()) {
            throw Exception(lastError?.message ?: "AI generation failed. Please check your network and API keys.")
        }

        val (explanation, latexCode) = parseAiResponse(rawResponse)
        val title = deriveTitle(userPrompt, explanation)

        GenerationResult(
            conversationalText = explanation,
            latexCode = latexCode,
            suggestedTitle = title
        )
    }

    private fun parseAiResponse(response: String): Pair<String, String?> {
        val trimmed = response.trim()
        val latexFencePattern = Regex("```(?:latex|tex)?\\s*\\n?([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val match = latexFencePattern.find(trimmed)

        if (match != null) {
            val latexCode = match.groupValues[1].trim()
            val before = trimmed.substring(0, match.range.first).trim()
            val after = trimmed.substring(match.range.last + 1).trim()
            val explanation = when {
                before.isNotEmpty() && after.isNotEmpty() -> "$before\n\n$after"
                before.isNotEmpty() -> before
                after.isNotEmpty() -> after
                else -> "Here is the customized LaTeX code and PDF document generated for your request."
            }
            return Pair(explanation, latexCode)
        }

        // If no code fence but response contains LaTeX document markers
        if (trimmed.contains("\\documentclass") || trimmed.contains("\\begin{document}") || trimmed.contains("\\section")) {
            val docStart = trimmed.indexOf("\\documentclass").takeIf { it != -1 }
                ?: trimmed.indexOf("\\begin{document}").takeIf { it != -1 }
                ?: trimmed.indexOf("\\section").takeIf { it != -1 }
                ?: 0

            val explanation = if (docStart > 0) trimmed.substring(0, docStart).trim() else "Generated LaTeX Document"
            val latexCode = trimmed.substring(docStart).trim()
            return Pair(explanation.ifBlank { "Generated LaTeX Document" }, latexCode)
        }

        return Pair(trimmed, null)
    }

    fun prepareFullLatexDocument(latexCode: String, title: String): String {
        var clean = latexCode.trim()

        if (clean.contains("\\documentclass") && clean.contains("\\begin{document}")) {
            // Document is already complete, ensure essential package definitions or return clean
            return clean
        }

        // Strip any stray outer document wrappers if partially present
        clean = clean
            .replace(Regex("""\\documentclass(\[.*?\])?\{.*?\}"""), "")
            .replace(Regex("""\\begin\{document\}"""), "")
            .replace(Regex("""\\end\{document\}"""), "")
            .trim()

        val escapedTitle = title
            .replace("\\", "\\textbackslash{}")
            .replace("&", "\\&")
            .replace("%", "\\%")
            .replace("$", "\\$")
            .replace("#", "\\#")
            .replace("_", "\\_")
            .replace("{", "\\{")
            .replace("}", "\\}")

        return """
            \documentclass{article}
            \usepackage{geometry}
            \geometry{a4paper, margin=1in}
            \usepackage{xcolor}
            \usepackage{amsmath}
            \usepackage{amsfonts}
            \usepackage{amssymb}
            \usepackage{fontspec}
            \usepackage{polyglossia}
            \setmainlanguage{bengali}
            \setotherlanguage{english}
            \usepackage{tikz}
            \usepackage{pgfplots}
            \usepackage{circuitikz}
            \usepackage{booktabs}
            \usepackage{tcolorbox}
            \usepackage[colorlinks=true, linkcolor=blue, urlcolor=blue]{hyperref}
            \newenvironment{rectbox}{\begin{tcolorbox}}{\end{tcolorbox}}
            \pgfplotsset{compat=1.18}
            \setmainfont[
                Path=/data/data/com.termux/files/home/,
                Script=Bengali,
                AutoFakeBold=1.5,
                AutoFakeSlant=0.2
            ]{solaiman.ttf}
            \newfontfamily\englishfont{DejaVu Serif}
            \newfontfamily\bengalifont[
                Path=/data/data/com.termux/files/home/,
                Script=Bengali,
                AutoFakeBold=1.5,
                AutoFakeSlant=0.2
            ]{solaiman.ttf}
            \title{$escapedTitle}
            \author{\textenglish{DocMorph AI Assistant}}
            \date{\today}
            \begin{document}
            \XeTeXinterchartokenstate=1
            \maketitle
            $clean
            \end{document}
        """.trimIndent()
    }

    suspend fun compileLatexToPdf(
        latexCode: String,
        documentTitle: String,
        targetFile: File? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        val safeTitle = FileUtils.getSafeFileName(documentTitle, "LatexDoc")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputDir = FileUtils.getExportDirectory(context, "aipdfs/latex_chat")
        if (!outputDir.exists()) outputDir.mkdirs()

        val finalPdfFile = targetFile ?: File(outputDir, "${safeTitle}_$timestamp.pdf")
        val finalTexFile = if (targetFile != null) {
            File(targetFile.parentFile ?: outputDir, "${targetFile.nameWithoutExtension}.tex")
        } else {
            File(outputDir, "${safeTitle}_$timestamp.tex")
        }

        val fullLatex = prepareFullLatexDocument(latexCode, documentTitle)

        // Write companion .tex file for user reference
        try {
            finalTexFile.writeText(fullLatex, Charsets.UTF_8)
        } catch (e: Exception) {
            AppLogger.w("LatexChatService", "Failed to write .tex file: ${e.message}")
        }

        // Temporary directory for compilation
        val buildDir = File(context.cacheDir, "chat_build_${System.currentTimeMillis()}")
        buildDir.mkdirs()
        val tempTex = File(buildDir, "input.tex")

        try {
            tempTex.writeText(fullLatex, Charsets.UTF_8)

            // 1. Attempt compilation via Termux XeLaTeX Bridge
            AppLogger.i("LatexChatService", "Attempting XeLaTeX compilation via TermuxXeLaTeXBridge...")
            val compileResult = TermuxXeLaTeXBridge.compile(context, tempTex)

            if (compileResult.isSuccess) {
                val compiled = compileResult.getOrNull()
                if (compiled != null && compiled.exists() && compiled.length() > 0) {
                    compiled.copyTo(finalPdfFile, overwrite = true)
                    AppLogger.i("LatexChatService", "XeLaTeX compilation succeeded: ${finalPdfFile.absolutePath}")
                    return@withContext Result.success(finalPdfFile)
                }
            }

            // 2. Fallback to native Android PDF rendering if XeLaTeX server is offline or fails
            AppLogger.w("LatexChatService", "TermuxXeLaTeX unavailable or failed; using native Android PDF fallback")
            val fallbackSuccess = renderNativeFallbackPdf(documentTitle, fullLatex, finalPdfFile)
            if (fallbackSuccess && finalPdfFile.exists() && finalPdfFile.length() > 0) {
                AppLogger.i("LatexChatService", "Native PDF fallback successfully created: ${finalPdfFile.absolutePath}")
                return@withContext Result.success(finalPdfFile)
            }

            Result.failure(Exception(compileResult.exceptionOrNull()?.message ?: "PDF generation failed"))
        } catch (e: Exception) {
            AppLogger.e("LatexChatService", "Compilation exception: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                buildDir.deleteRecursively()
            } catch (_: Exception) {}
        }
    }

    private fun renderNativeFallbackPdf(title: String, latexContent: String, outputFile: File): Boolean {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 standard width
            val pageHeight = 842 // A4 standard height
            val margin = 40f
            val maxLineWidth = pageWidth - (margin * 2)

            val titlePaint = Paint().apply {
                color = Color.rgb(26, 35, 126) // Deep Indigo
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val subtitlePaint = Paint().apply {
                color = Color.rgb(90, 100, 120)
                textSize = 10f
                isAntiAlias = true
            }
            val headerPaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val bodyPaint = Paint().apply {
                color = Color.rgb(33, 33, 33)
                textSize = 11f
                isAntiAlias = true
            }
            val codePaint = Paint().apply {
                color = Color.rgb(40, 50, 60)
                textSize = 10f
                typeface = android.graphics.Typeface.MONOSPACE
                isAntiAlias = true
            }
            val boxBgPaint = Paint().apply {
                color = Color.rgb(245, 247, 250)
                style = Paint.Style.FILL
            }
            val boxBorderPaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            // Extract plain text lines from LaTeX
            val cleanLines = cleanLatexForFallbackDisplay(latexContent)

            var currentPageNumber = 1
            var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create())
            var canvas = page.canvas
            var y = 50f

            // Document Header
            canvas.drawText(title.take(50), margin, y, titlePaint)
            y += 18f
            val dateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
            canvas.drawText("Generated by DocMorph LaTeX AI Chatbot • $dateStr", margin, y, subtitlePaint)
            y += 14f

            // Header dividing line
            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1.5f
            }
            canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
            y += 24f

            fun checkPageOverflow(neededHeight: Float) {
                if (y + neededHeight > pageHeight - margin) {
                    pdfDocument.finishPage(page)
                    currentPageNumber++
                    page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create())
                    canvas = page.canvas
                    y = margin + 15f
                }
            }

            for (line in cleanLines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) {
                    y += 10f
                    continue
                }

                if (trimmed.startsWith("### ")) {
                    checkPageOverflow(30f)
                    val sectionTitle = trimmed.removePrefix("### ").trim()
                    canvas.drawText(sectionTitle, margin, y + 14f, headerPaint)
                    y += 26f
                } else if (trimmed.startsWith("## ")) {
                    checkPageOverflow(36f)
                    val sectionTitle = trimmed.removePrefix("## ").trim()
                    canvas.drawText(sectionTitle, margin, y + 16f, headerPaint)
                    y += 30f
                } else if (trimmed.startsWith("[Code]") || trimmed.startsWith("[Equation]")) {
                    checkPageOverflow(32f)
                    canvas.drawRoundRect(margin, y, pageWidth - margin, y + 26f, 4f, 4f, boxBgPaint)
                    canvas.drawRoundRect(margin, y, pageWidth - margin, y + 26f, 4f, 4f, boxBorderPaint)
                    canvas.drawText(trimmed.take(70), margin + 8f, y + 17f, codePaint)
                    y += 34f
                } else {
                    // Wrap regular text
                    val wrapped = wrapText(trimmed, bodyPaint, maxLineWidth)
                    for (wLine in wrapped) {
                        checkPageOverflow(16f)
                        canvas.drawText(wLine, margin, y + 11f, bodyPaint)
                        y += 16f
                    }
                }
            }

            pdfDocument.finishPage(page)

            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
                fos.flush()
            }
            pdfDocument.close()
            true
        } catch (e: Exception) {
            AppLogger.e("LatexChatService", "Fallback PDF render failed: ${e.message}", e)
            false
        }
    }

    private fun cleanLatexForFallbackDisplay(latex: String): List<String> {
        val result = mutableListOf<String>()
        val lines = latex.lines()
        var insidePreamble = true

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.contains("\\begin{document}")) {
                insidePreamble = false
                continue
            }
            if (trimmed.contains("\\end{document}")) {
                break
            }
            if (insidePreamble) continue

            // Parse structural elements
            when {
                trimmed.startsWith("\\section*{") || trimmed.startsWith("\\section{") -> {
                    val title = trimmed.substringAfter("{").substringBeforeLast("}")
                    result.add("## $title")
                }
                trimmed.startsWith("\\subsection*{") || trimmed.startsWith("\\subsection{") -> {
                    val title = trimmed.substringAfter("{").substringBeforeLast("}")
                    result.add("### $title")
                }
                trimmed.startsWith("\\item") -> {
                    val item = trimmed.removePrefix("\\item").trim()
                    result.add("• " + stripLatexCommands(item))
                }
                trimmed.startsWith("\\[") || trimmed.startsWith("$$") -> {
                    result.add("[Equation] " + stripLatexCommands(trimmed))
                }
                trimmed.startsWith("\\begin{") || trimmed.startsWith("\\end{") -> {
                    // ignore environment boundaries
                }
                trimmed.isNotBlank() -> {
                    val cleaned = stripLatexCommands(trimmed)
                    if (cleaned.isNotBlank()) {
                        result.add(cleaned)
                    }
                }
            }
        }
        return if (result.isEmpty()) lines.map { stripLatexCommands(it) }.filter { it.isNotBlank() } else result
    }

    private fun stripLatexCommands(text: String): String {
        return text
            .replace(Regex("""\\textbf\{([^}]+)\}"""), "$1")
            .replace(Regex("""\\textit\{([^}]+)\}"""), "$1")
            .replace(Regex("""\\textenglish\{([^}]+)\}"""), "$1")
            .replace(Regex("""\\text\{([^}]+)\}"""), "$1")
            .replace(Regex("""\\bengalifont\{([^}]+)\}"""), "$1")
            .replace(Regex("""\\[a-zA-Z]+(\[[^\]]*\])?(\{([^}]*)\})?"""), "$3")
            .replace(Regex("""[$&%#{}_]"""), "")
            .replace("\\\\", "")
            .trim()
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    private fun deriveTitle(prompt: String, explanation: String): String {
        val firstLine = prompt.lines().firstOrNull { it.isNotBlank() }?.trim() ?: "Latex Document"
        val clean = firstLine.replace(Regex("[^a-zA-Z0-9 _-]"), " ").trim()
        return if (clean.length > 5) {
            clean.take(35)
        } else {
            "Latex Document"
        }
    }
}
