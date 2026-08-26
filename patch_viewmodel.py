import re

with open("app/src/main/java/com/example/ui/screens/settings/SettingsViewModel.kt", "r") as f:
    content = f.read()

if "import com.example.domain.models.ThemeMode" not in content:
    content = content.replace("import com.example.domain.models.AiProvider", "import com.example.domain.models.AiProvider\nimport com.example.domain.models.ThemeMode")

if "fun updateThemeMode" not in content:
    method = """
    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch { dataStore.updateThemeMode(themeMode) }
    }
"""
    # Insert before testConnection
    content = content.replace("    fun testConnection(", method + "    fun testConnection(")

with open("app/src/main/java/com/example/ui/screens/settings/SettingsViewModel.kt", "w") as f:
    f.write(content)

