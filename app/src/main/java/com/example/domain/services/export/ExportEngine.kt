package com.example.domain.services.export

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.example.domain.services.pdf.TectonicBridge
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
                Timber.i("Exporting project %s as %s", projectName, if (isPdf) "PDF" else "LaTeX")
                com.example.utils.AppLogger.i("ExportEngine", "Exporting project $projectName as ${if (isPdf) "PDF" else "LaTeX"}")
                val safeName = projectName.trim().replace(Regex("[^a-zA-Z0-9.-]"), "_").ifEmpty { "Project" }
                
                var documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                var baseDir = File(documentsDir, "aipdfs/\$safeName")
                Timber.d("Target base directory: %s", baseDir.absolutePath)
                
                if (!baseDir.exists() && !baseDir.mkdirs()) {
                    documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
                    baseDir = File(documentsDir, "aipdfs/\$safeName")
                }
                baseDir.mkdirs()
                
                // Wrap in full document
                val fullLatex = """
                    \documentclass{article}
                    \usepackage[utf8]{inputenc}
                    \usepackage{amsmath}
                    \usepackage{amsfonts}
                    \usepackage{amssymb}
                    \title{$projectName}
                    \begin{document}
                    \maketitle
                    $latexContent
                    \end{document}
                """.trimIndent()

                val texFile = File(baseDir, "document.tex")
                texFile.writeText(fullLatex)
                
                _exportProgress.value = 0.5f

                if (isPdf) {
                    try {
                        val result = TectonicBridge.compileLatex(context, fullLatex)
                        if (result.isSuccess) {
                            val generatedPdf = result.getOrNull()
                            if (generatedPdf != null && generatedPdf.exists()) {
                                val targetPdf = File(baseDir, "document.pdf")
                                generatedPdf.copyTo(targetPdf, overwrite = true)
                                _exportProgress.value = 1f
                                withContext(Dispatchers.Main) {
                                    onComplete(targetPdf, texFile)
                                }
                                return@launch
                            }
                        }
                        throw Exception("PDF compilation failed: ${result.exceptionOrNull()?.message}")
                    } catch (e: UnsatisfiedLinkError) {
                        Timber.e(e, "Tectonic JNI missing")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "JNI library missing. Saved as .tex only. Run `cargo ndk` to build.", Toast.LENGTH_LONG).show()
                            onComplete(null, texFile)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Render Error during export")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Render Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            onComplete(null, texFile)
                        }
                    }
                } else {
                    _exportProgress.value = 1f
                    withContext(Dispatchers.Main) {
                        onComplete(null, texFile)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to export project")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    onComplete(null, File(""))
                }
            }
        }
    }
}
