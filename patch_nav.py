import re

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

pattern = r'androidx\.compose\.material3\.Surface\(\s*modifier = Modifier\s*\.padding\(horizontal = 24\.dp, vertical = 16\.dp\).*?contentColor = MaterialTheme\.colorScheme\.onSurface\s*\)\s*\{'

replacement = """com.example.ui.components.glass.GlassSurface(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        alpha = 0.3f
                    ) {"""

content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)
