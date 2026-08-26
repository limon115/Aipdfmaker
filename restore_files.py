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
            
            content = content.replace('CustomIcons.Pencil', 'Icons.Default.Edit')
            content = content.replace('CustomIcons.Folder', 'Icons.Default.Folder')
            content = content.replace('CustomIcons.SettingsGear', 'Icons.Default.Settings')
            content = content.replace('CustomIcons.Sparkles', 'Icons.Default.AutoAwesome')
            
            # Remove imports
            content = re.sub(r'import com\.example\.ui\.theme\.CustomIcons\n?', '', content)
            
            # Restore TextFields
            content = content.replace('GlassTextField(', 'OutlinedTextField(')
            content = re.sub(r'import com\.example\.ui\.theme\.GlassTextField\n?', '', content)
            
            if content != original:
                with open(filepath, "w") as f:
                    f.write(content)
                print(f"Restored in {filepath}")

