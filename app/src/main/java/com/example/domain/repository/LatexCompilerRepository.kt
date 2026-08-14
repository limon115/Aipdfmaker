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

            var documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            var baseDir = File(documentsDir, "aipdfs/$safeName")

            if (!baseDir.exists() && !baseDir.mkdirs()) {
                documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                baseDir = File(documentsDir, "aipdfs/$safeName")
            }
            baseDir.mkdirs()

            val fullLatex = """
                \documentclass{article}
                \usepackage{amsmath}
                \usepackage{amsfonts}
                \usepackage{amssymb}
                \usepackage{fontspec}
                
                % Set Baskervville as the premium main English font
                \setmainfont[Path=./]{Baskervville.ttf}
                
                % Auto-switch to Kalpurush ONLY for Bengali characters
                \usepackage[Bengali]{ucharclasses}
                \newfontfamily\bengalifont[Path=./]{kalpurush.ttf}
                \setTransitionsForBengali{\begingroup\bengalifont}{\endgroup}
                
                \title{$projectName}
                \begin{document}
                \maketitle
                $latexContent
                \end{document}
            """.trimIndent()

            // Copy Kalpurush (Bengali Font)
            val bnFontFile = File(baseDir, "kalpurush.ttf")
            if (!bnFontFile.exists()) {
                context.assets.open("fonts/kalpurush.ttf").use { input ->
                    bnFontFile.outputStream().use { output -> input.copyTo(output) }
                }
            }

            // Copy Baskervville (English Font)
            val enFontFile = File(baseDir, "Baskervville.ttf")
            if (!enFontFile.exists()) {
                context.assets.open("fonts/Baskervville.ttf").use { input ->
                    enFontFile.outputStream().use { output -> input.copyTo(output) }
                }
            }

            val texFile = File(baseDir, "main.tex")
            texFile.writeText(fullLatex)

            val result = TectonicBridge.compileLatex(context, fullLatex)

            if (result.isSuccess) {
                val generatedPdf = result.getOrNull()
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
