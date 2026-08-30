with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    content = f.read()

start_str = 'suspend fun debugLatex'
end_str = '    suspend fun generateBlueprint'
start_idx = content.find(start_str)
end_idx = content.find(end_str, start_idx)

new_method = '''suspend fun debugLatex(latexCode: String, logContent: String): String {
        val systemPrompt = """You are an elite Python and LaTeX debugging assistant. Read the following LaTeX code and the corresponding compiler log which contains errors.

Your task is to identify the bugs and write a Python script that injects the fixes into the code.

CRITICAL INSTRUCTIONS:
1. You MUST return ONLY a raw Python script.
2. The Python script should read the file path from sys.argv[1].
3. Open the file, read the contents, perform string replacements or regex substitutions to fix the LaTeX errors, and overwrite the file.
4. Do NOT output partial LaTeX code or full LaTeX code, ONLY output the Python script.
5. Do NOT include markdown blocks (like ```python). Just return the raw Python code.
6. The Python script will be executed locally to patch the user's LaTeX code before compilation.
""".trimIndent()

        val userPrompt = """LaTeX Code:
$latexCode

Compiler Log:
$logContent""".trimIndent()
        
        val rawResponse = generateContent(userPrompt, customSystemPrompt = systemPrompt, maxTokens = 8192)
        return rawResponse.replace("```python", "").replace("```", "").trim()
    }

'''

if start_idx != -1 and end_idx != -1:
    content = content[:start_idx] + new_method + content[end_idx:]

with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'w') as f:
    f.write(content)
