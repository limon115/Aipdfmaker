import os

filepath = "app/src/main/java/com/example/ui/screens/processing/ProcessingScreen.kt"
with open(filepath, "r") as f:
    text = f.read()

import_statement = "import androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.filled.Check\n"
if "import androidx.compose.material.icons.Icons" not in text:
    text = text.replace('import androidx.compose.material3.*', import_statement + 'import androidx.compose.material3.*')

with open(filepath, "w") as f:
    f.write(text)
