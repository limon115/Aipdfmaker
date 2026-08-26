import re

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

# Replace Modifier.padding(innerPadding) with Modifier.fillMaxSize() in NavHost
pattern = r'NavHost\(\s*navController = navController,\s*startDestination = BottomNavItem\.Home\.route,\s*modifier = Modifier\.padding\(innerPadding\)\s*\)'
replacement = """NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.fillMaxSize()
        )"""

content = re.sub(pattern, replacement, content)

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)
