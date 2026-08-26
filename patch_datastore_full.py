with open("app/src/main/java/com/example/data/datastore/AiSettingsDataStore.kt", "r") as f:
    content = f.read()

# Make sure imports have ThemeMode
if "import com.example.domain.models.ThemeMode" not in content:
    content = content.replace("import com.example.domain.models.AiProvider", "import com.example.domain.models.AiProvider\nimport com.example.domain.models.ThemeMode")

# Add THEME_MODE key
if "THEME_MODE" not in content:
    content = content.replace("val AI2_TOP_P = floatPreferencesKey(\"ai2_top_p\")", 
                              "val AI2_TOP_P = floatPreferencesKey(\"ai2_top_p\")\n        val THEME_MODE = stringPreferencesKey(\"theme_mode\")")

# Add to AiSettings data class
if "val themeMode: ThemeMode" not in content:
    content = content.replace("val ai2TopP: Float = 1.0f\n)", "val ai2TopP: Float = 1.0f,\n    val themeMode: ThemeMode = ThemeMode.SYSTEM\n)")

# Add to aiSettingsFlow
if "themeMode =" not in content:
    content = content.replace("ai2TopP = preferences[AI2_TOP_P] ?: 1.0f\n            )", "ai2TopP = preferences[AI2_TOP_P] ?: 1.0f,\n                themeMode = runCatching { ThemeMode.valueOf(preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM)\n            )")

with open("app/src/main/java/com/example/data/datastore/AiSettingsDataStore.kt", "w") as f:
    f.write(content)
