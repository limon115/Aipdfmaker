import os

filepath = "app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt"
with open(filepath, 'r') as f:
    code = f.read()

# 1. Update the LaTeX Preamble to use fontspec and load Kalpurush locally
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
                \\setmainfont{kalpurush.ttf}[Path=./]
                \\title{$projectName}
                \\begin{document}
                \\maketitle
                $latexContent
                \\end{document}
            \"\"\".trimIndent()"""

# 2. Add the Kotlin logic to copy the font from Android assets to the working directory
old_write = """            val texFile = File(baseDir, "document.tex")
            texFile.writeText(fullLatex)"""

new_write = """            // Copy the Kalpurush font to the working directory so Tectonic can find it
            val fontFile = File(baseDir, "kalpurush.ttf")
            if (!fontFile.exists()) {
                context.assets.open("fonts/kalpurush.ttf").use { input ->
                    fontFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val texFile = File(baseDir, "document.tex")
            texFile.writeText(fullLatex)"""

if "\\usepackage{fontspec}" not in code:
    code = code.replace(old_latex, new_latex)
    code = code.replace(old_write, new_write)
    with open(filepath, 'w') as f:
        f.write(code)
    print("✅ LatexCompilerRepository patched: Bangla fontspec support and local asset copying injected!")
else:
    print("⚡ Bangla support is already injected.")
