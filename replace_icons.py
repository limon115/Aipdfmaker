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
            
            content = re.sub(r'Icons\.(Default|Filled|Outlined)\.Edit', 'CustomIcons.Pencil', content)
            content = re.sub(r'Icons\.(Default|Filled|Outlined)\.Folder', 'CustomIcons.Folder', content)
            content = re.sub(r'Icons\.(Default|Filled|Outlined)\.Settings', 'CustomIcons.SettingsGear', content)
            content = re.sub(r'Icons\.(Default|Filled|Outlined)\.(AutoAwesome|Star)', 'CustomIcons.Sparkles', content)
            
            if "CustomIcons." in content and "import com.example.ui.theme.CustomIcons" not in content:
                content = content.replace("package com.example.ui", "package com.example.ui\n\nimport com.example.ui.theme.CustomIcons")

            # Fix imports for CustomIcons if it didn't get added properly (e.g. package com.example.ui.screens...)
            if "CustomIcons." in content and "import com.example.ui.theme.CustomIcons" not in content:
                # Find the package line
                pkg_match = re.search(r'^package .+', content, re.MULTILINE)
                if pkg_match:
                    pkg_line = pkg_match.group(0)
                    content = content.replace(pkg_line, pkg_line + "\n\nimport com.example.ui.theme.CustomIcons")

            if content != original:
                with open(filepath, "w") as f:
                    f.write(content)
                print(f"Replaced icons in {filepath}")

