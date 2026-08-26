import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("import com.example.ui.theme.MyApplicationTheme", "import com.example.ui.theme.ThemeProvider")
content = content.replace("MyApplicationTheme(darkTheme = isDarkTheme) {", "ThemeProvider(initialDarkTheme = isDarkTheme) {")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
