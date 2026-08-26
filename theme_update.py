import os
import re

def write_file(path, content):
    with open(path, "w") as f:
        f.write(content.strip() + "\n")

# 1. Color.kt
write_file("app/src/main/java/com/example/ui/theme/Color.kt", """
package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme
val PrimaryLight = Color(0xFF2563EB)
val BackgroundLight = Color(0xFFF2F4F7)
val SurfaceLight = Color(0xFFFFFFFF).copy(alpha = 0.7f)
val SurfaceElevatedLight = Color(0xFFFFFFFF).copy(alpha = 0.85f)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF64748B)
val BorderLight = Color(0xFF0F172A).copy(alpha = 0.08f)
val DividerLight = Color(0xFF0F172A).copy(alpha = 0.06f)

// Dark Theme
val PrimaryDark = Color(0xFF60A5FA)
val BackgroundDark = Color(0xFF0A0E14)
val SurfaceDark = Color(0xFF111827).copy(alpha = 0.6f)
val SurfaceElevatedDark = Color(0xFF111827).copy(alpha = 0.8f)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val BorderDark = Color(0xFFFFFFFF).copy(alpha = 0.08f)
val DividerDark = Color(0xFFFFFFFF).copy(alpha = 0.05f)

// Semantic
val SuccessColor = Color(0xFF22C55E)
val WarningColor = Color(0xFFF59E0B)
val ErrorColor = Color(0xFFEF4444)
""")

# 2. Type.kt
write_file("app/src/main/java/com/example/ui/theme/Type.kt", """
package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 45.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 33.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)
""")

# 3. Theme.kt
write_file("app/src/main/java/com/example/ui/theme/Theme.kt", """
package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = (if (darkTheme) BackgroundDark else BackgroundLight).toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
""")

# 4. GlassTheme.kt
write_file("app/src/main/java/com/example/ui/theme/GlassTheme.kt", """
package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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

@Composable
fun GlassBackground(
    modifier: Modifier = Modifier, 
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (darkTheme) BackgroundDark else BackgroundLight),
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    elevated: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (elevated) {
        if (isDark) SurfaceElevatedDark else SurfaceElevatedLight
    } else {
        if (isDark) SurfaceDark else SurfaceLight
    }
    
    val borderColor = if (isDark) BorderDark else BorderLight

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
print("Themes updated.")
