import re

with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'r') as f:
    content = f.read()

# Replace the part after `texFile.writeText(fullLatex)`
old_code = """val result = TermuxXeLaTeXBridge.compile(
                context = context,
                texFile = texFile,
                fixScript = fixScript
            ).getOrElse { error ->
                throw Exception(
                    "XeLaTeX compilation failed: ${error.message}",
                    error
                )
            }

            val targetPdf = File(baseDir, "document.pdf")
            result.copyTo(
                targetPdf,
                overwrite = true
            )

            Pair(targetPdf, texFile)"""

new_code = """val doubleCompileScript = \"\"\"
                xelatex main.tex
                cp main.aux "${baseDir.absolutePath}/main.aux"
                cp main.log "${baseDir.absolutePath}/main.log"
                ${fixScript ?: ""}
            \"\"\".trimIndent()

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

            // result is already main.pdf in baseDir
            Pair(result, texFile)"""

content = content.replace(old_code.strip(), new_code.strip())

# The original has slightly different indentation maybe? Let's use regex
content = re.sub(
    r'val result = TermuxXeLaTeXBridge\.compile.*?Pair\(targetPdf, texFile\)', 
    new_code, 
    content, 
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/domain/repository/LatexCompilerRepository.kt', 'w') as f:
    f.write(content)
