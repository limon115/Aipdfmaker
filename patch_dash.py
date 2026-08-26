import re

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

pattern = r'composable\(BottomNavItem\.Dashboard\.route\) \{\s*Column\(modifier = androidx\.compose\.ui\.Modifier\.fillMaxSize\(\)\.padding\(16\.dp\)\.padding\(top = 48\.dp\)\) \{\s*com\.example\.ui\.screens\.settings\.AiUsageDashboardCard\(\)\s*\}\s*\}'

replacement = """composable(BottomNavItem.Dashboard.route) {
                Column(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 100.dp)
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                ) {
                    com.example.ui.screens.settings.AiUsageDashboardCard()
                }
            }"""

content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)
