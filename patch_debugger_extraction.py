import re

with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    content = f.read()

old_method = r'    suspend fun debugLatex\(latexCode: String, logContent: String\): String \{.*?\n    \}'

new_method = '''    suspend fun debugLatex(latexCode: String, logContent: String): String {
        val systemPrompt = """You are a strictly automated Python script generator for patching LaTeX files.
You will receive a LaTeX file and a compiler error log.
Your ONLY job is to output a valid Python 3 script that fixes the LaTeX errors via string replacement or regex.

CRITICAL RULES:
1. NO EXPLANATIONS. NO GREETINGS. NO THOUGHT PROCESS.
2. Output ONLY the raw Python code. 
3. The very first line of your response MUST be `import sys`.
4. Read the target file path from `sys.argv[1]`.
5. Read, patch, and overwrite the file.
6. Any reasoning or thought process MUST be inside Python comments (#).
""".trimIndent()

        val userPrompt = """LaTeX Code:
$latexCode

Compiler Log:
$logContent""".trimIndent()
        
        val rawResponse = generateContent(userPrompt, customSystemPrompt = systemPrompt, maxTokens = 8192)
        
        val codeBlockRegex = Regex("```(?:python)?(.*?)```", RegexOption.DOT_MATCHES_ALL)
        val matchResult = codeBlockRegex.find(rawResponse)
        
        return if (matchResult != null) {
            matchResult.groupValues[1].trim()
        } else {
            rawResponse.replace("```python", "").replace("```", "").trim()
        }
    }'''

content = re.sub(old_method, new_method, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'w') as f:
    f.write(content)
