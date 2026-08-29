import re

with open('app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('onNavigateToProviderSelection: (isAi1: Boolean) -> Unit', 'onNavigateToProviderSelection: (aiIndex: Int) -> Unit')
content = content.replace('onNavigateToProviderSelection(true)', 'onNavigateToProviderSelection(1)')
content = content.replace('onNavigateToProviderSelection(false)', 'onNavigateToProviderSelection(2)')

with open('app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    main_content = f.read()

main_content = main_content.replace('onNavigateToProviderSelection = { navController.navigate("provider_selection/${it}") },', 'onNavigateToProviderSelection = { aiIndex -> navController.navigate("provider_selection/${aiIndex}") },')

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(main_content)

