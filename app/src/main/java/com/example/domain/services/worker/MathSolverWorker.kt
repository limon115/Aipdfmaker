package com.example.domain.services.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.BuildConfig
import com.example.data.datastore.AiSettingsDataStore
import com.example.data.network.AiNetworkClient
import com.example.domain.models.AiProvider
import com.example.domain.services.pdf.PdfNotificationManager
import com.example.domain.services.pdf.TermuxXeLaTeXBridge
import com.example.data.database.AppDatabase
import com.example.data.database.DocumentSnippetEntity
import com.example.data.database.ProjectEntity
import com.example.utils.AppLogger
import com.example.utils.FileUtils
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream

class MathSolverWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_PROBLEM_TEXT = "KEY_PROBLEM_TEXT"
        const val KEY_CUSTOM_TITLE = "KEY_CUSTOM_TITLE"
        const val KEY_PDF_PATH = "KEY_PDF_PATH"
        const val KEY_ERROR = "KEY_ERROR"
        const val CHANNEL_ID = "math_solver_bg_channel"
        const val NOTIFICATION_ID = 4001
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    override suspend fun doWork(): Result {
        val problemText = inputData.getString(KEY_PROBLEM_TEXT) ?: ""
        val customTitle = inputData.getString(KEY_CUSTOM_TITLE)?.trim()?.ifBlank { null }

        if (problemText.isBlank()) {
            val errorMsg = "Problem text was empty."
            showErrorNotification("Math Solution", errorMsg)
            return Result.failure(workDataOf(KEY_ERROR to errorMsg))
        }

        try {
            setProgress(workDataOf("PROGRESS" to 0.25f, "STATUS" to "AI is analyzing problem & generating solution..."))
            setForeground(createForegroundInfo("AI is solving the math problem..."))
        } catch (e: Exception) {
            AppLogger.w("MathSolverWorker", "Failed to set foreground service info: ${e.message}")
        }

        try {
            val dataStore = AiSettingsDataStore(context)
            val settings = dataStore.aiSettingsFlow.first()

            val candidates = settings.resolveCandidates(primarySlot = 2)

            if (candidates.isEmpty()) {
                val errorMsg = "No AI API key found. Please configure your Gemini or OpenAI API key in AI Settings."
                AppLogger.e("MathSolverWorker", errorMsg)
                showErrorNotification("Math Solution", errorMsg)
                return Result.failure(workDataOf(KEY_ERROR to errorMsg))
            }

            val prompt = """
                Act as an expert mathematics tutor. I have a specific math problem for you to solve. Please provide a comprehensive breakdown by strictly following these instructions:
                
                0. Problem Statement (MANDATORY AT THE VERY BEGINNING):
                Always write the question/problem provided at the very beginning of the PDF before solving. Present the complete original question clearly at the start of your document body inside an elegant box or dedicated section (e.g., \begin{tcolorbox}[title={\textbf{Problem Statement}}] ... \end{tcolorbox} or \section*{Problem Statement}).
                CRITICAL: NEVER output raw bracketed option text like "title=Problem Statement]" without the enclosing \begin{tcolorbox} environment and properly paired brackets!
                
                1. Related Theory & Fundamentals:
                Explain all the core theories, concepts, and fundamental formulas related to this problem. Strictly use fundamental core formulas for your proofs and explanations. Do not use derived memory tricks or shortcuts unless you ask for my permission first.
                
                2. The Solution:
                Always explain the logical steps before writing out the mathematical calculations.
                Keep the mathematical equations clean, separate from the descriptive text, and properly formatted.
                
                3. Variations:
                Show me all possible variations of this specific type of math problem that I might encounter. For each variation, provide the complete solution.

                CRITICAL LATEX & FORMATTING RULES (FAILURE IS NOT AN OPTION):
                0. ALWAYS WRITE THE QUESTION FIRST: You MUST write the provided question/problem statement at the very top of the generated document body inside \begin{tcolorbox}[title={\textbf{Problem Statement}}] ... \end{tcolorbox} or \section*{Problem Statement} before beginning the theory or solution.
                1. Return ONLY valid LaTeX code for the document body. Do NOT include \documentclass or \begin{document}.
                2. ABSOLUTELY NO MARKDOWN. NEVER use **bold**, *italics*, # headers, --- dividers, or markdown lists. Use \textbf{}, \textit{}, \section{}, \subsection{}, and \begin{itemize} \item ... \end{itemize}.
                3. ALL DIAGRAMS MUST BE WRAPPED IN ENVIRONMENTS. Never write raw coordinates or [scale=...] properties without the proper wrapper.
                   - Math/Geometry graphs MUST be enclosed in \begin{tikzpicture} ... \end{tikzpicture}.
                   - Physics Circuits MUST be enclosed in \begin{circuitikz} ... \end{circuitikz}.
                4. ALL TABLES MUST BE STRICT LATEX. NEVER use Markdown tables. Use \begin{table}[h] \centering \begin{tabular}{...} \toprule ... \midrule ... \bottomrule \end{tabular} \end{table}.
                   - IMPORTANT: Always end table rows with `\\` (double backslash).
                   - IMPORTANT: ALWAYS use exactly `\bottomrule` at the end of the table. NEVER use `\bottom.`, `\bottom>`, or any other typo.
                5. MATH MODE STRICTNESS: The `aligned` environment MUST be nested inside `equation`, `align`, `\[ ... \]`, or `${"$$"} ... ${"$$"}`.
                   - NEVER use `\begin{aligned}` completely alone in the text.
                   - CRITICAL: Ensure EVERY `\begin` has a matching `\end`. For example, `\begin{aligned}` MUST be closed with `\end{aligned}`. Do not leave stray `\end{gather}` or `\end{pmatrix}` without a `\begin`.
                   - CRITICAL: Delimiters must match. If you open with `\left[`, close with `\right]`. Do NOT close with `\end{pmatrix}`.
                6. HEADINGS: You MUST use actual structural commands for headings like `\section{Topic}`, `\subsection{Subtopic}`. NEVER write bare text in braces like `{Topic}` as a heading.
                7. SCRIPT CONSISTENCY: When writing in Bengali, strictly stick to pure Bengali and English characters. ABSOLUTELY DO NOT insert Arabic, Gujarati, or Devanagari characters (e.g., avoid inserting wrong script glyphs into Bengali words).
                8. TIKZ VALIDITY: Avoid zero-length draw commands (e.g., `\draw (2,2) -- (2,2)`). Ensure every path has actual length.
                9. ENVIRONMENT BALANCING: You MUST meticulously balance every `\begin` with its corresponding `\end`. Do NOT leave `\begin{tikzpicture}` without `\end{tikzpicture}`. Do NOT add stray `\end{center}` without a matching `\begin{center}`.
                10. DIAGRAM LABELING: When labeling TikZ graphs or diagrams, carefully position text nodes (e.g., using `above`, `below`, `left`, `right`, or explicit shifts) to ensure text NEVER overlaps with lines, curves, or other text.
                
                **STRICT XELATEX, POLYGLOSSIA & BENGALI RULES FOR THIS GENERATION:**
                1. **Polyglossia Language Management (CRITICAL):** The main document font is Bengali (`solaiman.ttf`) and the main document language is Bengali (`\setmainlanguage{bengali}`, `\setotherlanguage{english}`).
                   - Because Bangla is the main font, all Bengali text renders directly by default without needing wrappers.
                   - CRITICAL: Since the main document font is Bangla, you MUST explicitly wrap ALL English texts inside Polyglossia's english environment:
                     * For multi-line solutions, paragraphs, or blocks in English, wrap them in `\begin{english} ... \end{english}`.
                     * For inline English words, terms, headings, or phrases, wrap them in `\textenglish{<English text>}`.
                   - Do NOT redeclare `\usepackage{polyglossia}`, `\setmainlanguage`, `\setotherlanguage`, or font definitions; they are already loaded in the preamble.
                2. **Bengali in Math Mode (CRITICAL):** Polyglossia language environments do NOT automatically switch fonts inside LaTeX math mode (${'$'}...${'$'}, `\[ ... \]`, `align`, `equation`). Whenever writing Bengali text inside an equation, you MUST explicitly invoke `\text{\bengalifont ...}`:
                   - WRONG: `\text{সাফল্য}`
                   - CORRECT: `\text{\bengalifont সাফল্য}`
                3. **Numerals and English in Math:** Keep equations, variables, and numerical values in standard ASCII numerals (`0-9`) for clean mathematical alignment and parsing. For English text inside math formulas via `\text{...}`, wrap in `\text{\textenglish{...}}`.
                4. **No Em-Dashes:** The SolaimanLipi font does not support the Unicode Em-Dash (`—`). Never generate `—`. Always use standard LaTeX dashes (`---` for em-dash, `--` for en-dash).
                5. **No Floats in Boxes:** NEVER use floating environments (`\begin{table}` or `\begin{figure}`) inside a `tcolorbox`, `minipage`, or any other box. Use `\begin{center}` and `\begin{tabular}` directly.
                6. **Environment Hygiene:** Double-check that every `\begin{...}` has a matching `\end{...}`. 
                7. **No Stray Line Breaks:** Do not use `\\` to create new paragraphs in standard text. Use a blank line for a new paragraph or `\vspace{}`.
                8. **Script Consistency:** Output only English and Bengali text. Do not generate Arabic, Tamil, or other non-Latin scripts.
                
                * Outputs should be in the same language as input by user.
                
                Here is the problem:
                $problemText
            """.trimIndent()

            var aiResponse: String? = null
            var lastError: Exception? = null

            for (candidate in candidates) {
                try {
                    AppLogger.i("MathSolverWorker", "Attempting solve with provider ${candidate.provider}, model ${candidate.model}")
                    val aiClient = AiNetworkClient(
                        provider = candidate.provider,
                        apiKey = candidate.apiKey,
                        model = candidate.model,
                        temperature = candidate.temperature,
                        featureName = "Math Solver"
                    )
                    val response = aiClient.generateContent(prompt)
                    if (response.isNotBlank()) {
                        aiResponse = response
                        AppLogger.i("MathSolverWorker", "Successfully generated AI math solution via ${candidate.provider}")
                        break
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = e
                    AppLogger.w("MathSolverWorker", "Attempt with ${candidate.provider} (${candidate.model}) failed: ${e.message}")
                }
            }

            if (aiResponse.isNullOrBlank()) {
                val errorMsg = "AI generation failed: ${lastError?.message ?: "Empty response from AI"}. Please verify your API key and connection."
                AppLogger.e("MathSolverWorker", errorMsg, lastError)
                showErrorNotification("Math Solution", errorMsg)
                return Result.failure(workDataOf(KEY_ERROR to errorMsg))
            }

            var cleanLatex = aiResponse.trim()
            if (cleanLatex.startsWith("```")) {
                val firstLineEnd = cleanLatex.indexOf('\n')
                if (firstLineEnd != -1) {
                    cleanLatex = cleanLatex.substring(firstLineEnd + 1)
                }
            }
            if (cleanLatex.endsWith("```")) {
                cleanLatex = cleanLatex.substringBeforeLast("```")
            }
            cleanLatex = cleanLatex.trim()

            // If AI returned full document wrapper, strip documentclass and begin/end document
            if (cleanLatex.contains("\\documentclass")) {
                cleanLatex = cleanLatex.substringAfter("\\begin{document}", cleanLatex)
            }
            cleanLatex = cleanLatex
                .replace(Regex("""\\documentclass(\[.*?\])?\{.*?\}"""), "")
                .replace(Regex("""\\begin\{document\}"""), "")
                .replace(Regex("""\\end\{document\}"""), "")
                .trim()

            // Normalize any malformed \begin{tcolorbox} missing opening '['
            cleanLatex = cleanLatex.replace(
                Regex("""\\begin\{tcolorbox\}\s*title\s*=""", RegexOption.IGNORE_CASE),
                """\begin{tcolorbox}[title="""
            )

            // Fix bare stray "title=Problem Statement]" or "[title=Problem Statement]" without \begin{tcolorbox}
            cleanLatex = cleanLatex.replace(
                Regex("""(?m)^(?!\s*\\begin\{tcolorbox\}\s*\[)\s*\[?title\s*=\s*(?:\\textbf\{)?Problem\s*Statement\}?\]?\s*""", RegexOption.IGNORE_CASE)
            ) {
                """\begin{tcolorbox}[title={\textbf{Problem Statement}}]"""
            }

            // Ensure title options in \begin{tcolorbox}[title=...] are cleanly braced
            cleanLatex = cleanLatex.replace(
                Regex("""\\begin\{tcolorbox\}\[title=([^\{\]\n][^\]\n]*)\]""", RegexOption.IGNORE_CASE)
            ) { match ->
                val titleVal = match.groupValues[1].trim()
                """\begin{tcolorbox}[title={$titleVal}]"""
            }

            // Balance \begin{tcolorbox} and \end{tcolorbox} if the AI left tcolorbox unclosed
            val openBoxes = cleanLatex.split("\\begin{tcolorbox}").size - 1
            val closeBoxes = cleanLatex.split("\\end{tcolorbox}").size - 1
            if (openBoxes > closeBoxes) {
                val nextSectionIdx = cleanLatex.indexOf("\\section")
                cleanLatex = if (nextSectionIdx != -1) {
                    val before = cleanLatex.substring(0, nextSectionIdx)
                    val after = cleanLatex.substring(nextSectionIdx)
                    val beforeOpens = before.split("\\begin{tcolorbox}").size - 1
                    val beforeCloses = before.split("\\end{tcolorbox}").size - 1
                    if (beforeOpens > beforeCloses) {
                        before.trimEnd() + "\n\\end{tcolorbox}\n\n" + after
                    } else {
                        cleanLatex + "\n\\end{tcolorbox}"
                    }
                } else {
                    cleanLatex + "\n\\end{tcolorbox}"
                }
            }

            try {
                setProgress(workDataOf("PROGRESS" to 0.75f, "STATUS" to "Compiling LaTeX code to PDF..."))
                setForeground(createForegroundInfo("Compiling LaTeX solution to PDF..."))
            } catch (e: Exception) {
                AppLogger.w("MathSolverWorker", "Failed to set foreground status: ${e.message}")
            }

            val firstLine = problemText.lines().firstOrNull { it.isNotBlank() }?.trim() ?: "Math Solution"
            val resolvedTitle = customTitle ?: ("Math: " + firstLine.take(40))
            val safeTitle = com.example.utils.FileUtils.getSafeFileName(
                customTitle ?: firstLine.replace(Regex("[^a-zA-Z0-9 _-]"), " ").take(30),
                "Math_Solution"
            )

            val latexTitle = resolvedTitle
                .replace("\\", "\\textbackslash{}")
                .replace("&", "\\&")
                .replace("%", "\\%")
                .replace("$", "\\$")
                .replace("#", "\\#")
                .replace("_", "\\_")
                .replace("{", "\\{")
                .replace("}", "\\}")

            val fullLatex = """
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
                \title{$latexTitle}
                \author{AI Tutor}
                \date{\today}
                \begin{document}
                \XeTeXinterchartokenstate=1
                \maketitle
                $cleanLatex
                \end{document}
            """.trimIndent()

            val mathSolverDir = com.example.utils.FileUtils.getMathSolverDirectory(context)
            if (!mathSolverDir.exists()) {
                mathSolverDir.mkdirs()
            }

            // Deduplication Check: insure no duplicate PDF is saved for same LaTeX code
            var duplicatePdf: File? = null

            // 1. Scan existing .tex files in mathsolver directory
            val existingTexFiles = mathSolverDir.listFiles { file ->
                file.isFile && file.name.endsWith(".tex", ignoreCase = true)
            } ?: emptyArray()

            for (tex in existingTexFiles) {
                try {
                    val existingContent = tex.readText(Charsets.UTF_8).trim()
                    if (isSameLatexCode(existingContent, fullLatex, cleanLatex)) {
                        val matchingPdf = File(mathSolverDir, "${tex.nameWithoutExtension}.pdf")
                        if (matchingPdf.exists() && matchingPdf.length() > 0) {
                            duplicatePdf = matchingPdf
                            AppLogger.i("MathSolverWorker", "Found existing matching PDF for identical LaTeX in mathsolver: ${matchingPdf.absolutePath}")
                            break
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.w("MathSolverWorker", "Error checking existing tex file ${tex.name}: ${e.message}")
                }
            }

            // 2. Scan Room DB snippets for identical LaTeX code
            if (duplicatePdf == null) {
                try {
                    val db = com.example.data.database.AppDatabase.getDatabase(context)
                    val existingSnippet = db.documentSnippetDao().findSnippetByContent(fullLatex)
                        ?: db.documentSnippetDao().findSnippetByContent(cleanLatex)
                    if (existingSnippet != null) {
                        val project = db.projectDao().getProjectById(existingSnippet.projectId)
                        if (project != null) {
                            val candidatePdf = com.example.utils.FileUtils.findCompiledPdf(context, project.title)
                            if (candidatePdf != null && candidatePdf.exists() && candidatePdf.length() > 0) {
                                duplicatePdf = candidatePdf
                                AppLogger.i("MathSolverWorker", "Found existing compiled PDF from DB project '${project.title}': ${candidatePdf.absolutePath}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.w("MathSolverWorker", "Error searching DB snippets for duplicate LaTeX: ${e.message}")
                }
            }

            if (duplicatePdf != null && duplicatePdf.exists()) {
                AppLogger.i("MathSolverWorker", "Identical LaTeX code already exists. Reusing existing PDF without duplication: ${duplicatePdf.absolutePath}")
                try {
                    setProgress(workDataOf("PROGRESS" to 1.0f, "STATUS" to "Solution found (cached PDF reused)."))
                } catch (_: Exception) {}

                updateOrInsertProjectInDb(problemText, resolvedTitle, fullLatex)
                showSuccessNotification(resolvedTitle, duplicatePdf)
                return Result.success(workDataOf(KEY_PDF_PATH to duplicatePdf.absolutePath))
            }

            // No duplicate PDF found: compile LaTeX to PDF
            val buildDir = File(context.cacheDir, "math_build_${System.currentTimeMillis()}").apply { mkdirs() }
            val texFile = File(buildDir, "solution.tex")
            FileOutputStream(texFile).use { fos ->
                fos.write(fullLatex.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            val compileResult = TermuxXeLaTeXBridge.compile(context = context, texFile = texFile)
            val finalTargetPdf = File(mathSolverDir, "$safeTitle.pdf")
            val finalTargetTex = File(mathSolverDir, "$safeTitle.tex")

            var finalPdfFile: File? = null

            if (compileResult.isSuccess) {
                val generatedPdf = compileResult.getOrNull()
                if (generatedPdf != null && generatedPdf.exists()) {
                    try {
                        generatedPdf.copyTo(finalTargetPdf, overwrite = true)
                        finalPdfFile = finalTargetPdf
                        AppLogger.i("MathSolverWorker", "Saved XeLaTeX PDF to mathsolver directory: ${finalTargetPdf.absolutePath}")
                    } catch (e: Exception) {
                        finalPdfFile = generatedPdf
                        AppLogger.e("MathSolverWorker", "Failed to copy PDF to mathsolver directory: ${e.message}")
                    }
                }
            }

            // Fallback to native Android PDF rendering if XeLaTeX failed or was unavailable
            if (finalPdfFile == null || !finalPdfFile.exists()) {
                AppLogger.w("MathSolverWorker", "XeLaTeX compilation unavailable or returned error. Compiling via native Android PDF renderer.")
                val fallbackPdfFile = File(buildDir, "solution_native.pdf")
                val success = renderNativeFallbackPdf(problemText, cleanLatex, fallbackPdfFile)
                if (success && fallbackPdfFile.exists()) {
                    try {
                        fallbackPdfFile.copyTo(finalTargetPdf, overwrite = true)
                        finalPdfFile = finalTargetPdf
                    } catch (e: Exception) {
                        finalPdfFile = fallbackPdfFile
                    }
                }
            }

            // Save companion .tex file for future deduplication checks
            try {
                finalTargetTex.writeText(fullLatex, Charsets.UTF_8)
            } catch (e: Exception) {
                AppLogger.w("MathSolverWorker", "Failed to write companion tex file: ${e.message}")
            }

            // Clean up temporary build directory
            try {
                buildDir.deleteRecursively()
            } catch (_: Exception) {}

            if (finalPdfFile != null && finalPdfFile.exists()) {
                // Also mirror to project directory for home screen / notes viewer discovery
                try {
                    val projSafeName = com.example.utils.FileUtils.getSafeFileName(resolvedTitle, "Project")
                    val projDir = com.example.utils.FileUtils.getExportDirectory(context, "aipdfs/$projSafeName")
                    finalPdfFile.copyTo(File(projDir, "main.pdf"), overwrite = true)
                } catch (e: Exception) {
                    AppLogger.w("MathSolverWorker", "Failed to mirror solution to project dir: ${e.message}")
                }

                updateOrInsertProjectInDb(problemText, resolvedTitle, fullLatex)
                showSuccessNotification(resolvedTitle, finalPdfFile)
                return Result.success(workDataOf(KEY_PDF_PATH to finalPdfFile.absolutePath))
            } else {
                val errorMsg = "Could not produce PDF file: ${compileResult.exceptionOrNull()?.message ?: "PDF rendering error"}"
                showErrorNotification("Math Solution", errorMsg)
                return Result.failure(workDataOf(KEY_ERROR to errorMsg))
            }

        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Unknown Background Error"
            showErrorNotification("Math Solution", errorMsg)
            return Result.failure(workDataOf(KEY_ERROR to errorMsg))
        }
    }

    private fun createForegroundInfo(progressText: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Math Solver (Background)")
            .setContentText(progressText)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun showSuccessNotification(title: String, pdfFile: File) {
        val pdfNotificationManager = PdfNotificationManager(context)
        pdfNotificationManager.showSuccessNotification(title, pdfFile)
    }

    private fun showErrorNotification(title: String, errorMessage: String) {
        val pdfNotificationManager = PdfNotificationManager(context)
        pdfNotificationManager.showErrorNotification(title, errorMessage)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Math Solver Background",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of background math solver operations"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun renderNativeFallbackPdf(problemText: String, solutionLatex: String, outputFile: File): Boolean {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                isAntiAlias = true
            }
            val boldPaint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val headerPaint = Paint().apply {
                color = Color.rgb(20, 60, 120)
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }

            var y = 50f
            canvas.drawText("AI Math Solver - Complete Solution", 40f, y, headerPaint)
            y += 28f

            val boxPaint = Paint().apply {
                color = Color.rgb(240, 244, 250)
                style = Paint.Style.FILL
            }
            val borderPaint = Paint().apply {
                color = Color.rgb(180, 200, 230)
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawRoundRect(35f, y, 560f, y + 55f, 8f, 8f, boxPaint)
            canvas.drawRoundRect(35f, y, 560f, y + 55f, 8f, 8f, borderPaint)

            canvas.drawText("Problem:", 45f, y + 20f, boldPaint)
            val problemLines = problemText.lines().filter { it.isNotBlank() }.take(2)
            var probY = y + 38f
            for (pl in problemLines) {
                canvas.drawText(pl.take(75), 45f, probY, paint)
                probY += 14f
            }
            y += 75f

            val cleanText = solutionLatex
                .replace(Regex("\\\\section\\*?\\{([^}]+)\\}"), "\n\n### $1\n")
                .replace(Regex("\\\\subsection\\*?\\{([^}]+)\\}"), "\n## $1\n")
                .replace(Regex("\\\\textbf\\{([^}]+)\\}"), "$1")
                .replace(Regex("\\\\textit\\{([^}]+)\\}"), "$1")
                .replace(Regex("\\\\item"), "• ")
                .replace(Regex("\\\\[\\[\\]]"), "")
                .replace(Regex("\\\\begin\\{[^}]+\\}(?:\\s*\\[[^\\]]*\\])?"), "")
                .replace(Regex("\\\\end\\{[^}]+\\}"), "")
                .replace(Regex("(?:\\[\\s*)?title\\s*=\\s*(?:\\\\textbf\\{)?[^\\]\\n]+(?:\\})?\\]?"), "")
                .replace(Regex("\\\\boxed\\{([^}]+)\\}"), "[$1]")
                .replace(Regex("\\\\(?:quad|qquad|,|;|!)"), " ")
                .replace(Regex("\\\\text\\{([^}]+)\\}"), "$1")
                .replace(Regex("\\\\frac\\{([^}]+)\\}\\{([^}]+)\\}"), "($1)/($2)")
                .replace(Regex("\\\\[a-zA-Z]+"), "")
                .replace("$", "")

            val lines = cleanText.lines()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) {
                    y += 8f
                    continue
                }
                if (y > 800f) break
                if (trimmed.startsWith("###")) {
                    y += 10f
                    canvas.drawText(trimmed.removePrefix("###").trim(), 40f, y, boldPaint)
                    y += 18f
                } else if (trimmed.startsWith("##")) {
                    y += 6f
                    canvas.drawText(trimmed.removePrefix("##").trim(), 40f, y, boldPaint)
                    y += 16f
                } else {
                    val chunks = trimmed.chunked(78)
                    for (chunk in chunks) {
                        if (y > 800f) break
                        canvas.drawText(chunk, 40f, y, paint)
                        y += 14f
                    }
                }
            }

            pdfDocument.finishPage(page)
            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { fos ->
                pdfDocument.writeTo(fos)
                fos.flush()
            }
            pdfDocument.close()
            true
        } catch (e: Exception) {
            AppLogger.e("MathSolverWorker", "Native PDF generation failed", e)
            false
        }
    }

    private fun generateOfflineMathSolution(problemText: String, apiErrorMessage: String?): String {
        val cleanProblem = problemText.trim()
        val escapedProblem = cleanProblem
            .replace("\\", "\\textbackslash{}")
            .replace("&", "\\&")
            .replace("%", "\\%")
            .replace("$", "\\$")
            .replace("#", "\\#")
            .replace("_", "\\_")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("~", "\\textasciitilde{}")
            .replace("^", "\\textasciicircum{}")

        val isCalculus = cleanProblem.contains(Regex("(derivative|integral|limit|dx|dy/dx|integrate|differentiate|slope)", RegexOption.IGNORE_CASE))
        val isTrig = cleanProblem.contains(Regex("(sin|cos|tan|sec|csc|cot|angle|theta|radian|degree)", RegexOption.IGNORE_CASE))
        val isPhysics = cleanProblem.contains(Regex("(velocity|acceleration|force|mass|gravity|energy|circuit|resistor|current|voltage|ohm)", RegexOption.IGNORE_CASE))

        val theoryContent = when {
            isCalculus -> """
                Calculus analyzes dynamic quantities, infinitesimals, and accumulation through foundational operations:
                \begin{itemize}
                    \item \textbf{Instantaneous Rate of Change:}
                    \[
                        f'(x) = \lim_{h \to 0} \frac{f(x+h) - f(x)}{h}
                    \]
                    \item \textbf{Fundamental Theorem of Calculus:} Connecting differentiation and accumulation:
                    \[
                        \int_a^b f(x)\,dx = F(b) - F(a), \quad \text{where } F'(x) = f(x)
                    \]
                    \item \textbf{Linearity Property:} $\frac{d}{dx}[a f(x) + b g(x)] = a f'(x) + b g'(x)$.
                \end{itemize}
            """.trimIndent()
            isTrig -> """
                Trigonometric relations model periodicity, harmonics, and angles:
                \begin{itemize}
                    \item \textbf{Pythagorean Identity:} For all $\theta \in \mathbb{R}$:
                    \[
                        \sin^2\theta + \cos^2\theta = 1, \quad 1 + \tan^2\theta = \sec^2\theta
                    \]
                    \item \textbf{Euler's Identity:} Bridge between analytical functions and rotational transformations:
                    \[
                        e^{i\theta} = \cos\theta + i\sin\theta
                    \]
                \end{itemize}
            """.trimIndent()
            isPhysics -> """
                Governing physical conservation laws and dynamical principles:
                \begin{itemize}
                    \item \textbf{Newton's Dynamical Law:}
                    \[
                        \sum \mathbf{F} = m\mathbf{a} = \frac{d\mathbf{p}}{dt}
                    \]
                    \item \textbf{Conservation of Total Energy:}
                    \[
                        E_{\text{total}} = K + U = \text{constant}
                    \]
                \end{itemize}
            """.trimIndent()
            else -> """
                Fundamental algebraic properties, equivalence relations, and operations:
                \begin{itemize}
                    \item \textbf{Axiom of Equality:}
                    \[
                        a = b \iff a + c = b + c \quad \text{and} \quad a \cdot c = b \cdot c \quad (c \neq 0)
                    \]
                    \item \textbf{Quadratic Roots Formula:} For general second-order equations ${'$'}ax^2 + bx + c = 0${'$'}:
                    \[
                        x = \frac{-b \pm \sqrt{b^2 - 4ac}}{2a}
                    \]
                \end{itemize}
            """.trimIndent()
        }

        return """
            \section{Problem Formulation}
            \begin{center}
            \fbox{\parbox{0.9\textwidth}{\textbf{Problem Statement:}\\[0.5em] $escapedProblem}}
            \end{center}

            \section{Related Theory \& Fundamentals}
            $theoryContent

            \section{Detailed Step-by-Step Analytical Breakdown}
            We proceed to resolve the problem systematically from first principles:
            \begin{enumerate}
                \item \textbf{Define Quantities and Known Conditions:}
                Identify all given parameters, constants, and target unknowns from the problem formulation.
                
                \item \textbf{Establish Governing Equations:}
                Formulate the relationships using exact mathematical expressions:
                \begin{align*}
                    \text{Given formulation: } &\quad $escapedProblem \\
                    \text{Step 1: } &\quad \text{Separate independent terms and group like variables.} \\
                    \text{Step 2: } &\quad \text{Apply inverse operations systematically to isolate the unknown.} \\
                    \text{Evaluation: } &\quad \boxed{\text{Analytical solution evaluated for given parameters.}}
                \end{align*}
            \end{enumerate}

            \section{Variations \& Generalizations}
            \begin{itemize}
                \item \textbf{Variation 1 (Parametric Perturbation):} When coefficients vary continuously, solutions shift predictably along the critical boundary.
                \item \textbf{Variation 2 (Higher Dimensional Generalization):} Extending the formulation to multi-variable constraints:
                \[
                    f(x_1, x_2, \dots, x_n) = 0
                \]
            \end{itemize}

            \section{Verification \& Consistency Check}
            Direct substitution confirms that all algebraic and dimensional properties hold consistent.
        """.trimIndent()
    }

    private suspend fun updateOrInsertProjectInDb(problemText: String, title: String, fullLatex: String) {
        try {
            val db = AppDatabase.getDatabase(context)
            val existingProject = db.projectDao().findMathProjectByProblem(problemText)
                ?: db.projectDao().findProjectByTitle(title)

            val projectId = if (existingProject != null) {
                val updated = existingProject.copy(
                    title = title,
                    lastUpdated = System.currentTimeMillis(),
                    status = "Completed",
                    description = problemText,
                    sourceText = problemText
                )
                db.projectDao().updateProject(updated)
                existingProject.id
            } else {
                val project = ProjectEntity(
                    title = title,
                    course = "AI Math Solver",
                    chapter = "Step-by-step Solution",
                    description = problemText,
                    noteStyle = "Math Solution",
                    outputFormat = "PDF",
                    status = "Completed",
                    pageCount = 1,
                    lastUpdated = System.currentTimeMillis(),
                    sourceText = problemText
                )
                db.projectDao().insertProject(project).toInt()
            }

            val existingSnippet = db.documentSnippetDao().getFirstSnippetForProject(projectId)
            if (existingSnippet != null) {
                val updatedSnippet = existingSnippet.copy(
                    topicTitle = "Math Solution",
                    jsonContent = fullLatex
                )
                db.documentSnippetDao().insertSnippet(updatedSnippet)
            } else {
                val snippet = DocumentSnippetEntity(
                    projectId = projectId,
                    topicTitle = "Math Solution",
                    jsonContent = fullLatex,
                    orderIndex = 0
                )
                db.documentSnippetDao().insertSnippet(snippet)
            }
            AppLogger.i("MathSolverWorker", "Cleanly updated/inserted Math Solution project ID $projectId in database")
        } catch (e: Exception) {
            AppLogger.e("MathSolverWorker", "Failed to update/insert project in database: ${e.message}")
        }
    }

    private fun isSameLatexCode(existing: String, fullLatex: String, cleanLatex: String): Boolean {
        val normExisting = normalizeLatex(existing)
        val normFull = normalizeLatex(fullLatex)
        val normClean = normalizeLatex(cleanLatex)
        return normExisting == normFull || normExisting == normClean ||
               existing.trim() == fullLatex.trim() || existing.trim() == cleanLatex.trim()
    }

    private fun normalizeLatex(latex: String): String {
        return latex.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("%") && !it.startsWith("\\date") }
            .joinToString("\n")
    }
}
