import re

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    content = f.read()

target = """    Scaffold(
        bottomBar = {"""

replacement = """    com.example.ui.theme.GlassBackground {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {"""

content = content.replace(target, replacement)

# Add closing brace for GlassBackground before the last brace
content = content.rstrip()
if content.endswith("}"):
    content = content[:-1] + "    }\n}"

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(content)
print("Scaffold wrapped in GlassBackground.")
