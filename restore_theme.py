import os

with open("app/src/main/java/com/example/ui/theme/Type.kt", "w") as f:
    f.write("""package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography =  Typography(
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
  )
""")

with open("app/src/main/java/com/example/ui/theme/Color.kt", "w") as f:
    f.write("""package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val PurpleAccent = Color(0xFF38BDF8) // Mapped to sky blue
val CrispWhite = Color(0xFFFFFFFF)
val LightGrayBackground = Color(0xFFF0F9FF)
val DarkText = Color(0xFF0F172A)
val SubtleGray = Color(0x4DFFFFFF)
val GrayText = Color(0xFF64748B)
""")

with open("app/src/main/java/com/example/ui/theme/Theme.kt", "w") as f:
    f.write("""package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SkyBlueAccent = Color(0xFF38BDF8)
val SoftLavender = Color(0xFFE0E7FF)
val DeepNavyText = Color(0xFF0F172A)
val MutedBlueGray = Color(0xFF64748B)
val GlassWhite = Color(0xFFB9E4FD).copy(alpha = 0.35f)
val GlassBorder = Color(0x4DFFFFFF)

private val LightColorScheme = lightColorScheme(
    primary = SkyBlueAccent,
    onPrimary = Color.White,
    secondary = SoftLavender,
    onSecondary = DeepNavyText,
    // Scaffold backgrounds transparent so GlassBackground shows through
    background = Color.Transparent, 
    onBackground = DeepNavyText,
    // Surface used by Cards, make it translucent white
    surface = GlassWhite, 
    onSurface = DeepNavyText,
    surfaceVariant = Color(0xFFB9E4FD).copy(alpha = 0.45f), // 60% white for slightly stronger surface variants
    onSurfaceVariant = MutedBlueGray,
    outline = GlassBorder
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
""")

with open("app/src/main/java/com/example/ui/theme/GlassTheme.kt", "w") as f:
    f.write("""package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE0F2FE), // Light sky blue
                        Color(0xFFF0F9FF), // Icy blue
                        Color(0xFFF8FAFC)  // Soft white
                    )
                )
            ),
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = SkyBlueAccent,
                spotColor = SkyBlueAccent.copy(alpha = 0.5f)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.4f),
                shape = shape
            )
            .clip(shape),
        color = Color(0xFFB9E4FD).copy(alpha = 0.35f),
        contentColor = DeepNavyText,
        content = content
    )
}
""")
