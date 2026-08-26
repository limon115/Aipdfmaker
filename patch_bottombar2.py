import re

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

# Add missing imports if not present
if 'import androidx.compose.foundation.layout.fillMaxWidth' not in content:
    content = content.replace('import androidx.compose.foundation.layout.Column', 'import androidx.compose.foundation.layout.Column\nimport androidx.compose.foundation.layout.fillMaxWidth')

if 'import androidx.compose.foundation.layout.navigationBars' not in content:
    content = content.replace('import androidx.compose.foundation.layout.Column', 'import androidx.compose.foundation.layout.Column\nimport androidx.compose.foundation.layout.navigationBars')

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)
