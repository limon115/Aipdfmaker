import os
import re

UI_DIR = "app/src/main/java/com/example/ui/screens"

def patch_file(filepath):
    with open(filepath, "r") as f:
        content = f.read()

    original = content
    
    # Imports
    if "import com.example.ui.theme.GlassCard" not in content and "Card(" in content:
        content = content.replace("import androidx.compose.runtime.Composable", "import androidx.compose.runtime.Composable\nimport com.example.ui.theme.GlassCard")
        if "import androidx.compose.foundation.shape.RoundedCornerShape" not in content:
            content = content.replace("import androidx.compose.runtime.Composable", "import androidx.compose.runtime.Composable\nimport androidx.compose.foundation.shape.RoundedCornerShape")

    # Replace Cards with GlassCard
    # We find Card( modifier = ..., shape = ..., colors = ..., elevation = ... )
    # This regex handles `Card(` replacement
    content = re.sub(r'\bCard\(', 'GlassCard(', content)
    
    # Standard CardDefaults.cardColors(...)
    content = re.sub(r'colors\s*=\s*CardDefaults\.cardColors\([^)]*\),?', '', content)
    content = re.sub(r'elevation\s*=\s*CardDefaults\.cardElevation\([^)]*\),?', '', content)
    
    # Replace default standard RoundedCornerShape to 24.dp
    content = re.sub(r'RoundedCornerShape\(\d+\.dp\)', 'RoundedCornerShape(24.dp)', content)

    if original != content:
        with open(filepath, "w") as f:
            f.write(content)
        print(f"Patched Cards in {filepath}")

for root, _, files in os.walk(UI_DIR):
    for file in files:
        if file.endswith(".kt"):
            patch_file(os.path.join(root, file))

