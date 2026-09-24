package com.example.ui.components.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
    singleLine: Boolean = false,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    readOnly: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    shape: Shape = RoundedCornerShape(16.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val isDark = AppTheme.colors.isDark
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bgAlpha = if (isDark) 0.35f else 0.50f
    val bgGradient = Brush.linearGradient(
        colors = listOf(
            if (isDark) Color(0xFF161924).copy(alpha = bgAlpha) else Color.White.copy(alpha = bgAlpha),
            if (isDark) Color(0xFF10121A).copy(alpha = bgAlpha * 0.7f) else Color.White.copy(alpha = bgAlpha * 0.85f)
        )
    )

    val focusedBorderColor1 by animateColorAsState(
        targetValue = if (isFocused) Color(0xFF6C63FF) else Color.White.copy(alpha = if (isDark) 0.35f else 0.70f),
        animationSpec = tween(250),
        label = "border1"
    )
    val focusedBorderColor2 by animateColorAsState(
        targetValue = if (isFocused) Color(0xFF00E5FF) else Color.White.copy(alpha = if (isDark) 0.08f else 0.20f),
        animationSpec = tween(250),
        label = "border2"
    )

    val borderGradient = Brush.linearGradient(
        colors = listOf(focusedBorderColor1, focusedBorderColor2),
        start = Offset(0f, 0f),
        end = Offset(1000f, 1000f)
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        minLines = minLines,
        maxLines = maxLines,
        singleLine = singleLine,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        readOnly = readOnly,
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgGradient)
            .border(if (isFocused) 1.5.dp else 1.dp, borderGradient, shape),
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
            cursorColor = Color(0xFF00E5FF),
            focusedLabelColor = Color(0xFF00E5FF),
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        textStyle = TextStyle(fontFamily = MaterialTheme.typography.bodyLarge.fontFamily)
    )
}
