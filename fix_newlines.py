import os

UI_DIR = "app/src/main/java/com/example/ui"

for root, _, files in os.walk(UI_DIR):
    for file in files:
        if file.endswith(".kt"):
            filepath = os.path.join(root, file)
            with open(filepath, "r") as f:
                content = f.read()
            original = content
            
            content = content.replace("package com.example.ui.screensimport", "package com.example.ui.screens\n\nimport")
            content = content.replace("package com.example.ui.navigationimport", "package com.example.ui.navigation\n\nimport")
            content = content.replace("package com.example.uiimport", "package com.example.ui\n\nimport")

            if content != original:
                with open(filepath, "w") as f:
                    f.write(content)
                print(f"Fixed newlines in {filepath}")

