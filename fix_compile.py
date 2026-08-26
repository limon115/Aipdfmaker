import os
import re
import subprocess

UI_DIR = "app/src/main/java/com/example/ui/screens"

for root, _, files in os.walk(UI_DIR):
    for file in files:
        if file.endswith(".kt"):
            filepath = os.path.join(root, file)
            with open(filepath, "r") as f:
                content = f.read()
            original = content
            
            # Unresolved reference 'GlassCard'. 
            # We need to ensure `import com.example.ui.theme.GlassCard` is present
            if "GlassCard" in content and "import com.example.ui.theme.GlassCard" not in content:
                content = content.replace("import androidx.compose.runtime.Composable", "import androidx.compose.runtime.Composable\nimport com.example.ui.theme.GlassCard")

            # Syntax error: Unexpected tokens (use ';' to separate expressions on the same line).
            # Fix `shape = RoundedCornerShape(24.dp)) {` -> `shape = RoundedCornerShape(24.dp)\n    ) {`
            content = content.replace("shape = RoundedCornerShape(24.dp)) {", "shape = RoundedCornerShape(24.dp)\n    ) {")

            if content != original:
                with open(filepath, "w") as f:
                    f.write(content)
                print(f"Fixed {filepath}")

# Double check processing screens
subprocess.run(["sed", "-i", "s/shape = RoundedCornerShape(24.dp)) {/shape = RoundedCornerShape(24.dp)) {/g", "app/src/main/java/com/example/ui/screens/processing/TopicsDetectedScreen.kt"])
