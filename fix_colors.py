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
            
            # Unresolved reference 'TransparentVariant' 
            # We must have accidentally replaced something wrongly or introduced a bad color reference.
            # Usually TransparentVariant doesn't exist. I'll replace it with Color.Transparent
            content = content.replace("TransparentVariant", "Transparent")

            if content != original:
                with open(filepath, "w") as f:
                    f.write(content)
                print(f"Fixed colors in {filepath}")

