package com.example.domain.services.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.BuildConfig
import com.example.data.datastore.AiSettingsDataStore
import com.example.data.network.AiNetworkClient
import com.example.domain.services.pdf.PdfNotificationManager
import com.example.domain.services.pdf.TermuxXeLaTeXBridge
import com.example.utils.AppLogger
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream

class MathSolverWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_PROBLEM_TEXT = "KEY_PROBLEM_TEXT"
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

        if (problemText.isBlank()) {
            val errorMsg = "Problem text was empty."
            showErrorNotification("Math Solution", errorMsg)
            return Result.failure(workDataOf(KEY_ERROR to errorMsg))
        }

        try {
            setForeground(createForegroundInfo("AI is solving the math problem..."))
        } catch (e: Exception) {
            AppLogger.w("MathSolverWorker", "Failed to set foreground service info: ${e.message}")
        }

        try {
            val dataStore = AiSettingsDataStore(context)
            val settings = dataStore.aiSettingsFlow.first()

            val aiClient = AiNetworkClient(
                provider = settings.ai2Provider.name,
                apiKey = settings.ai2ApiKey.ifBlank { BuildConfig.GEMINI_API_KEY },
                model = settings.ai2Model.ifBlank { "gemini-1.5-flash" },
                temperature = settings.ai2Temperature
            )

            val prompt = """
                Act as an expert mathematics tutor. I have a specific math problem for you to solve. Please provide a comprehensive breakdown by strictly following these instructions:
                
                1. Related Theory & Fundamentals:
                Explain all the core theories, concepts, and fundamental formulas related to this problem. Strictly use fundamental core formulas for your proofs and explanations. Do not use derived memory tricks or shortcuts unless you ask for my permission first.
                
                2. The Solution:
                Always explain the logical steps before writing out the mathematical calculations.
                Keep the mathematical equations clean, separate from the descriptive text, and properly formatted.
                
                3. Variations:
                Show me all possible variations of this specific type of math problem that I might encounter. For each variation, provide the complete solution.

                CRITICAL LATEX & FORMATTING RULES (FAILURE IS NOT AN OPTION):
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
                
                * Outputs should be in the same language as input by user.
                
                Here is the problem:
                $problemText
            """.trimIndent()

            val aiResponse = try {
                aiClient.generateContent(prompt)
            } catch (e: Exception) {
                val errorMsg = "AI Generation Failed: ${e.message}"
                showErrorNotification("Math Solution", errorMsg)
                return Result.failure(workDataOf(KEY_ERROR to errorMsg))
            }

            val cleanLatex = aiResponse.removePrefix("```latex").replace("```latex\n", "").removePrefix("```").removeSuffix("```").trim()

            try {
                setForeground(createForegroundInfo("Compiling LaTeX solution to PDF..."))
            } catch (e: Exception) {
                AppLogger.w("MathSolverWorker", "Failed to set foreground status: ${e.message}")
            }

            val safeName = "Math_Solution_${System.currentTimeMillis()}"
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val baseDir = File(documentsDir, "aipdfs/$safeName")
            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }

            val fullLatex = """
                \documentclass{article}
                \usepackage{amsmath}
                \usepackage{amsfonts}
                \usepackage{amssymb}
                \usepackage{fontspec}
                \usepackage[Bengali]{ucharclasses}
                \usepackage{geometry}
                \geometry{a4paper, margin=1in}
                \usepackage{tikz}
                \usepackage{pgfplots}
                \usepackage{circuitikz}
                \usepackage{booktabs}
                \pgfplotsset{compat=1.18}
                \setmainfont{DejaVu Serif}
                \newfontfamily\bengalifont[
                    Path=/data/data/com.termux/files/home/,
                    Script=Bengali,
                    Language=Bengali,
                    AutoFakeBold=1.5,
                    AutoFakeSlant=0.2
                ]{solaiman.ttf}
                \setTransitionsFor{Bengali}{\bengalifont}{}
                \setTransitionsFor{Devanagari}{\bengalifont}{}
                \setTransitionsFor{BasicLatin}{\rmfamily}{}
                \title{Math Solution}
                \author{AI Tutor}
                \date{\today}
                \begin{document}
                \XeTeXinterchartokenstate=1
                \maketitle
                $cleanLatex
                \end{document}
            """.trimIndent()

            val texFile = File(baseDir, "solution.tex")
            FileOutputStream(texFile).use { fos ->
                fos.write(fullLatex.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            val compileResult = TermuxXeLaTeXBridge.compile(context = context, texFile = texFile)

            if (compileResult.isSuccess) {
                val generatedPdf = compileResult.getOrNull()
                if (generatedPdf != null && generatedPdf.exists()) {
                    showSuccessNotification("Math Solution", generatedPdf)
                    return Result.success(workDataOf(KEY_PDF_PATH to generatedPdf.absolutePath))
                } else {
                    val errorMsg = "PDF was generated but not found."
                    showErrorNotification("Math Solution", errorMsg)
                    return Result.failure(workDataOf(KEY_ERROR to errorMsg))
                }
            } else {
                val errorMsg = "LaTeX Compilation Failed:\n${compileResult.exceptionOrNull()?.message}"
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
}
