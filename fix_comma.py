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
            
            # regex to fix `,\s*\)` -> `)`
            content = re.sub(r',\s*\)', ')', content)
            
            # Or if it's `,\s*)\s*{`
            content = re.sub(r',\s*\)\s*\{', ') {', content)
            
            if content != original:
                with open(filepath, "w") as f:
                    f.write(content)
                print(f"Fixed commas in {filepath}")

