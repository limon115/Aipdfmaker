import os

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Fix the broken string replacement that caused `@Composable\nfun GlassBackground {`
content = content.replace("@Composable\nfun GlassBackground {\n        DocMorphApp()\n      } {", "@Composable\nfun DocMorphApp() {")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
print("Fixed MainActivity")
