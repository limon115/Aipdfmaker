package com.example.domain.repository

import android.content.Context
import android.os.Environment
import com.example.domain.services.pdf.TectonicBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import timber.log.Timber

class LatexCompilerRepository(private val context: Context) {
    suspend fun compileAndExportPdf(projectName: String, latexContent: String): Result<Pair<File, File>> = withContext(Dispatchers.IO) {
        runCatching {
            val safeName = projectName.trim().replace(Regex("[^a-zA-Z0-9.-]"), "_").ifEmpty { "Project" }
            
            // 🎯 PREEMPTIVE FIX: Escape LaTeX special characters so they don't break the compiler
            val displayTitle = projectName
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

            var documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            var baseDir = File(documentsDir, "aipdfs/$safeName")

            if (!baseDir.exists() && !baseDir.mkdirs()) {
                documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                baseDir = File(documentsDir, "aipdfs/$safeName")
            }
            baseDir.mkdirs()

            // 🎯 THE SANDBOX FIX: Create an internal font directory that the C++ engine has root access to
            val internalFontDir = File(context.filesDir, "tectonic_fonts").apply { mkdirs() }

            val fullLatex = """
                \documentclass{article}
                \usepackage{amsmath}
                \usepackage{amsfonts}
                \usepackage{amssymb}
                \usepackage{fontspec}

                % Set Baskervville as the premium main English font (from safe internal storage)
                \setmainfont[Path=${internalFontDir.absolutePath}/]{Baskervville.ttf}

                % Auto-switch to Kalpurush ONLY for Bengali characters (from safe internal storage)
                \usepackage[Bengali]{ucharclasses}
                \newfontfamily\bengalifont[Path=${internalFontDir.absolutePath}/]{kalpurush.ttf}
                \newfontfamily\englishfont[Path=${internalFontDir.absolutePath}/]{Baskervville.ttf}
                \setTransitionsForBengali{\bengalifont}{\englishfont}

                \title{$displayTitle}
                \begin{document}
                \maketitle
                $latexContent
                \end{document}
            """.trimIndent()

            // Copy Kalpurush (Bengali Font) to safe internal sandbox
            val bnFontFile = File(internalFontDir, "kalpurush.ttf")
            if (!bnFontFile.exists()) {
                context.assets.open("fonts/kalpurush.ttf").use { input ->
                    bnFontFile.outputStream().use { output -> input.copyTo(output) }
                }
            }

            // Copy Baskervville (English Font) to safe internal sandbox
            val enFontFile = File(internalFontDir, "Baskervville.ttf")
            if (!enFontFile.exists()) {
                context.assets.open("fonts/Baskervville.ttf").use { input ->
                    enFontFile.outputStream().use { output -> input.copyTo(output) }
                }
            }

            val texFile = File(baseDir, "main.tex")
            texFile.writeText(fullLatex)

            // 🎯 NATIVE STACK OVERFLOW FIX: Android limits background threads to 1MB.
            // Complex XeTeX font parsing requires massive memory. We forge an 8MB thread!
            var result: Result<File>? = null
            val latch = java.util.concurrent.CountDownLatch(1)
            Thread(null, {
                try {
                    kotlinx.coroutines.runBlocking { result = TectonicBridge.compileLatex(context, fullLatex) }
                } finally {
                    latch.countDown() // 🟠 THE FIX: Guarantee the latch releases even if compilation fails
                }
            }, "TectonicEngine", 8388608).start() // 8MB Stack Size
            latch.await()

            if (result!!.isSuccess) {
                val generatedPdf = result!!.getOrNull()
                if (generatedPdf != null && generatedPdf.exists()) {
                    val targetPdf = File(baseDir, "document.pdf")
                    generatedPdf.copyTo(targetPdf, overwrite = true)
                    return@runCatching Pair(targetPdf, texFile)
                }
            }
            throw Exception("PDF compilation failed: ${result.exceptionOrNull()?.message}")
        }
    }
}
