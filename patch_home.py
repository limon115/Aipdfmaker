import re

with open('app/src/main/java/com/example/ui/screens/home/HomeScreen.kt', 'r') as f:
    content = f.read()

pattern = r'contentPadding = PaddingValues\(16\.dp\)'
replacement = 'contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp)'
content = re.sub(pattern, replacement, content)

with open('app/src/main/java/com/example/ui/screens/home/HomeScreen.kt', 'w') as f:
    f.write(content)
