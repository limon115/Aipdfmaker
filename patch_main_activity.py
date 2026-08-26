import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Make sure GlassBackground is imported
if "com.example.ui.theme.GlassBackground" not in content:
    content = content.replace("import com.example.ui.theme.MyApplicationTheme", "import com.example.ui.theme.MyApplicationTheme\nimport com.example.ui.theme.GlassBackground")

# Wrap DocMorphApp with GlassBackground
content = content.replace("DocMorphApp()", "GlassBackground {\n        DocMorphApp()\n      }")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
