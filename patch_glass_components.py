import re

with open("app/src/main/java/com/example/ui/theme/GlassComponents.kt", "r") as f:
    content = f.read()

# Make sure fillMaxWidth is on OutlinedTextField modifier.
target = """        modifier = modifier
            .clip(shape)
            .border(1.dp, borderColor, shape),"""

replacement = """        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, borderColor, shape),"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/theme/GlassComponents.kt", "w") as f:
    f.write(content)
