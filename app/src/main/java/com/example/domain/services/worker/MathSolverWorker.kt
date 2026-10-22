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

            val template = settings.promptMathSolver.ifBlank {
                com.example.domain.models.DefaultPrompts.DEFAULT_MATH_SOLVER_PROMPT
            }
            val prompt = if (template.contains("${'$'}problemText")) {
                template.replace("${'$'}problemText", problemText)
            } else {
                "$template\n\nHere is the problem:\n$problemText"
            }

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
                \author{\textenglish{AI Tutor}}
                \date{}
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
