import re

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

# Change provider_selection/{isAi1} to provider_selection/{aiIndex}
content = content.replace('composable("provider_selection/{isAi1}")', 'composable("provider_selection/{aiIndex}")')
content = content.replace('val isAi1 = backStackEntry.arguments?.getString("isAi1")?.toBoolean() ?: true', 'val aiIndex = backStackEntry.arguments?.getString("aiIndex")?.toIntOrNull() ?: 1')

current_provider = """currentProvider = when (aiIndex) {
                        1 -> settings?.ai1Provider ?: com.example.domain.models.AiProvider.GOOGLE_GEMINI
                        2 -> settings?.ai2Provider ?: com.example.domain.models.AiProvider.GOOGLE_GEMINI
                        else -> settings?.ai3Provider ?: com.example.domain.models.AiProvider.GOOGLE_GEMINI
                    }"""
content = re.sub(r'currentProvider = if \(isAi1\).*?GOOGLE_GEMINI,', current_provider + ',', content)

on_provider_selected = """onProviderSelected = { provider ->
                        when (aiIndex) {
                            1 -> settingsViewModel.updateAi1Provider(provider)
                            2 -> settingsViewModel.updateAi2Provider(provider)
                            else -> settingsViewModel.updateAi3Provider(provider)
                        }
                        navController.popBackStack()
                    }"""
content = re.sub(r'onProviderSelected = \{ provider ->.*?navController\.popBackStack\(\)\n                    \}', on_provider_selected, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)

