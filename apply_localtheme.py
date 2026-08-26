import re
import os

files_to_check = [
    "app/src/main/java/com/example/ui/theme/GlassTheme.kt",
    "app/src/main/java/com/example/ui/theme/GlassComponents.kt",
    "app/src/main/java/com/example/ui/screens/MainScreen.kt",
]

for file in files_to_check:
    with open(file, "r") as f:
        content = f.read()
    
    # Check if we are checking isSystemInDarkTheme
    # Instead of just `isSystemInDarkTheme()`, we want `com.example.ui.theme.LocalThemeIsDark.current`
    if file == "app/src/main/java/com/example/ui/theme/GlassTheme.kt":
        content = content.replace("darkTheme: Boolean = isSystemInDarkTheme()", "darkTheme: Boolean = LocalThemeIsDark.current")
        content = content.replace("val isDark = isSystemInDarkTheme()", "val isDark = LocalThemeIsDark.current")
        # Remove import
        content = content.replace("import androidx.compose.foundation.isSystemInDarkTheme\n", "")
    elif file == "app/src/main/java/com/example/ui/theme/GlassComponents.kt":
        content = content.replace("val isDark = isSystemInDarkTheme()", "val isDark = LocalThemeIsDark.current")
        content = content.replace("import androidx.compose.foundation.isSystemInDarkTheme\n", "")
    elif file == "app/src/main/java/com/example/ui/screens/MainScreen.kt":
        content = content.replace("androidx.compose.foundation.isSystemInDarkTheme()", "com.example.ui.theme.LocalThemeIsDark.current")

    with open(file, "w") as f:
        f.write(content)

