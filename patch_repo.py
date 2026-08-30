import re
with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'suspend fun compileAndExportPdf(\n        projectName: String,\n        latexContent: String\n    ): Result<Pair<File, File>>',
    'suspend fun compileAndExportPdf(\n        projectName: String,\n        latexContent: String,\n        fixScript: String? = null\n    ): Result<Pair<File, File>>'
)

old_fullLatex = '''            val fullLatex = """
                \\documentclass{article}
                \\usepackage{amsmath}
                \\usepackage{amsfonts}
                \\usepackage{amssymb}
                \\usepackage{fontspec}
                \\usepackage[Bengali]{ucharclasses}
                \\setmainfont{DejaVu Serif}
                \\newfontfamily\\bengalifont[
                    Path=/data/data/com.termux/files/home/,
                    Script=Bengali,
                    Language=Bengali,
                    AutoFakeBold=1.5,
                    AutoFakeSlant=0.2
                ]{solaiman.ttf}
                \\setTransitionsFor{Bengali}{\\bengalifont}{}
                \\setTransitionsFor{Devanagari}{\\bengalifont}{}
                \\setTransitionsFor{BasicLatin}{\\rmfamily}{}
                \\title{$projectName}
                \\begin{document}
                \\maketitle
                $latexContent
                \\end{document}
            """.trimIndent()'''

new_fullLatex = '''            val fullLatex = if (latexContent.contains("\\\\documentclass")) {
                latexContent
            } else {
                """
                \\documentclass{article}
                \\usepackage{amsmath}
                \\usepackage{amsfonts}
                \\usepackage{amssymb}
                \\usepackage{fontspec}
                \\usepackage[Bengali]{ucharclasses}
                \\setmainfont{DejaVu Serif}
                \\newfontfamily\\bengalifont[
                    Path=/data/data/com.termux/files/home/,
                    Script=Bengali,
                    Language=Bengali,
                    AutoFakeBold=1.5,
                    AutoFakeSlant=0.2
                ]{solaiman.ttf}
                \\setTransitionsFor{Bengali}{\\bengalifont}{}
                \\setTransitionsFor{Devanagari}{\\bengalifont}{}
                \\setTransitionsFor{BasicLatin}{\\rmfamily}{}
                \\title{$projectName}
                \\begin{document}
                \\maketitle
                $latexContent
                \\end{document}
                """.trimIndent()
            }'''

if old_fullLatex in content:
    content = content.replace(old_fullLatex, new_fullLatex)
else:
    print("Could not find old_fullLatex string!")

content = content.replace(
    'val result = TermuxXeLaTeXBridge.compile(\n                context = context,\n                texFile = texFile\n            )',
    'val result = TermuxXeLaTeXBridge.compile(\n                context = context,\n                texFile = texFile,\n                fixScript = fixScript\n            )'
)

with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'w') as f:
    f.write(content)
