import re
with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'r') as f:
    content = f.read()

new_logic = '''            val fullLatex = if (latexContent.contains("\\\\documentclass")) {
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

content = re.sub(r'val fullLatex = """(.*?)""".trimIndent\(\)', new_logic.strip(), content, flags=re.DOTALL)

with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'w') as f:
    f.write(content)
