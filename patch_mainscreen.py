import sys

with open('/app/applet/app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '                ProviderSelectionScreen(\n                    currentProvider = if (isAi1) settings.ai1Provider else settings.ai2Provider,',
    '                ProviderSelectionScreen(\n                    currentProvider = if (isAi1) settings?.ai1Provider ?: com.example.domain.models.AiProvider.GOOGLE_GEMINI else settings?.ai2Provider ?: com.example.domain.models.AiProvider.GOOGLE_GEMINI,'
)

with open('/app/applet/app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)
