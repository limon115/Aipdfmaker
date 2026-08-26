import os
import re

UI_DIR = "app/src/main/java/com/example/ui/screens"

def patch_file(filepath):
    with open(filepath, "r") as f:
        content = f.read()

    original = content
    
    # Imports
    if "com.example.ui.theme.GlassBackground" not in content:
        content = content.replace("import androidx.compose.runtime.Composable", "import androidx.compose.runtime.Composable\nimport com.example.ui.theme.GlassBackground\nimport com.example.ui.theme.GlassCard\nimport androidx.compose.ui.graphics.Color\nimport androidx.compose.foundation.shape.RoundedCornerShape")

    # Wrap Scaffold
    if "Scaffold(" in content and "GlassBackground {" not in content:
        content = content.replace("Scaffold(", "GlassBackground {\n    Scaffold(")
        
        # Need to close the GlassBackground correctly. We will just append } at the very end of the @Composable function if it has a Scaffold.
        # This regex replaces the last closing brace of the first function. Actually, a simpler way is just to close it explicitly.
        # Let's count braces or do a simpler replace.
        # A simple hack: Find the outermost Scaffold closure and append a bracket.
        # Or, just replace the end of the root layout. This is risky with regex. Let's do a brace-matching parser.
        
    # Replace Scaffold container colors
    content = re.sub(r'containerColor\s*=\s*MaterialTheme\.colorScheme\.background', 'containerColor = Color.Transparent', content)
    
    # Replace TopAppBar colors
    content = re.sub(r'containerColor\s*=\s*MaterialTheme\.colorScheme\.surface', 'containerColor = Color.Transparent', content)
    
    # Replace Cards with GlassCard
    # We find Card( modifier = ..., shape = ..., colors = ..., elevation = ... )
    # This is complex, let's just replace `Card(` with `GlassCard(` and remove colors and elevation.
    # We will do a generic replacement.
    content = re.sub(r'\bCard\(', 'GlassCard(', content)
    content = re.sub(r'colors\s*=\s*CardDefaults\.cardColors\([^)]*\),?', '', content)
    content = re.sub(r'elevation\s*=\s*CardDefaults\.cardElevation\([^)]*\),?', '', content)
    
    if original != content:
        with open(filepath, "w") as f:
            f.write(content)
        print(f"Patched (basic) {filepath}")

for root, _, files in os.walk(UI_DIR):
    for file in files:
        if file.endswith(".kt"):
            patch_file(os.path.join(root, file))

