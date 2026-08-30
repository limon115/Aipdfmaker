with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'r') as f:
    content = f.read()

target = "\\usepackage{tcolorbox} % Added for rectbox fallback"
replacement = target + "\n                \\newenvironment{rectbox}{\\begin{tcolorbox}}{\\end{tcolorbox}}"

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'w') as f:
    f.write(content)
