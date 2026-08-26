with open("app/src/main/java/com/example/ui/theme/GlassTheme.kt", "r") as f:
    content = f.read()

target = """@Composable
fun GlassBackground(
    modifier: Modifier = Modifier, 
    darkTheme: Boolean = LocalThemeIsDark.current,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (darkTheme) BackgroundDark else BackgroundLight),
        content = content
    )
}"""

replacement = """import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier, 
    darkTheme: Boolean = LocalThemeIsDark.current,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (darkTheme) BackgroundDark else BackgroundLight)
    ) {
        // Apply a subtle background blur effect
        Canvas(modifier = Modifier.fillMaxSize().blur(40.dp)) {
            val primaryColor = if (darkTheme) PrimaryDark else PrimaryLight
            drawCircle(
                color = primaryColor.copy(alpha = if (darkTheme) 0.15f else 0.3f),
                radius = size.width / 1.5f,
                center = Offset(size.width * 0.8f, 0f)
            )
            drawCircle(
                color = primaryColor.copy(alpha = if (darkTheme) 0.1f else 0.2f),
                radius = size.width / 1.2f,
                center = Offset(size.width * 0.2f, size.height)
            )
        }
        
        // Semi-transparent surface overlay to ensure it remains performant and readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (darkTheme) Color(0xFF081524).copy(alpha = 0.4f) else Color(0xFF00D0FF).copy(alpha = 0.2f)
                )
        )
        
        content()
    }
}"""

content = content.replace(target, replacement)
if "import androidx.compose.foundation.Canvas" not in content:
    content = content.replace("import androidx.compose.foundation.background", "import androidx.compose.foundation.background\nimport androidx.compose.foundation.Canvas\nimport androidx.compose.ui.draw.blur\nimport androidx.compose.ui.geometry.Offset")

with open("app/src/main/java/com/example/ui/theme/GlassTheme.kt", "w") as f:
    f.write(content)
