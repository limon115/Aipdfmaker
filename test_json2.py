import re
with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    content = f.read()

start_str = 'suspend fun debugLatex'
end_str = '    suspend fun generateBlueprint'
start_idx = content.find(start_str)
end_idx = content.find(end_str, start_idx)

new_method = '''suspend fun debugLatex(latexCode: String, logContent: String): String {
        val systemPrompt = """You are an automated Python script generator for patching LaTeX files.
Your ONLY job is to output a valid JSON object containing a Python script.
The Python script MUST read the file path from `sys.argv[1]`, fix the LaTeX errors via string replacement or regex, and overwrite the file.

CRITICAL RULES:
1. You MUST respond with ONLY a valid JSON object.
2. The JSON MUST have exactly two keys: "thought_process" (string, your reasoning) and "python_script" (string, the raw python code).
3. The python_script string MUST be a valid, raw Python 3 script (starting with import sys).
4. Do NOT wrap the JSON in markdown blocks. Output pure JSON.
""".trimIndent()

        val userPrompt = """LaTeX Code:
$latexCode

Compiler Log:
$logContent""".trimIndent()
        
        val rawResponse = generateContent(
            prompt = userPrompt, 
            customSystemPrompt = systemPrompt, 
            mimeType = "application/json",
            maxTokens = 8192
        )
        
        try {
            val cleanJsonStr = rawResponse.replace("```json", "").replace("```", "").trim()
            val jsonObj = org.json.JSONObject(cleanJsonStr)
            return jsonObj.optString("python_script", rawResponse)
        } catch (e: Exception) {
            // Fallback if parsing fails
            com.example.utils.AppLogger.e("LatexDebugger", "Failed to parse JSON response", e)
            val codeBlockRegex = Regex("```(?:python)?(.*?)```", RegexOption.DOT_MATCHES_ALL)
            val matchResult = codeBlockRegex.find(rawResponse)
            return if (matchResult != null) {
                matchResult.groupValues[1].trim()
            } else {
                rawResponse
            }
        }
    }

'''

if start_idx != -1 and end_idx != -1:
    content = content[:start_idx] + new_method + content[end_idx:]
    with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'w') as f:
        f.write(content)
