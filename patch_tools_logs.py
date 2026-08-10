with open("app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt", "r") as f:
    text = f.read()

target = """fun DeveloperToolsCard(
    onNavigateToApiLab: () -> Unit
) {"""
replacement = """fun DeveloperToolsCard(
    onNavigateToApiLab: () -> Unit,
    onNavigateToLogs: () -> Unit = {}
) {"""

if target in text:
    text = text.replace(target, replacement)
else:
    print("target not found")

target2 = """                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToApiLab() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }"""
replacement2 = """                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToApiLab() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            
            ListItem(
                headlineContent = { Text("App Logs") },
                supportingContent = { Text("View internal application logs for debugging") },
                leadingContent = {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToLogs() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }"""

if target2 in text:
    text = text.replace(target2, replacement2)
else:
    print("target2 not found")

target3 = """fun AiSettingsScreen(
    onNavigateToProviderSelection: (isAi1: Boolean) -> Unit,
    onNavigateToApiLab: () -> Unit,
    viewModel: AiSettingsViewModel = viewModel(factory = ViewModelFactory.getInstance())
) {"""
replacement3 = """fun AiSettingsScreen(
    onNavigateToProviderSelection: (isAi1: Boolean) -> Unit,
    onNavigateToApiLab: () -> Unit,
    onNavigateToLogs: () -> Unit = {},
    viewModel: AiSettingsViewModel = viewModel(factory = ViewModelFactory.getInstance())
) {"""

if target3 in text:
    text = text.replace(target3, replacement3)
else:
    print("target3 not found")

target4 = """            DeveloperToolsCard(
                onNavigateToApiLab = onNavigateToApiLab
            )"""
replacement4 = """            DeveloperToolsCard(
                onNavigateToApiLab = onNavigateToApiLab,
                onNavigateToLogs = onNavigateToLogs
            )"""

if target4 in text:
    text = text.replace(target4, replacement4)
else:
    print("target4 not found")

with open("app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt", "w") as f:
    f.write(text)

