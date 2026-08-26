import re

with open("app/src/main/java/com/example/ui/theme/Color.kt", "r") as f:
    content = f.read()

content = re.sub(r'val PrimaryLight = .*', 'val PrimaryLight = Color(0xFF60A5FA)', content)
content = re.sub(r'val BackgroundLight = .*', 'val BackgroundLight = Color(0xFF0A0E14)', content)
content = re.sub(r'val SurfaceLight = .*', 'val SurfaceLight = Color(0xFF111827).copy(alpha = 0.6f)', content)
content = re.sub(r'val SurfaceElevatedLight = .*', 'val SurfaceElevatedLight = Color(0xFF111827).copy(alpha = 0.8f)', content)
content = re.sub(r'val TextPrimaryLight = .*', 'val TextPrimaryLight = Color(0xFFF8FAFC)', content)
content = re.sub(r'val TextSecondaryLight = .*', 'val TextSecondaryLight = Color(0xFF94A3B8)', content)
content = re.sub(r'val BorderLight = .*', 'val BorderLight = Color(0xFFFFFFFF).copy(alpha = 0.08f)', content)
content = re.sub(r'val DividerLight = .*', 'val DividerLight = Color(0xFFFFFFFF).copy(alpha = 0.05f)', content)

with open("app/src/main/java/com/example/ui/theme/Color.kt", "w") as f:
    f.write(content)

