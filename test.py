import re

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    content = f.read()

# Make sure we got it right
print(content[content.find("Scaffold("):content.find("NavHost")])
