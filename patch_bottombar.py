import re

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

pattern = r'com\.example\.ui\.components\.glass\.GlassSurface\(\s*modifier = Modifier\.padding\(horizontal = 24\.dp, vertical = 16\.dp\),\s*shape = androidx\.compose\.foundation\.shape\.RoundedCornerShape\(24\.dp\),\s*alpha = 0\.3f\s*\)\s*\{\s*NavigationBar\(\s*containerColor = androidx\.compose\.ui\.graphics\.Color\.Transparent,\s*contentColor = MaterialTheme\.colorScheme\.onSurface,\s*tonalElevation = 0\.dp,\s*windowInsets = androidx\.compose\.foundation\.layout\.WindowInsets\(0, 0, 0, 0\)\s*\)'

replacement = """com.example.ui.components.glass.GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        ),
                        alpha = 0.3f
                    ) {
                        NavigationBar(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 0.dp,
                            windowInsets = androidx.compose.foundation.layout.WindowInsets.navigationBars
                        )"""

content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)
