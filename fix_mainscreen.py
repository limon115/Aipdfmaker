import re

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    content = f.read()

# Fix 1: border
content = content.replace(".androidx.compose.foundation.border(", ".border(")
if "import androidx.compose.foundation.border" not in content:
    content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport androidx.compose.foundation.border")

# Fix 2: CenteredText
content = content.replace("CenteredText(\"Import from File (Placeholder)\")", 
"""Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Import from File (Placeholder)")
                }""")

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(content)
print("MainScreen fixed.")
