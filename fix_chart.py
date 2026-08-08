import sys

with open('/app/applet/app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(".androidx.compose.foundation.background", ".background")
if "import androidx.compose.foundation.background" not in content:
    content = content.replace("package com.example.ui.screens.settings", "package com.example.ui.screens.settings\n\nimport androidx.compose.foundation.background")

with open('/app/applet/app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'w') as f:
    f.write(content)

print("Fixed")
