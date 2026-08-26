import re

with open("app/src/main/java/com/example/ui/theme/Theme.kt", "w") as f:
    f.write("""package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class AppThemeColors(
    val primary: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val divider: Color,
    val isDark: Boolean
)

val lightAppColors = AppThemeColors(
    primary = PrimaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceElevated = SurfaceElevatedLight,
    textPrimary = TextPrimaryLight,
    textSecondary = TextSecondaryLight,
    border = BorderLight,
    divider = DividerLight,
    isDark = false
)

val darkAppColors = AppThemeColors(
    primary = PrimaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceElevated = SurfaceElevatedDark,
    textPrimary = TextPrimaryDark,
    textSecondary = TextSecondaryDark,
    border = BorderDark,
    divider = DividerDark,
    isDark = true
)

val LocalAppColors = staticCompositionLocalOf<AppThemeColors> {
    error("No AppThemeColors provided")
}

object AppTheme {
    val colors: AppThemeColors
        @Composable
        get() = LocalAppColors.current
}

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    background = Color.Transparent, 
    surface = SurfaceLight,
    onPrimary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    outlineVariant = DividerLight,
    error = ErrorColor
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    background = Color.Transparent,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    outlineVariant = DividerDark,
    error = ErrorColor
)

@Composable
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

val LocalThemeIsDark = staticCompositionLocalOf { false }
""")

with open("app/src/main/java/com/example/ui/theme/GlassTheme.kt", "w") as f:
    f.write("""package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier, 
    content: @Composable BoxScope.() -> Unit
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Apply a subtle background blur effect
        Canvas(modifier = Modifier.fillMaxSize().blur(40.dp)) {
            val primaryColor = colors.primary
            drawCircle(
                color = primaryColor.copy(alpha = if (colors.isDark) 0.15f else 0.3f),
                radius = size.width / 1.5f,
                center = Offset(size.width * 0.8f, 0f)
            )
            drawCircle(
                color = primaryColor.copy(alpha = if (colors.isDark) 0.1f else 0.2f),
                radius = size.width / 1.2f,
                center = Offset(size.width * 0.2f, size.height)
            )
        }
        
        // Semi-transparent surface overlay to ensure it remains performant and readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (colors.isDark) Color(0xFF081524).copy(alpha = 0.4f) else Color(0xFF00D0FF).copy(alpha = 0.2f)
                )
        )
        
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    elevated: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    val bgColor = if (elevated) colors.surfaceElevated else colors.surface
    val borderColor = colors.border

    Surface(
        modifier = modifier
            .shadow(
                elevation = if (elevated) 8.dp else 0.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.02f)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            )
            .clip(shape),
        color = bgColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = content
    )
}
""")
