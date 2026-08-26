import re

with open("app/src/main/java/com/example/data/datastore/AiSettingsDataStore.kt", "r") as f:
    content = f.read()

# First, restore original structure if we messed up
if "suspend fun updateThemeMode" in content:
    content = re.sub(r'suspend fun updateThemeMode.*?\}', '', content, flags=re.DOTALL)

# Add method inside the class (before the closing brace of the class, which is before `data class AiSettings`)
# We can find `data class AiSettings` and insert before it.
parts = content.split("data class AiSettings")
if len(parts) == 2:
    # check if the method is already there
    if "suspend fun updateThemeMode" not in parts[0]:
        new_method = """    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = themeMode.name }
    }
}
"""
        # replace the last "}" in parts[0] with the new method
        parts[0] = parts[0].rsplit("}", 1)[0] + new_method

content = parts[0] + "data class AiSettings" + parts[1]

with open("app/src/main/java/com/example/data/datastore/AiSettingsDataStore.kt", "w") as f:
    f.write(content)

