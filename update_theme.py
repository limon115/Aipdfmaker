import re

with open("app/src/main/java/com/example/ui/theme/Theme.kt", "r") as f:
    content = f.read()

# Replace MyApplicationTheme with ThemeProvider that uses MutableState
target = """@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) darkAppColors else lightAppColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = appColors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalThemeIsDark provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}

val LocalThemeIsDark = staticCompositionLocalOf { false }"""

replacement = """import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.MutableState

val LocalThemeIsDark = staticCompositionLocalOf<MutableState<Boolean>> { 
    error("No Theme state provided") 
}

@Composable
fun ThemeProvider(
    initialDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val isDarkMode = remember(initialDarkTheme) { mutableStateOf(initialDarkTheme) }
    val isDark = isDarkMode.value

    // Expose custom ColorScheme based on the required palettes (with backgrounds)
    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = PrimaryDark,
            background = BackgroundDark, // #081524
            surface = SurfaceDark,
            onPrimary = Color.White,
            onBackground = TextPrimaryDark,
            onSurface = TextPrimaryDark,
            onSurfaceVariant = TextSecondaryDark,
            outline = BorderDark,
            outlineVariant = DividerDark,
            error = ErrorColor
        )
    } else {
        lightColorScheme(
            primary = PrimaryLight,
            background = BackgroundLight, // #00D0FF
            surface = SurfaceLight,
            onPrimary = Color.White,
            onBackground = TextPrimaryLight,
            onSurface = TextPrimaryLight,
            onSurfaceVariant = TextSecondaryLight,
            outline = BorderLight,
            outlineVariant = DividerLight,
            error = ErrorColor
        )
    }
    
    val appColors = if (isDark) darkAppColors else lightAppColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = appColors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalThemeIsDark provides isDarkMode
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}"""

content = content.replace(target, replacement)

# Remove the old LightColorScheme and DarkColorScheme constants
content = re.sub(r'private val LightColorScheme = lightColorScheme\(.*?error = ErrorColor\n\)', '', content, flags=re.DOTALL)
content = re.sub(r'private val DarkColorScheme = darkColorScheme\(.*?error = ErrorColor\n\)', '', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/theme/Theme.kt", "w") as f:
    f.write(content)
