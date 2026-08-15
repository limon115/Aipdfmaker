import os

filepath = "app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt"
with open(filepath, 'r') as f:
    code = f.read()

# 1. Update the LaTeX Preamble for Auto-Switching with Baskervville
old_latex = """            val fullLatex = \"\"\"
                \\documentclass{article}
                \\usepackage[utf8]{inputenc}
                \\usepackage{amsmath}
                \\usepackage{amsfonts}
                \\usepackage{amssymb}
                \\title{$projectName}
                \\begin{document}
                \\maketitle
                $latexContent
                \\end{document}
            \"\"\".trimIndent()"""

new_latex = """            val fullLatex = \"\"\"
                \\documentclass{article}
                \\usepackage{amsmath}
                \\usepackage{amsfonts}
                \\usepackage{amssymb}
                \\usepackage{fontspec}
                
                % 1. Set Baskervville as the premium main English font
                \\setmainfont{Baskervville.ttf}[Path=./]
                
                % 2. Auto-switch to Kalpurush ONLY for Bengali characters
                \\usepackage[Bengali]{ucharclasses}
                \\newfontfamily\\bengalifont{kalpurush.ttf}[Path=./]
                \\setTransitionsForBengali{\\begingroup\\bengalifont}{\\endgroup}
                
                \\title{$projectName}
                \\begin{document}
                \\maketitle
                $latexContent
                \\end{document}
            \"\"\".trimIndent()"""

# 2. Add the logic to copy BOTH fonts to the working directory
old_write = """            val texFile = File(baseDir, "document.tex")
            texFile.writeText(fullLatex)"""

new_write = """            // Copy Kalpurush (Bengali Font)
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

            val texFile = File(baseDir, "document.tex")
            texFile.writeText(fullLatex)"""

# Apply patches
if "\\setmainfont{Baskervville.ttf}" not in code:
    code = code.replace(old_latex, new_latex)
    code = code.replace(old_write, new_write)
    with open(filepath, 'w') as f:
        f.write(code)
    print("✅ LatexCompilerRepository patched: Baskervville + Kalpurush dual-font support injected!")
else:
    print("⚡ Baskervville support is already injected.")
