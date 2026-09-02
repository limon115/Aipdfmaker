package com.example.ui.screens.math

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.datastore.AiSettingsDataStore
import com.example.data.network.AiNetworkClient
import com.example.domain.services.pdf.TermuxXeLaTeXBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import timber.log.Timber

sealed class MathSolverState {
    object Idle : MathSolverState()
    object Processing : MathSolverState()
    object CompilingPdf : MathSolverState()
    data class Success(val pdfFile: File) : MathSolverState()
    data class Error(val message: String) : MathSolverState()
}

class MathSolverViewModel : ViewModel() {
    private val _state = MutableStateFlow<MathSolverState>(MathSolverState.Idle)
    val state: StateFlow<MathSolverState> = _state.asStateFlow()

    fun solveProblem(context: Context, problemText: String) {
        _state.value = MathSolverState.Processing
        viewModelScope.launch(Dispatchers.IO) {
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
                    Never use a TL;DR or summarize the math at the end.
                    
                    3. Variations:
                    Show me all possible variations of this specific type of math problem that I might encounter. For each variation, provide the complete solution using the exact same formatting rules as above (steps explained first, clean math separate, fundamental formulas only).
                    
                    * Outputs should be in the same language as input by user.
                    
                    Here is the problem:
                    $problemText
                    
                    IMPORTANT LaTeX FORMATTING RULES:
                    Provide your entire response in valid LaTeX body format (WITHOUT `${'\\'}begin{document}` or `${'\\'}end{document}`, and WITHOUT any preamble). 
                    Output ONLY the raw content that will be placed inside a LaTeX document body.
                    Use standard LaTeX math formatting (e.g., `${'$'}` for inline math and `${'\\'}[ ... ${'\\'}]` or `${'\\'}begin{equation}...${'\\'}end{equation}` for block math).
                    Do NOT wrap your response in markdown code blocks like ```latex ... ```. Just return the raw text.
                """.trimIndent()

                val aiResponse = try {
                    aiClient.generateContent(prompt)
                } catch (e: Exception) {
                    _state.value = MathSolverState.Error("AI Generation Failed: ${e.message}")
                    return@launch
                }
                
                val cleanLatex = aiResponse.removePrefix("```latex").replace("```latex\\n", "").removePrefix("```").removeSuffix("```").trim()

                _state.value = MathSolverState.CompilingPdf
                
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
                Timber.i("Writing LaTeX file to: ${texFile.absolutePath}")
                var fileOutputStream: FileOutputStream? = null
                try {
                    fileOutputStream = FileOutputStream(texFile)
                    fileOutputStream.write(fullLatex.toByteArray(Charsets.UTF_8))
                    fileOutputStream.flush()
                    Timber.d("LaTeX file writing complete.")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to write LaTeX file")
                    throw e
                } finally {
                    fileOutputStream?.close()
                }

                Timber.i("Calling TermuxXeLaTeXBridge.compile...")
                val compileResult = TermuxXeLaTeXBridge.compile(context = context, texFile = texFile)

                if (compileResult.isSuccess) {
                    val generatedPdf = compileResult.getOrNull()
                    if (generatedPdf != null && generatedPdf.exists()) {
                        _state.value = MathSolverState.Success(generatedPdf)
                    } else {
                        _state.value = MathSolverState.Error("PDF was generated but not found.")
                    }
                } else {
                    _state.value = MathSolverState.Error("LaTeX Compilation Failed:\n${compileResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _state.value = MathSolverState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    fun resetState() {
        _state.value = MathSolverState.Idle
    }
}
