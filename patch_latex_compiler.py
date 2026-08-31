import re

with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'r') as f:
    content = f.read()

# Add import
content = content.replace('import java.io.File', 'import java.io.File\nimport com.example.data.database.ProjectEntity')

# Change signature
content = content.replace('suspend fun compileAndExportPdf(\n        projectName: String,\n        latexContent: String,\n        fixScript: String? = null\n    ): Result<Pair<File, File>>', 'suspend fun compileAndExportPdf(\n        project: ProjectEntity,\n        latexContent: String,\n        fixScript: String? = null\n    ): Result<Pair<File, File>>')
content = content.replace('val safeName = projectName', 'val safeName = project.title')

# Isolate the `val fullLatex = ...` part
start_idx = content.find('val fullLatex = if')
end_idx = content.find('val texFile = File', start_idx)

old_latex = content[start_idx:end_idx]

new_latex = """val fullLatex = if (latexContent.contains("\\\\documentclass")) {
                latexContent
            } else {
                val subjectName = project.course.ifBlank { "Subject" }
                val chapterName = project.chapter.ifBlank { "Chapter" }
                val detailsText = project.description.ifBlank { "Comprehensive Study Notes" }
                
                "\"\"
                \\\\documentclass[letterpaper]{article}
                \\\\usepackage[margin=1in]{geometry}
                \\\\usepackage{xcolor}
                \\\\usepackage{amsmath}
                \\\\usepackage{amsfonts}
                \\\\usepackage{amssymb}
                \\\\usepackage{fontspec}
                \\\\usepackage{ucharclasses}
                \\\\usepackage{tikz}
                \\\\usepackage{circuitikz}
                \\\\usepackage{booktabs}
                \\\\usepackage{tcolorbox}
                \\\\newenvironment{rectbox}{\\\\begin{tcolorbox}}{\\\\end{tcolorbox}}
                \\\\setmainfont{DejaVu Serif}
                \\\\newfontfamily\\\\bengalifont[
                    Path=/data/data/com.termux/files/home/,
                    Script=Bengali,
                    Language=Bengali,
                    AutoFakeBold=1.5,
                    AutoFakeSlant=0.2
                ]{solaiman.ttf}
                \\\\setTransitionsFor{Bengali}{\\\\bengalifont}{}
                \\\\setTransitionsFor{Devanagari}{\\\\bengalifont}{}
                \\\\setTransitionsFor{BasicLatin}{\\\\rmfamily}{}

                % --- Signature Design Color ---
                \\\\definecolor{titlepagecolor}{cmyk}{1,.60,0,.40}

                % --- Professional Serif Typography (Palatino) ---
                \\\\DeclareFixedFont{\\\\subjectfont}{T1}{ppl}{b}{it}{0.5in}
                \\\\DeclareFixedFont{\\\\chapterfont}{T1}{ppl}{b}{n}{0.35in}

                \\\\makeatletter                       
                \\\\def\\\\printauthor{%                  
                    {\\\\large \\\\@author}}              
                \\\\makeatother

                % --- Author Information ---
                \\\\author{%
                    Khalid Hasan Limon \\\\\\\\
                    HSC 26 \\\\\\\\
                    \\\\texttt{Study Notes}
                }

                % --- Graphical Decoration ---
                \\\\newcommand\\\\titlepagedecoration{%
                \\\\begin{tikzpicture}[remember picture,overlay,shorten >= -10pt]
                \\\\coordinate (aux1) at ([yshift=-15pt]current page.north east);
                \\\\coordinate (aux2) at ([yshift=-410pt]current page.north east);
                \\\\coordinate (aux3) at ([xshift=-4.5cm]current page.north east);
                \\\\coordinate (aux4) at ([yshift=-150pt]current page.north east);
                \\\\begin{scope}[titlepagecolor!40,line width=12pt,rounded corners=12pt]
                \\\\draw
                  (aux1) -- coordinate (a)
                  ++(225:5) --
                  ++(-45:5.1) coordinate (b);
                \\\\draw[shorten <= -10pt]
                  (aux3) --
                  (a) --
                  (aux1);
                \\\\draw[opacity=0.6,titlepagecolor,shorten <= -10pt]
                  (b) --
                  ++(225:2.2) --
                  ++(-45:2.2);
                \\\\end{scope}
                \\\\draw[titlepagecolor,line width=8pt,rounded corners=8pt,shorten <= -10pt]
                  (aux4) --
                  ++(225:0.8) --
                  ++(-45:0.8);
                \\\\begin{scope}[titlepagecolor!70,line width=6pt,rounded corners=8pt]
                \\\\draw[shorten <= -10pt]
                  (aux2) --
                  ++(225:3) coordinate[pos=0.45] (c) --
                  ++(-45:3.1);
                \\\\draw
                  (aux2) --
                  (c) --
                  ++(135:2.5) --
                  ++(45:2.5) --
                  ++(-45:2.5) coordinate[pos=0.3] (d);   
                \\\\draw 
                  (d) -- +(45:1);
                \\\\end{scope}
                \\\\end{tikzpicture}%
                }

                \\\\begin{document}
                \\\\begin{titlepage}
                \\\\noindent
                \\\\subjectfont ${subjectName}\\\\par
                \\\\vspace{0.8cm}
                \\\\noindent
                \\\\chapterfont ${chapterName}\\\\par
                \\\\vspace{1.2cm}
                \\\\noindent
                \\\\Large \\\\textit{${detailsText}}

                \\\\null\\\\vfill
                \\\\vspace*{1cm}
                \\\\noindent
                \\\\hfill
                \\\\begin{minipage}{0.4\\\\linewidth}
                    \\\\begin{flushright}
                        \\\\printauthor
                    \\\\end{flushright}
                \\\\end{minipage}
                %
                \\\\begin{minipage}{0.02\\\\linewidth}
                    \\\\rule{1pt}{125pt}
                \\\\end{minipage}
                \\\\titlepagedecoration
                \\\\end{titlepage}

                $latexContent
                \\\\end{document}
                \"\"\".trimIndent()
            }
            """

content = content.replace(old_latex, new_latex)

with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'w') as f:
    f.write(content)
