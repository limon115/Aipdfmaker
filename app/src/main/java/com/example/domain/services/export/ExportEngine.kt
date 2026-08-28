package com.example.domain.services.export

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.example.domain.services.pdf.TermuxXeLaTeXBridge
import java.io.File
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExportEngine(private val context: Context) {

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    fun exportProjectFiles(
        projectName: String,
        latexContent: String,
        isPdf: Boolean = true,
        onComplete: (pdfFile: File?, texFile: File) -> Unit
    ) {
        _exportProgress.value = 0f

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Timber.i(
                    "Exporting project %s as %s",
                    projectName,
                    if (isPdf) "PDF" else "LaTeX"
                )

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
                        "Unable to create export directory: ${baseDir.absolutePath}"
                    )
                }

                val fullLatex = """
                    \documentclass{article}

                    % 🎯 TEXTBOOK UPGRADE: Essential Packages
                    \usepackage{amsmath}
                    \usepackage{amsfonts}
                    \usepackage{amssymb}
                    \usepackage{fontspec}
                    \usepackage[Bengali]{ucharclasses}
                    \usepackage{geometry}
                    \geometry{a4paper, margin=1in}
                    \usepackage{tikz}
                    \usepackage{pgfplots}
                    \pgfplotsset{compat=1.18}
                    \usepackage{circuitikz}
                    \usepackage[version=4]{mhchem}
                    \usepackage{chemfig}
                    \usepackage{booktabs}
                    \usepackage{hyperref}

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
                    \author{AI Generated Notes}
                    \date{\today}

                    \begin{document}

                    \maketitle
                    \tableofcontents
                    \newpage

                    $latexContent

                    \end{document}
                """.trimIndent()

                val texFile = File(baseDir, "document.tex")
                texFile.writeText(fullLatex)

                _exportProgress.value = 0.5f

                if (!isPdf) {
                    _exportProgress.value = 1f
                    withContext(Dispatchers.Main) { onComplete(null, texFile) }
                    return@launch
                }

                val result = TermuxXeLaTeXBridge.compile(context = context, texFile = texFile)

                if (result.isSuccess) {
                    val generatedPdf = result.getOrNull()
                    if (generatedPdf != null && generatedPdf.exists()) {
                        val targetPdf = File(baseDir, "document.pdf")
                        generatedPdf.copyTo(targetPdf, overwrite = true)
                        _exportProgress.value = 1f
                        withContext(Dispatchers.Main) { onComplete(targetPdf, texFile) }
                        return@launch
                    }
                }

                throw Exception("XeLaTeX compilation failed: ${result.exceptionOrNull()?.message}")

            } catch (e: Exception) {
                Timber.e(e, "XeLaTeX export failed")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "PDF compilation failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    onComplete(null, File(""))
                }
            }
        }
    }
}
