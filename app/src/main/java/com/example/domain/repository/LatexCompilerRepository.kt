package com.example.domain.repository

import android.content.Context
import android.os.Environment
import com.example.domain.services.pdf.TermuxXeLaTeXBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class LatexCompilerRepository(private val context: Context) {

    suspend fun compileAndExportPdf(
        projectName: String,
        latexContent: String,
        fixScript: String? = null
    ): Result<Pair<File, File>> = withContext(Dispatchers.IO) {
        runCatching {
            val safeName = projectName
                .trim()
                .replace(Regex("[^a-zA-Z0-9.-]"), "_")
                .ifEmpty { "Project" }

            val documentsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
            )

            val baseDir = File(
                documentsDir,
                "aipdfs/$safeName"
            )

            if (!baseDir.exists() && !baseDir.mkdirs()) {
                throw Exception(
                    "Unable to create compilation directory: ${baseDir.absolutePath}"
                )
            }

            val fullLatex = if (latexContent.contains("\\documentclass")) {
                latexContent
            } else {
                """
                \documentclass{article}
                \usepackage{amsmath}
                \usepackage{amsfonts}
                \usepackage{amssymb}
                \usepackage{fontspec}
                \usepackage{ucharclasses} % Removed invalid [Bengali] option
                \usepackage{tikz} % Added for diagrams
                \usepackage{circuitikz} % Added for circuits
                \usepackage{booktabs} % Added for toprule/midrule
                \usepackage{tcolorbox} % Added for rectbox fallback
                \newenvironment{rectbox}{\begin{tcolorbox}}{\end{tcolorbox}}
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
                \title{$projectName}
                \begin{document}
                \maketitle
                $latexContent
                \end{document}
                """.trimIndent()
            }

            val texFile = File(baseDir, "main.tex")
            texFile.writeText(fullLatex)

            val result = TermuxXeLaTeXBridge.compile(
                context = context,
                texFile = texFile,
                fixScript = fixScript
            ).getOrElse { error ->
                throw Exception(
                    "XeLaTeX compilation failed: ${error.message}",
                    error
                )
            }

            val targetPdf = File(baseDir, "document.pdf")

            result.copyTo(
                targetPdf,
                overwrite = true
            )

            Pair(targetPdf, texFile)
        }
    }
}
