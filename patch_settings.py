import re

with open('app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'r') as f:
    content = f.read()

pattern = r'DeveloperToolsCard\(\s*onNavigateToApiLab = onNavigateToApiLab,\s*onNavigateToLogs = onNavigateToLogs\s*\)\s*\}'
replacement = """DeveloperToolsCard(
                onNavigateToApiLab = onNavigateToApiLab,
                onNavigateToLogs = onNavigateToLogs
            )
            Spacer(modifier = Modifier.height(100.dp))
        }"""
content = re.sub(pattern, replacement, content)

with open('app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'w') as f:
    f.write(content)
