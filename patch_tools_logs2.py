with open("app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt", "r") as f:
    text = f.read()

target = """fun AiSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateToProviderSelection: (isAi1: Boolean) -> Unit,
    onNavigateToApiLab: () -> Unit
) {"""
replacement = """fun AiSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateToProviderSelection: (isAi1: Boolean) -> Unit,
    onNavigateToApiLab: () -> Unit,
    onNavigateToLogs: () -> Unit = {}
) {"""

if target in text:
    text = text.replace(target, replacement)
else:
    print("target not found")

with open("app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt", "w") as f:
    f.write(text)

