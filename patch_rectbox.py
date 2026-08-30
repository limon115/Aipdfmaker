import re
with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '\\\\usepackage{tcolorbox} % Added for rectbox fallback',
    '\\\\usepackage{tcolorbox} % Added for rectbox fallback\n                \\\\newenvironment{rectbox}{\\\\begin{tcolorbox}}{\\\\end{tcolorbox}}'
)

with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'w') as f:
    f.write(content)
