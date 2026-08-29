import re

with open('app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('                    )\n                }\n\n            AiConfigCard(', '                    )\n                }\n            )\n\n            AiConfigCard(')

with open('app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'w') as f:
    f.write(content)
