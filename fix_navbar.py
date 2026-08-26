import re

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    content = f.read()

target = """                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .shadow(
                            elevation = 16.dp, 
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {"""

replacement = """                val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                androidx.compose.material3.Surface(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .shadow(
                            elevation = 8.dp, 
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                            spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.05f),
                            ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.05f)
                        )
                        .androidx.compose.foundation.border(
                            width = 1.dp,
                            color = if (isDark) com.example.ui.theme.BorderDark else com.example.ui.theme.BorderLight,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    color = if (isDark) com.example.ui.theme.SurfaceElevatedDark else com.example.ui.theme.SurfaceElevatedLight,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(content)
print("Navbar updated in MainScreen.kt")
