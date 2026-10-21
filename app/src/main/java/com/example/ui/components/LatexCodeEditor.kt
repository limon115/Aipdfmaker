package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * High-performance undo/redo manager tracking [TextFieldValue] history.
 */
class UndoRedoManager(private val maxHistory: Int = 100) {
    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()
    private var isPerforming = false

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun push(value: TextFieldValue) {
        if (isPerforming) return
        if (undoStack.isNotEmpty() && undoStack.last().text == value.text) {
            undoStack[undoStack.lastIndex] = value
            return
        }
        undoStack.add(value)
        if (undoStack.size > maxHistory) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun undo(current: TextFieldValue): TextFieldValue? {
        if (undoStack.isEmpty()) return null
        isPerforming = true
        redoStack.add(current)
        val prev = undoStack.removeAt(undoStack.lastIndex)
        isPerforming = false
        return prev
    }

    fun redo(current: TextFieldValue): TextFieldValue? {
        if (redoStack.isEmpty()) return null
        isPerforming = true
        undoStack.add(current)
        val next = redoStack.removeAt(redoStack.lastIndex)
        isPerforming = false
        return next
    }

    fun reset(initial: TextFieldValue) {
        undoStack.clear()
        redoStack.clear()
        undoStack.add(initial)
    }
}

/**
 * Real-time syntax highlighter for LaTeX source code.
 * Preserves the exact underlying text verbatim with [OffsetMapping.Identity]
 * to ensure zero cursor drift and 100% exact text fidelity.
 */
class LatexSyntaxVisualTransformation(
    private val searchQuery: String = "",
    private val currentMatchRange: TextRange? = null
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val highlighted = buildAnnotatedString {
            append(raw)

            fun safeAddStyle(style: SpanStyle, start: Int, end: Int) {
                val s = start.coerceIn(0, raw.length)
                val e = end.coerceIn(0, raw.length)
                if (s < e) {
                    addStyle(style, s, e)
                }
            }

            try {
                // 1. Comments: % ... (slate gray, italic)
                val commentRegex = Regex("%.*$", RegexOption.MULTILINE)
                for (match in commentRegex.findAll(raw)) {
                    safeAddStyle(
                        SpanStyle(color = Color(0xFF64748B), fontStyle = FontStyle.Italic),
                        match.range.first,
                        match.range.last + 1
                    )
                }

                // 2. Control sequences / Commands: \documentclass, \usepackage, etc. (vibrant blue)
                val commandRegex = Regex("""\\[a-zA-Z@]+""")
                for (match in commandRegex.findAll(raw)) {
                    safeAddStyle(
                        SpanStyle(
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.SemiBold
                        ),
                        match.range.first,
                        match.range.last + 1
                    )
                }

                // 3. Numbers & floats: 1.18, 1.5, 0.2, 80 (magenta / violet)
                val numberRegex = Regex("""\b\d+(\.\d+)?\b""")
                for (match in numberRegex.findAll(raw)) {
                    safeAddStyle(
                        SpanStyle(
                            color = Color(0xFFC026D3),
                            fontWeight = FontWeight.Medium
                        ),
                        match.range.first,
                        match.range.last + 1
                    )
                }

                // 4. Key names in brackets / options: Path=, Script=, AutoFakeBold=, AutoFakeSlant=, compat=, title=
                // Avoid lookbehind with variable length which crashes on Android
                val keyRegex = Regex("""[\[,]\s*([a-zA-Z0-9_\-]+)\s*=""")
                for (match in keyRegex.findAll(raw)) {
                    val keyGroup = match.groups[1]
                    if (keyGroup != null) {
                        safeAddStyle(
                            SpanStyle(
                                color = Color(0xFF7C3AED),
                                fontWeight = FontWeight.Medium
                            ),
                            keyGroup.range.first,
                            keyGroup.range.last + 1
                        )
                    }
                }

                // 5. Math mode: $...$ or $$...$$ (teal green delimiters)
                val mathRegex = Regex("""\$\$[\s\S]*?\$\$|\$[^\$\n]*?\$""")
                for (match in mathRegex.findAll(raw)) {
                    val isDouble = match.value.startsWith("$$")
                    val delimLen = if (isDouble) 2 else 1
                    safeAddStyle(
                        SpanStyle(
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Bold
                        ),
                        match.range.first,
                        match.range.first + delimLen
                    )
                    safeAddStyle(
                        SpanStyle(
                            color = Color(0xFF059669),
                            fontWeight = FontWeight.Bold
                        ),
                        match.range.last + 1 - delimLen,
                        match.range.last + 1
                    )
                }

                // 6. Highlight search matches (yellow and orange for current match)
                if (searchQuery.isNotBlank()) {
                    var searchIndex = 0
                    while (searchIndex < raw.length) {
                        val found = raw.indexOf(searchQuery, searchIndex, ignoreCase = true)
                        if (found == -1) break
                        val end = found + searchQuery.length
                        val isCurrentMatch = currentMatchRange != null &&
                                currentMatchRange.start == found && currentMatchRange.end == end
                        safeAddStyle(
                            SpanStyle(
                                background = if (isCurrentMatch) Color(0xFFFDBA74) else Color(0xFFFEF08A),
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold
                            ),
                            found,
                            end
                        )
                        searchIndex = maxOf(searchIndex + 1, end)
                    }
                }
            } catch (_: Exception) {}
        }
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

/**
 * Real multiline code-editing component for LaTeX source editing.
 * Features:
 * - Line numbers gutter with baseline pixel alignment
 * - Active line highlight behind cursor
 * - Syntax highlighting (LaTeX macros, numbers, math, comments, keys)
 * - Auto-indentation on Enter
 * - Undo/Redo stack
 * - Visible blue cursor with precise placement
 * - Monospace typography with dynamic font size
 * - Quick accessory symbol bar for fast mobile LaTeX input
 */
@Composable
fun LatexCodeEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: Float = 12f,
    searchQuery: String = "",
    currentMatchRange: TextRange? = null,
    undoRedoManager: UndoRedoManager = remember { UndoRedoManager() },
    onUndoStateChange: (() -> Unit)? = null
) {
    // Reset layout result when text changes so stale layouts never cause out-of-bounds queries
    var textLayoutResult by remember(value.text) { mutableStateOf<TextLayoutResult?>(null) }
    var previousValue by remember { mutableStateOf(value) }
    val verticalScrollState = rememberScrollState()
    val visualTransformation = remember(searchQuery, currentMatchRange) {
        LatexSyntaxVisualTransformation(searchQuery, currentMatchRange)
    }

    val textFontSize = fontSize.sp
    val textLineHeight = (fontSize * 1.5f).sp

    val lineCount = remember(value.text) {
        maxOf(1, value.text.count { it == '\n' } + 1)
    }
    val gutterDigits = remember(lineCount) {
        maxOf(2, lineCount.toString().length)
    }
    val gutterWidth = (gutterDigits * 9 + 20).dp

    val handleTextChange: (TextFieldValue) -> Unit = { newValue ->
        var finalValue = newValue
        try {
            // Auto-indentation when pressing Enter
            if (newValue.text.length == previousValue.text.length + 1 &&
                newValue.selection.start > 0 &&
                newValue.text[newValue.selection.start - 1] == '\n'
            ) {
                val cursor = previousValue.selection.start.coerceIn(0, previousValue.text.length)
                val textBefore = previousValue.text.substring(0, cursor)
                val lastLineBreak = textBefore.lastIndexOf('\n')
                val prevLine = if (lastLineBreak == -1) textBefore else textBefore.substring(lastLineBreak + 1)
                val leadingSpaces = prevLine.takeWhile { it == ' ' || it == '\t' }
                val trimmed = prevLine.trim()
                val extraIndent = if (trimmed.endsWith("{") || trimmed.endsWith("[") || trimmed.contains(Regex("""\\begin\{[^}]+\}$"""))) "    " else ""
                val indent = leadingSpaces + extraIndent
                if (indent.isNotEmpty()) {
                    val insertPos = newValue.selection.start.coerceIn(0, newValue.text.length)
                    val indented = newValue.text.substring(0, insertPos) + indent + newValue.text.substring(insertPos)
                    finalValue = TextFieldValue(indented, selection = TextRange(insertPos + indent.length))
                }
            }
        } catch (_: Exception) {}
        previousValue = finalValue
        undoRedoManager.push(finalValue)
        onUndoStateChange?.invoke()
        onValueChange(finalValue)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Main Editor Body (Gutter + Editor)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScrollState)
            ) {
                // Line Numbers Gutter
                Canvas(
                    modifier = Modifier
                        .width(gutterWidth)
                        .fillMaxHeight()
                ) {
                    if (size.width <= 0f || size.height <= 0f) return@Canvas
                    val rawText = value.text
                    val layout = textLayoutResult
                    val isLayoutValid = layout != null && layout.layoutInput.text.text == rawText

                    val textPaint = Paint().apply {
                        color = android.graphics.Color.parseColor("#94A3B8")
                        textSize = textFontSize.toPx()
                        typeface = Typeface.MONOSPACE
                        textAlign = Paint.Align.RIGHT
                        isAntiAlias = true
                    }

                    try {
                        var currentOffset = 0
                        var lineNum = 1
                        val lines = rawText.split('\n')

                        drawIntoCanvas { canvas ->
                            for (line in lines) {
                                val baseline = if (isLayoutValid && layout != null && layout.lineCount > 0) {
                                    val safeOffset = currentOffset.coerceIn(0, layout.layoutInput.text.length)
                                    val visualLine = try {
                                        layout.getLineForOffset(safeOffset).coerceIn(0, layout.lineCount - 1)
                                    } catch (_: Exception) { 0 }
                                    try {
                                        layout.getLineBaseline(visualLine)
                                    } catch (_: Exception) {
                                        (lineNum - 1) * textLineHeight.toPx() + textFontSize.toPx()
                                    }
                                } else {
                                    (lineNum - 1) * textLineHeight.toPx() + textFontSize.toPx()
                                }

                                if (baseline >= 0f && baseline <= size.height + 200f) {
                                    canvas.nativeCanvas.drawText(
                                        lineNum.toString(),
                                        size.width - 8.dp.toPx(),
                                        baseline,
                                        textPaint
                                    )
                                }
                                currentOffset += line.length + 1
                                lineNum++
                            }
                        }
                    } catch (_: Exception) {}

                    // Gutter right divider line
                    try {
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(size.width - 1.dp.toPx(), 0f),
                            end = Offset(size.width - 1.dp.toPx(), size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    } catch (_: Exception) {}
                }

                // Code Area with Active Line Highlight and Visible Cursor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 40.dp)
                        .drawBehind {
                            val layout = textLayoutResult
                            val rawText = value.text
                            if (layout != null && layout.layoutInput.text.text == rawText && layout.lineCount > 0 && rawText.isNotEmpty()) {
                                try {
                                    val cursorOffset = value.selection.start.coerceIn(0, layout.layoutInput.text.length)
                                    val activeVisualLine = layout.getLineForOffset(cursorOffset).coerceIn(0, layout.lineCount - 1)
                                    val activeTop = layout.getLineTop(activeVisualLine)
                                    val activeBottom = layout.getLineBottom(activeVisualLine)
                                    if (activeBottom > activeTop) {
                                        drawRect(
                                            color = Color(0xFFEFF6FF), // Soft active line highlight
                                            topLeft = Offset(0f, activeTop),
                                            size = Size(size.width, activeBottom - activeTop)
                                        )
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = handleTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    if (event.isCtrlPressed && event.key == Key.Z && !event.isShiftPressed) {
                                        val undone = undoRedoManager.undo(value)
                                        if (undone != null) {
                                            onValueChange(undone)
                                            onUndoStateChange?.invoke()
                                            return@onPreviewKeyEvent true
                                        }
                                    } else if (event.isCtrlPressed && (event.key == Key.Y || (event.key == Key.Z && event.isShiftPressed))) {
                                        val redone = undoRedoManager.redo(value)
                                        if (redone != null) {
                                            onValueChange(redone)
                                            onUndoStateChange?.invoke()
                                            return@onPreviewKeyEvent true
                                        }
                                    } else if (event.key == Key.Tab) {
                                        val pos = value.selection.start.coerceIn(0, value.text.length)
                                        val newText = value.text.substring(0, pos) + "    " + value.text.substring(value.selection.end.coerceIn(0, value.text.length))
                                        val updated = TextFieldValue(newText, selection = TextRange(pos + 4))
                                        handleTextChange(updated)
                                        return@onPreviewKeyEvent true
                                    }
                                }
                                false
                            },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = textFontSize,
                            lineHeight = textLineHeight,
                            color = Color(0xFF0F172A)
                        ),
                        cursorBrush = SolidColor(Color(0xFF2563EB)),
                        visualTransformation = visualTransformation,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii
                        ),
                        onTextLayout = { textLayoutResult = it },
                        singleLine = false
                    )
                }
            }
        }

        // Quick LaTeX Accessory Bar (mobile typing assist: tab, brackets, math, backslash, undo, redo)
        Surface(
            color = Color(0xFFF8FAFC),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(width = 1.dp, color = Color(0xFFF1F5F9))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickKeyButton(label = "Tab") {
                    val pos = value.selection.start.coerceIn(0, value.text.length)
                    val newText = value.text.substring(0, pos) + "    " + value.text.substring(value.selection.end.coerceIn(0, value.text.length))
                    handleTextChange(TextFieldValue(newText, selection = TextRange(pos + 4)))
                }
                QuickKeyButton(label = "\\") {
                    val pos = value.selection.start.coerceIn(0, value.text.length)
                    val newText = value.text.substring(0, pos) + "\\" + value.text.substring(value.selection.end.coerceIn(0, value.text.length))
                    handleTextChange(TextFieldValue(newText, selection = TextRange(pos + 1)))
                }
                QuickKeyButton(label = "{ }") {
                    val start = value.selection.start.coerceIn(0, value.text.length)
                    val end = value.selection.end.coerceIn(0, value.text.length)
                    val selected = value.text.substring(start, end)
                    val newText = value.text.substring(0, start) + "{" + selected + "}" + value.text.substring(end)
                    val newCursor = if (start == end) start + 1 else start + selected.length + 2
                    handleTextChange(TextFieldValue(newText, selection = TextRange(newCursor)))
                }
                QuickKeyButton(label = "[ ]") {
                    val start = value.selection.start.coerceIn(0, value.text.length)
                    val end = value.selection.end.coerceIn(0, value.text.length)
                    val selected = value.text.substring(start, end)
                    val newText = value.text.substring(0, start) + "[" + selected + "]" + value.text.substring(end)
                    val newCursor = if (start == end) start + 1 else start + selected.length + 2
                    handleTextChange(TextFieldValue(newText, selection = TextRange(newCursor)))
                }
                QuickKeyButton(label = "$") {
                    val start = value.selection.start.coerceIn(0, value.text.length)
                    val end = value.selection.end.coerceIn(0, value.text.length)
                    val selected = value.text.substring(start, end)
                    val newText = if (start == end) {
                        value.text.substring(0, start) + "$" + value.text.substring(end)
                    } else {
                        value.text.substring(0, start) + "$" + selected + "$" + value.text.substring(end)
                    }
                    val newCursor = if (start == end) start + 1 else start + selected.length + 2
                    handleTextChange(TextFieldValue(newText, selection = TextRange(newCursor)))
                }
                QuickKeyButton(label = "^") {
                    val pos = value.selection.start.coerceIn(0, value.text.length)
                    val newText = value.text.substring(0, pos) + "^" + value.text.substring(value.selection.end.coerceIn(0, value.text.length))
                    handleTextChange(TextFieldValue(newText, selection = TextRange(pos + 1)))
                }
                QuickKeyButton(label = "_") {
                    val pos = value.selection.start.coerceIn(0, value.text.length)
                    val newText = value.text.substring(0, pos) + "_" + value.text.substring(value.selection.end.coerceIn(0, value.text.length))
                    handleTextChange(TextFieldValue(newText, selection = TextRange(pos + 1)))
                }
                QuickKeyButton(label = "%") {
                    val pos = value.selection.start.coerceIn(0, value.text.length)
                    val newText = value.text.substring(0, pos) + "%" + value.text.substring(value.selection.end.coerceIn(0, value.text.length))
                    handleTextChange(TextFieldValue(newText, selection = TextRange(pos + 1)))
                }
                QuickKeyButton(label = "&") {
                    val pos = value.selection.start.coerceIn(0, value.text.length)
                    val newText = value.text.substring(0, pos) + "&" + value.text.substring(value.selection.end.coerceIn(0, value.text.length))
                    handleTextChange(TextFieldValue(newText, selection = TextRange(pos + 1)))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        val undone = undoRedoManager.undo(value)
                        if (undone != null) {
                            onValueChange(undone)
                            onUndoStateChange?.invoke()
                        }
                    },
                    enabled = undoRedoManager.canUndo,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (undoRedoManager.canUndo) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = {
                        val redone = undoRedoManager.redo(value)
                        if (redone != null) {
                            onValueChange(redone)
                            onUndoStateChange?.invoke()
                        }
                    },
                    enabled = undoRedoManager.canRedo,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (undoRedoManager.canRedo) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickKeyButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
