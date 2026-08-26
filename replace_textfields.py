import os
import re

UI_DIR = "app/src/main/java/com/example/ui"

for root, _, files in os.walk(UI_DIR):
    for file in files:
        if file.endswith(".kt") and file != "GlassComponents.kt":
            filepath = os.path.join(root, file)
            with open(filepath, "r") as f:
                content = f.read()
            original = content
            
            # Replace OutlinedTextField with GlassTextField
            content = re.sub(r'\bOutlinedTextField\(', 'GlassTextField(', content)
            
            if "GlassTextField(" in content and "import com.example.ui.theme.GlassTextField" not in content:
                # Add import
                pkg_match = re.search(r'^package .+', content, re.MULTILINE)
                if pkg_match:
                    pkg_line = pkg_match.group(0)
                    content = content.replace(pkg_line, pkg_line + "\n\nimport com.example.ui.theme.GlassTextField")

            if content != original:
                with open(filepath, "w") as f:
                    f.write(content)
                print(f"Replaced textfields in {filepath}")

