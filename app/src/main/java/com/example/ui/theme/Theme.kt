package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
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
            shapes = com.example.ui.theme.Shapes,
            content = content
        )
    }
}
