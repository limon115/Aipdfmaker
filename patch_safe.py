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
            
            # Safe replacement for Scaffold containerColor
            content = re.sub(r'containerColor\s*=\s*MaterialTheme\.colorScheme\.background', 'containerColor = androidx.compose.ui.graphics.Color.Transparent', content)
            
            # Safe replacement for TopAppBar containerColor
            content = re.sub(r'containerColor\s*=\s*MaterialTheme\.colorScheme\.surface', 'containerColor = androidx.compose.ui.graphics.Color.Transparent', content)
            
            if content != original:
                with open(filepath, "w") as f:
                    f.write(content)
                print(f"Patched {filepath}")

