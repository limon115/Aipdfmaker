with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'r') as f:
    content = f.read()

start_str = 'val fullLatex = """'
end_str = '""".trimIndent()'
start_idx = content.find(start_str)
end_idx = content.find(end_str, start_idx) + len(end_str)

new_logic = '''val fullLatex = if (latexContent.contains("\\\\documentclass")) {
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

if start_idx != -1 and end_idx != -1:
    content = content[:start_idx] + new_logic + content[end_idx:]

with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'w') as f:
    f.write(content)
