import os
import re

UI_DIR = "app/src/main/java/com/example/ui"

for root, _, files in os.walk(UI_DIR):
    for file in files:
        if file.endswith(".kt"):
            filepath = os.path.join(root, file)
            with open(filepath, "r") as f:
                content = f.read()
            original = content
            
            # Remove hardcoded White/Color(0xFFE8F5E9) for Cards
            content = re.sub(r'containerColor\s*=\s*(Color\.White|androidx\.compose\.ui\.graphics\.Color\.White|Color\(0xFFE8F5E9\))', 'containerColor = androidx.compose.ui.graphics.Color.Transparent', content)
            
            if content != original:
                with open(filepath, "w") as f:
                    f.write(content)
                print(f"Removed hardcoded colors in {filepath}")
