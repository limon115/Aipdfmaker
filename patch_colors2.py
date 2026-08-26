import re

with open("app/src/main/java/com/example/ui/theme/Color.kt", "r") as f:
    content = f.read()

content = re.sub(r'val BackgroundLight = Color\(0xFF0A0E14\)', 'val BackgroundLight = Color(0xFF00D0FF)', content)
content = re.sub(r'val BackgroundDark = Color\(0xFF0A0E14\)', 'val BackgroundDark = Color(0xFF081524)', content)

with open("app/src/main/java/com/example/ui/theme/Color.kt", "w") as f:
    f.write(content)
