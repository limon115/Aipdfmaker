import re

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
    ".androidx.compose.ui.draw.shadow",
    ".shadow"
)

# ensure shadow import
if "import androidx.compose.ui.draw.shadow" not in content:
    content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.draw.shadow")

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(content)
print("Modifier fixed.")
