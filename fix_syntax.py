import os
import re

UI_DIR = "app/src/main/java/com/example/ui/screens"

for root, _, files in os.walk(UI_DIR):
    for file in files:
        if file.endswith(".kt"):
            filepath = os.path.join(root, file)
            with open(filepath, "r") as f:
                content = f.read()
            original = content
            
            # Syntax error: Unexpected tokens (use ';' to separate expressions on the same line).
            # This happens because the replacement `RoundedCornerShape(24.dp)` was inserted without proper parentheses in some edge cases like `shape = RoundedCornerShape(24.dp)) {`
            
            # Fix `shape = RoundedCornerShape(24.dp)) {` -> `shape = RoundedCornerShape(24.dp)) {` wait, no, `shape = RoundedCornerShape(24.dp)) {` has an extra closing paren!
            content = content.replace("shape = RoundedCornerShape(24.dp)) {", "shape = RoundedCornerShape(24.dp)) {")
            content = re.sub(r'shape\s*=\s*RoundedCornerShape\(24\.dp\)\)\s*\{', r'shape = RoundedCornerShape(24.dp)\n    ) {', content)

            # Unresolved reference 'GlassCard'. 
            # We need to ensure `import com.example.ui.theme.GlassCard` is present
            if "GlassCard(" in content and "import com.example.ui.theme.GlassCard" not in content:
                content = content.replace("import androidx.compose.runtime.Composable", "import androidx.compose.runtime.Composable\nimport com.example.ui.theme.GlassCard")

            if content != original:
                with open(filepath, "w") as f:
                    f.write(content)
                print(f"Fixed syntax in {filepath}")

