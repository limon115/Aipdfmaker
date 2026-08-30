import re
with open('app/src/main/java/com/example/ui/screens/debugger/LatexDebuggerViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'lastUpdated = System.currentTimeMillis(),\n                    sourceText = ""',
    'lastUpdated = System.currentTimeMillis(),\n                    sourceText = _state.value.latexCode'
)

with open('app/src/main/java/com/example/ui/screens/debugger/LatexDebuggerViewModel.kt', 'w') as f:
    f.write(content)
