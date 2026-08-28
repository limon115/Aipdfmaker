package com.example

import androidx.compose.ui.text.font.FontFamily
import java.io.File
import androidx.compose.ui.text.font.Typeface
import android.graphics.Typeface as AndroidTypeface

fun getFont(file: File): FontFamily {
    val androidTypeface = AndroidTypeface.createFromFile(file)
    val composeTypeface = Typeface(androidTypeface)
    return FontFamily(composeTypeface)
}
