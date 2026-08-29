import re

# Fix AiNetworkClient.kt
with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'r') as f:
    content = f.read()

correct_method = """
    suspend fun debugLatex(latexCode: String, logContent: String): String {
        val systemPrompt = "You are an elite LaTeX debugging assistant. Read the following LaTeX code and the corresponding compiler log which contains errors. Your task is to identify and fix all errors, warnings, and formatting issues. You MUST return ONLY the completely rewritten, compiling LaTeX code. Do NOT include any markdown code blocks, explanations, or wrapper text. Just the raw LaTeX code."
        val userPrompt = "LaTeX Code:\\n" + latexCode + "\\n\\nCompiler Log:\\n" + logContent
        val rawResponse = generateContent(userPrompt, customSystemPrompt = systemPrompt)
        return rawResponse.replace("```latex", "").replace("```", "").trim()
    }
"""

content = re.sub(r'suspend fun debugLatex.*?return rawResponse\.replace\("```latex", ""\)\.replace\("```", ""\)\.trim\(\)\n    \}', correct_method.strip(), content, flags=re.DOTALL)
with open('app/src/main/java/com/example/data/network/AiNetworkClient.kt', 'w') as f:
    f.write(content)

# Fix MainScreen.kt
with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    main_content = f.read()
    
# Remove duplicate imports and bad imports from my previous patch
main_content = re.sub(r'import androidx\.compose\.foundation\.clickable.*?import androidx\.compose\.material\.icons\.Icons', '', main_content, flags=re.DOTALL)
main_content = main_content.strip()

# Make sure it starts with package
if not main_content.startswith("package"):
    main_content = "package com.example.ui.screens\n" + main_content

# Add only missing imports right after package
main_content = main_content.replace('package com.example.ui.screens', 'package com.example.ui.screens\n\nimport androidx.compose.material.icons.filled.Build\nimport androidx.compose.foundation.clickable\nimport androidx.compose.foundation.layout.width\n')

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(main_content)

# Fix AiSettingsScreen.kt syntax
with open('app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'r') as f:
    settings_content = f.read()

# I need to fix the stray syntax on line 172. 
settings_content = re.sub(r'\},(\s*)DeveloperToolsCard', r'\1DeveloperToolsCard', settings_content)

# Actually, let's just make sure AiConfigCard is closed properly
with open('app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'w') as f:
    f.write(settings_content)

