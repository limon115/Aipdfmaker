import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Add required imports
imports = """
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.data.datastore.AiSettingsDataStore
import com.example.domain.models.ThemeMode
"""
if "import com.example.domain.models.ThemeMode" not in content:
    content = content.replace("import com.example.ui.theme.GlassBackground", "import com.example.ui.theme.GlassBackground" + imports)

# Wrap setContent with DataStore collection
target = """    setContent {
      MyApplicationTheme {"""

replacement = """    val dataStore = AiSettingsDataStore(applicationContext)
    
    setContent {
      val settings by dataStore.aiSettingsFlow.collectAsState(initial = null)
      val themeMode = settings?.themeMode ?: ThemeMode.SYSTEM
      val isDarkTheme = when (themeMode) {
          ThemeMode.LIGHT -> false
          ThemeMode.DARK -> true
          ThemeMode.SYSTEM -> isSystemInDarkTheme()
      }

      MyApplicationTheme(darkTheme = isDarkTheme) {"""

content = content.replace(target, replacement)

# We also need to pass `darkTheme = isDarkTheme` to GlassBackground so it can update its background color immediately.
target2 = """      MyApplicationTheme(darkTheme = isDarkTheme) {
        GlassBackground {"""
replacement2 = """      MyApplicationTheme(darkTheme = isDarkTheme) {
        GlassBackground(darkTheme = isDarkTheme) {"""
content = content.replace(target2, replacement2)

# Wait, GlassBackground needs to accept it. Let's check `GlassTheme.kt` for `GlassBackground` signature.
# In `GlassTheme.kt`, `GlassBackground` accepts `darkTheme: Boolean = isSystemInDarkTheme()`.
# So it's perfect!

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

