package com.example.domain.repository

import android.content.Context
import android.os.Environment
import com.example.domain.services.pdf.TermuxXeLaTeXBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.example.data.database.ProjectEntity
import timber.log.Timber
import java.io.FileOutputStream

class LatexCompilerRepository(private val context: Context) {

    fun buildFullLatex(project: ProjectEntity, latexContent: String): String {
        if (latexContent.contains("\\documentclass")) {
            return latexContent
        }
        val subjectName = project.course.ifBlank { "Subject" }
        val chapterName = project.chapter.ifBlank { "Chapter" }
        val detailsText = project.description.ifBlank { "Comprehensive Study Notes" }
        
        return """
        \documentclass[letterpaper]{article}
        \usepackage[margin=1in]{geometry}
        \usepackage{xcolor}
        \usepackage{amsmath}
        \usepackage{amsfonts}
        \usepackage{amssymb}
        \usepackage{fontspec}
        \usepackage{ucharclasses}
        \usepackage{tikz}
        \usepackage{pgfplots}
        \pgfplotsset{compat=1.18}
        \usepackage{circuitikz}
        \usepackage{booktabs}
        \usepackage{tcolorbox}
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
        % --- Signature Design Color ---
        \definecolor{titlepagecolor}{cmyk}{1,.60,0,.40}
        % --- Professional Serif Typography (Palatino) ---
        \DeclareFixedFont{\subjectfont}{T1}{ppl}{b}{it}{0.5in}
        \DeclareFixedFont{\chapterfont}{T1}{ppl}{b}{n}{0.35in}
        \makeatletter                       
        \def\printauthor{%                  
            {\large \@author}}              
        \makeatother
        % --- Author Information ---
        \author{%
            Khalid Hasan Limon \\\\
            HSC 26 \\\\
            \texttt{Study Notes}
        }
        % --- Graphical Decoration ---
        \newcommand\titlepagedecoration{%
        \begin{tikzpicture}[remember picture,overlay,shorten >= -10pt]
        \coordinate (aux1) at ([yshift=-15pt]current page.north east);
        \coordinate (aux2) at ([yshift=-410pt]current page.north east);
        \coordinate (aux3) at ([xshift=-4.5cm]current page.north east);
        \coordinate (aux4) at ([yshift=-150pt]current page.north east);
        \begin{scope}[titlepagecolor!40,line width=12pt,rounded corners=12pt]
        \draw
          (aux1) -- coordinate (a)
          ++(225:5) --
          ++(-45:5.1) coordinate (b);
        \draw[shorten <= -10pt]
          (aux3) --
          (a) --
          (aux1);
        \draw[opacity=0.6,titlepagecolor,shorten <= -10pt]
          (b) --
          ++(225:2.2) --
          ++(-45:2.2);
        \end{scope}
        \draw[titlepagecolor,line width=8pt,rounded corners=8pt,shorten <= -10pt]
          (aux4) --
          ++(225:0.8) --
          ++(-45:0.8);
        \begin{scope}[titlepagecolor!70,line width=6pt,rounded corners=8pt]
        \draw[shorten <= -10pt]
          (aux2) --
          ++(225:3) coordinate[pos=0.45] (c) --
          ++(-45:3.1);
        \draw
          (aux2) --
          (c) --
          ++(135:2.5) --
          ++(45:2.5) --
          ++(-45:2.5) coordinate[pos=0.3] (d);   
        \draw 
          (d) -- +(45:1);
        \end{scope}
        \end{tikzpicture}%
        }
        
        \begin{document}
        \XeTeXinterchartokenstate=1
        \begin{titlepage}
        \noindent
        \subjectfont ${subjectName}\par
        \vspace{0.8cm}
        \noindent
        \chapterfont ${chapterName}\par
        \vspace{1.2cm}
        \noindent
        \Large \textit{${detailsText}}
        
        \null\vfill
        \vspace*{1cm}
        \noindent
        \hfill
        \begin{minipage}{0.4\linewidth}
            \begin{flushright}
                \printauthor
            \end{flushright}
        \end{minipage}
        %
        \begin{minipage}{0.02\linewidth}
            \rule{1pt}{125pt}
        \end{minipage}
        \titlepagedecoration
        \end{titlepage}
        
        ${latexContent}
        \end{document}
        """.trimIndent()
    }

    suspend fun compileAndExportPdf(
        onProgress: (Float) -> Unit = {},

        project: ProjectEntity,
        latexContent: String,
        fixScript: String? = null
    ): Result<Pair<File, File>> = withContext(Dispatchers.IO) {
        runCatching {
            val safeName = project.title
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

            onProgress(0.2f)
            val fullLatex = buildFullLatex(project, latexContent)
            val texFile = File(baseDir, "main.tex")
            onProgress(0.4f)
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

            onProgress(0.6f)
            val doubleCompileScript = """
                xelatex -interaction=nonstopmode -halt-on-error main.tex
                ${fixScript ?: ""}
            """.trimIndent()

            val result = TermuxXeLaTeXBridge.compile(
                context = context,
                texFile = texFile,
                fixScript = doubleCompileScript
            ).getOrElse { error ->
                throw Exception(
                    "XeLaTeX compilation failed: ${error.message}",
                    error
                )
            }

            onProgress(1.0f)
            // result is already main.pdf in baseDir
            Pair(result, texFile)
        }
    }
}
