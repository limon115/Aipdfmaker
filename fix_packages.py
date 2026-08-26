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
            
            # The bad string replacement made `package com.example.ui\n\nimport com.example.ui.theme.CustomIcons.screens`
            # and `package com.example.ui\n\nimport com.example.ui.theme.CustomIcons.navigation` etc
            
            # First, clean up ANY `import com.example.ui.theme.CustomIcons` to avoid duplicates
            lines = content.split('\n')
            new_lines = []
            
            for line in lines:
                if line.startswith("package com.example.ui"):
                    if ".theme.CustomIcons" in line:
                        pass # Wait, let's just use regex to fix the exact mistake
                        
            # Regex to find `package com.example.ui\n\nimport com.example.ui.theme.CustomIcons(.[a-zA-Z0-9_.]*)`
            # And replace it with `package com.example.ui\2\n\nimport com.example.ui.theme.CustomIcons`
            content = re.sub(
                r'package com\.example\.ui\n\nimport com\.example\.ui\.theme\.CustomIcons(\.[a-zA-Z0-9_]+)',
                r'package com.example.ui\1\n\nimport com.example.ui.theme.CustomIcons',
                content
            )

            if content != original:
                with open(filepath, "w") as f:
                    f.write(content)
                print(f"Fixed packages in {filepath}")

