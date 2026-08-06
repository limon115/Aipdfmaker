package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PurpleAccent,
    onPrimary = CrispWhite,
    secondary = PurpleAccent,
    onSecondary = CrispWhite,
    background = LightGrayBackground,
    onBackground = DarkText,
    surface = CrispWhite,
    onSurface = DarkText,
    surfaceVariant = SubtleGray,
    onSurfaceVariant = GrayText
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
