import re

with open('app/src/main/java/com/example/ui/screens/debugger/LatexDebuggerScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('placeholder = "Paste LaTeX code here..."', 'placeholder = { Text("Paste LaTeX code here...") }')
content = content.replace('placeholder = "Paste compiler log here..."', 'placeholder = { Text("Paste compiler log here...") }')

with open('app/src/main/java/com/example/ui/screens/debugger/LatexDebuggerScreen.kt', 'w') as f:
    f.write(content)
