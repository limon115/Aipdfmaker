import re

with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    content = f.read()

correct_method = """
    suspend fun debugLatex(latexCode: String, logContent: String): String {
        val systemPrompt = "You are an elite LaTeX debugging assistant. Read the following LaTeX code and the corresponding compiler log which contains errors. Your task is to identify and fix all errors, warnings, and formatting issues. You MUST return ONLY the completely rewritten, compiling LaTeX code. Do NOT include any markdown code blocks, explanations, or wrapper text. Just the raw LaTeX code."
        val userPrompt = "LaTeX Code:\\n$latexCode\\n\\nCompiler Log:\\n$logContent"
        val rawResponse = generateContent(userPrompt, customSystemPrompt = systemPrompt)
        return rawResponse.replace("```latex", "").replace("```", "").trim()
    }
"""

content = re.sub(r'suspend fun debugLatex.*?return cleanLatex\(rawResponse\)\n    \}', correct_method.strip(), content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'w') as f:
    f.write(content)
