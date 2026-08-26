import re

# Update MainActivity.kt
with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("GlassBackground(darkTheme = isDarkTheme) {", "GlassBackground {")
with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

# Update GlassComponents.kt
with open("app/src/main/java/com/example/ui/theme/GlassComponents.kt", "r") as f:
    content = f.read()

target_components = """    val isDark = LocalThemeIsDark.current
    val bgColor = if (isDark) SurfaceDark else SurfaceLight
    val borderColor = if (isDark) BorderDark else BorderLight"""

replacement_components = """    val colors = AppTheme.colors
    val bgColor = colors.surface
    val borderColor = colors.border"""

content = content.replace(target_components, replacement_components)

# Also update the alpha copies
content = content.replace("if (isDark) Color(0xFF111827).copy(alpha = 0.3f) else Color(0xFFFFFFFF).copy(alpha = 0.3f)", "colors.surface.copy(alpha = 0.3f)")
content = content.replace("if (isDark) TextSecondaryDark else TextSecondaryLight", "colors.textSecondary")
content = content.replace("if (isDark) TextPrimaryDark else TextPrimaryLight", "colors.textPrimary")
content = content.replace("if (isDark) TextPrimaryDark else TextPrimaryLight", "colors.textPrimary")

with open("app/src/main/java/com/example/ui/theme/GlassComponents.kt", "w") as f:
    f.write(content)
