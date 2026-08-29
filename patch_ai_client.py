import re

with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    content = f.read()

debug_latex_method = """
    suspend fun debugLatex(latexCode: String, logContent: String): String {
        val systemPrompt = "You are an elite LaTeX debugging assistant. Read the following LaTeX code and the corresponding compiler log which contains errors. Your task is to identify and fix all errors, warnings, and formatting issues. You MUST return ONLY the completely rewritten, compiling LaTeX code. Do NOT include any markdown code blocks, explanations, or wrapper text. Just the raw LaTeX code."
        
        val userPrompt = "LaTeX Code:\\n$latexCode\\n\\nCompiler Log:\\n$logContent"
        
        val rawResponse = callAi(userPrompt, systemPrompt)
        return cleanLatex(rawResponse)
    }
"""

content = re.sub(r'suspend fun generateBlueprint', debug_latex_method + '\n    suspend fun generateBlueprint', content)

with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'w') as f:
    f.write(content)

