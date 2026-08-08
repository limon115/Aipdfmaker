import sys

with open('/app/applet/app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

# Remove the incorrectly placed imports
content = content.replace("import androidx.compose.foundation.layout.Column\n", "")
content = content.replace("import androidx.compose.ui.unit.dp\n", "")

# Insert them after the package declaration
package_decl = "package com.example.ui.screens"
if package_decl in content:
    content = content.replace(package_decl, package_decl + "\n\nimport androidx.compose.foundation.layout.Column\nimport androidx.compose.ui.unit.dp")

with open('/app/applet/app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)

print("Fixed imports")
