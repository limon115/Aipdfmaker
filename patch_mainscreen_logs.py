with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    text = f.read()

target = """            composable(BottomNavItem.Settings.route) {
                AiSettingsScreen(
                    onNavigateToProviderSelection = { isAi1 ->
                        navController.navigate("provider_selection/$isAi1")
                    },
                    onNavigateToApiLab = {
                        navController.navigate("api_lab")
                    }
                )
            }"""
replacement = """            composable(BottomNavItem.Settings.route) {
                AiSettingsScreen(
                    onNavigateToProviderSelection = { isAi1 ->
                        navController.navigate("provider_selection/$isAi1")
                    },
                    onNavigateToApiLab = {
                        navController.navigate("api_lab")
                    },
                    onNavigateToLogs = {
                        navController.navigate("app_logs")
                    }
                )
            }
            
            composable("app_logs") {
                com.example.ui.screens.settings.AppLogsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }"""

if target in text:
    text = text.replace(target, replacement)
else:
    print("target not found")

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(text)

