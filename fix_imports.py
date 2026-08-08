import sys

def add_import(filepath, import_statement):
    with open(filepath, 'r') as f:
        content = f.read()
    if import_statement not in content:
        content = content.replace("package com.example.ui.screens.settings", f"package com.example.ui.screens.settings\n\n{import_statement}")
        with open(filepath, 'w') as f:
            f.write(content)

add_import('app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'import androidx.compose.material.icons.filled.ArrowForward\nimport androidx.compose.material3.MenuAnchorType')
add_import('app/src/main/java/com/example/ui/screens/settings/ApiLabScreen.kt', 'import androidx.compose.material3.MenuAnchorType')
