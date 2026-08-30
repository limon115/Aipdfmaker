with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    content = f.read()

start_index = content.find('suspend fun debugLatex')
end_index = content.find('suspend fun generateBlueprint')

correct_method = '''suspend fun debugLatex(latexCode: String, logContent: String): String {
        val systemPrompt = """You are an elite LaTeX debugging assistant. Read the following LaTeX code and the corresponding compiler log which contains errors.

Your task is to identify and fix ALL errors, warnings, and formatting issues.

CRITICAL INSTRUCTIONS:
1. You MUST return the ENTIRE, complete LaTeX document from \\documentclass to \\end{document}.
2. Do NOT output partial code.
3. Do NOT truncate the document.
4. Do NOT use placeholders like '% ... rest of code ...' or '\\dots'.
5. You MUST return ONLY the raw compiling LaTeX code.
6. Do NOT include any markdown code blocks (like ```latex), explanations, or wrapper text. Just the raw text.""".trimIndent()

        val userPrompt = """LaTeX Code:
$latexCode

Compiler Log:
$logContent""".trimIndent()
        
        val rawResponse = generateContent(userPrompt, customSystemPrompt = systemPrompt, maxTokens = 8192)
        return rawResponse.replace("```latex", "").replace("```", "").trim()
    }

    '''

if start_index != -1 and end_index != -1:
    new_content = content[:start_index] + correct_method + content[end_index:]
    with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'w') as f:
        f.write(new_content)
else:
    print("Could not find start or end index!")
