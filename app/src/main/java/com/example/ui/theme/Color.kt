package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme
val PrimaryLight = Color(0xFF2563EB) // Darker blue for better contrast on light mode
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF).copy(alpha = 0.6f)
val SurfaceElevatedLight = Color(0xFFFFFFFF).copy(alpha = 0.8f)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val BorderLight = Color(0xFF000000).copy(alpha = 0.08f)
val DividerLight = Color(0xFF000000).copy(alpha = 0.05f)

// Dark Theme
val PrimaryDark = Color(0xFF60A5FA)
val BackgroundDark = Color(0xFF081524)
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
