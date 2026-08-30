import re

with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    content = f.read()

correct_method = """
    suspend fun debugLatex(latexCode: String, logContent: String): String {
        val systemPrompt = "You are an elite LaTeX debugging assistant. Read the following LaTeX code and the corresponding compiler log which contains errors.\\n\\nYour task is to identify and fix ALL errors, warnings, and formatting issues.\\n\\nCRITICAL INSTRUCTIONS:\\n1. You MUST return the ENTIRE, complete LaTeX document from \\\\documentclass to \\\\end{document}.\\n2. Do NOT output partial code.\\n3. Do NOT truncate the document.\\n4. Do NOT use placeholders like '% ... rest of code ...' or '\\\\dots'.\\n5. You MUST return ONLY the raw compiling LaTeX code.\\n6. Do NOT include any markdown code blocks (like ```latex), explanations, or wrapper text. Just the raw text."
        val userPrompt = "LaTeX Code:\\n$latexCode\\n\\nCompiler Log:\\n$logContent"
        val rawResponse = generateContent(userPrompt, customSystemPrompt = systemPrompt)
        return rawResponse.replace("```latex", "").replace("```", "").trim()
    }
"""

content = re.sub(r'suspend fun debugLatex.*?return rawResponse\.replace\("```latex", ""\)\.replace\("```", ""\)\.trim\(\)\n    \}', correct_method.strip(), content, flags=re.DOTALL)
with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'w') as f:
    f.write(content)
