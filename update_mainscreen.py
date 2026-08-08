import sys

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

# Add ApiLabScreen import if missing
if "import com.example.ui.screens.settings.ApiLabScreen" not in content:
    content = content.replace("import com.example.ui.screens.settings.AiSettingsScreen", "import com.example.ui.screens.settings.AiSettingsScreen\nimport com.example.ui.screens.settings.ApiLabScreen")

# Update AiSettingsScreen invocation
old_settings = """            composable(BottomNavItem.Settings.route) {
                AiSettingsScreen(
                    onNavigateToProviderSelection = { isAi1 ->
                        navController.navigate("provider_selection/$isAi1")
                    }
                )
            }"""

new_settings = """            composable(BottomNavItem.Settings.route) {
                AiSettingsScreen(
                    onNavigateToProviderSelection = { isAi1 ->
                        navController.navigate("provider_selection/$isAi1")
                    },
                    onNavigateToApiLab = {
                        navController.navigate("api_lab")
                    }
                )
            }
            
            composable("api_lab") {
                ApiLabScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }"""

if "composable(\"api_lab\")" not in content:
    content = content.replace(old_settings, new_settings)

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)

print("Updated MainScreen")
