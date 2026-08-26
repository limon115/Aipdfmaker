import re

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    content = f.read()

content = content.replace("val isDark = com.example.ui.theme.LocalThemeIsDark.current", "val colors = com.example.ui.theme.AppTheme.colors")
content = content.replace("if (isDark) com.example.ui.theme.BorderDark else com.example.ui.theme.BorderLight", "colors.border")
content = content.replace("if (isDark) com.example.ui.theme.SurfaceElevatedDark else com.example.ui.theme.SurfaceElevatedLight", "colors.surfaceElevated")

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(content)
