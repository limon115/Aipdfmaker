import re

with open("app/src/main/java/com/example/ui/theme/Theme.kt", "r") as f:
    content = f.read()

imports_to_move = """import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.MutableState
"""

content = content.replace(imports_to_move, "")
content = imports_to_move + content

with open("app/src/main/java/com/example/ui/theme/Theme.kt", "w") as f:
    f.write(content)
