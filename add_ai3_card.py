import re

with open('app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'r') as f:
    content = f.read()

ai3_card = """
            AiConfigCard(
                title = "AI #3 - LaTeX Debugger",
                provider = settings.ai3Provider,
                model = settings.ai3Model,
                apiKey = settings.ai3ApiKey,
                onProviderClick = { onNavigateToProviderSelection(3) },
                onModelChange = viewModel::updateAi3Model,
                onApiKeyChange = viewModel::updateAi3ApiKey,
                onTestConnection = { provider, model, apiKey ->
                    viewModel.testConnection(
                        provider = provider,
                        model = model,
                        apiKey = apiKey,
                        onSuccess = { android.widget.Toast.makeText(context, "Connection Successful", android.widget.Toast.LENGTH_SHORT).show() },
                        onError = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(it))
                            android.widget.Toast.makeText(context, "Connection Failed: copied full error to clipboard", android.widget.Toast.LENGTH_LONG).show()
                        }
                    )
                }
            )
"""

content = re.sub(r'(AiConfigCard\(\s*title = "AI #2 - Note Generator"[\s\S]*?advancedSettings = \{[\s\S]*?\}\s*\)\s*\})', r'\1' + '\n' + ai3_card, content)

with open('app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'w') as f:
    f.write(content)
