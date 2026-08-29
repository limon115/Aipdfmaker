import re

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('onNavigateToProviderSelection = { isAi1 ->', 'onNavigateToProviderSelection = { aiIndex ->')
content = content.replace('navController.navigate("provider_selection/$isAi1")', 'navController.navigate("provider_selection/$aiIndex")')

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)
