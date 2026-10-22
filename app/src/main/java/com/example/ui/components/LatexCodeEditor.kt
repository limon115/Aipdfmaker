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
 * High-performance state-based undo/redo manager tracking [TextFieldValue] history.
 * Features intelligent typing batching, deep history stack, and Compose-reactive [canUndo]/[canRedo] states.
 */
class UndoRedoManager(private val maxHistory: Int = 300) {
    private val undoStack = mutableListOf<TextFieldValue>()
    private val redoStack = mutableListOf<TextFieldValue>()
    private var isPerforming = false
    private var lastPushTimestamp = 0L

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    private fun updateStateFlags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    fun reset(initial: TextFieldValue) {
        undoStack.clear()
        redoStack.clear()
        lastPushTimestamp = System.currentTimeMillis()
        updateStateFlags()
    }

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
        lastPushTimestamp = System.currentTimeMillis()
        updateStateFlags()
    }

    fun push(oldValue: TextFieldValue, newValue: TextFieldValue, forceCheckpoint: Boolean = false) {
        if (isPerforming) return
        if (oldValue.text == newValue.text) {
            return
        }

        val now = System.currentTimeMillis()
        val timeSinceLast = now - lastPushTimestamp
        val lengthDiff = kotlin.math.abs(newValue.text.length - oldValue.text.length)

        val isNewline = newValue.text.length > oldValue.text.length &&
                newValue.selection.start > 0 &&
                newValue.text.getOrNull(newValue.selection.start - 1) == '\n'
        val isWordBoundary = newValue.text.length > oldValue.text.length &&
                newValue.selection.start > 0 &&
                newValue.text.getOrNull(newValue.selection.start - 1) in listOf(' ', '\t', '{', '}', '[', ']', '$', '\\', '%', '&')
        val isLargeEdit = lengthDiff > 1
        val isPause = timeSinceLast > 750L

        if (undoStack.isEmpty() || forceCheckpoint || isPause || isLargeEdit || isNewline || isWordBoundary) {
            undoStack.add(oldValue)
            if (undoStack.size > maxHistory) {
                undoStack.removeAt(0)
            }
        }
        redoStack.clear()
        lastPushTimestamp = now
        updateStateFlags()
    }

    fun undo(current: TextFieldValue): TextFieldValue? {
        if (undoStack.isEmpty()) return null
        isPerforming = true
        redoStack.add(current)
        val target = undoStack.removeAt(undoStack.lastIndex)
        isPerforming = false
        lastPushTimestamp = System.currentTimeMillis()
        updateStateFlags()
        return target
    }

    fun redo(current: TextFieldValue): TextFieldValue? {
        if (redoStack.isEmpty()) return null
        isPerforming = true
        undoStack.add(current)
        val target = redoStack.removeAt(redoStack.lastIndex)
        isPerforming = false
        lastPushTimestamp = System.currentTimeMillis()
        updateStateFlags()
        return target
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateStateFlags()
    }
}

/**
 * In-memory search index caching full LaTeX document text and query results.
 * Delivers instantaneous sub-millisecond search results without re-scanning
 * the entire code on every frame.
 */
class LatexSearchIndex(val sourceText: String) {
    private val queryCache = mutableMapOf<String, List<TextRange>>()

    val length: Int get() = sourceText.length

    fun search(query: String): List<TextRange> {
        if (query.isBlank() || sourceText.isEmpty()) return emptyList()
        val normalized = query.lowercase()
        val cached = queryCache[normalized]
        if (cached != null) return cached

        val matches = ArrayList<TextRange>()
        val qLen = query.length
        var idx = 0
        while (idx < sourceText.length) {
            val found = sourceText.indexOf(query, idx, ignoreCase = true)
            if (found == -1) break
            val end = (found + qLen).coerceAtMost(sourceText.length)
            matches.add(TextRange(found, end))
            idx = maxOf(idx + 1, end)
        }
        queryCache[normalized] = matches
        return matches
    }
}

/**
 * Real-time syntax highlighter for LaTeX source code with memoized transformation.
 * Uses precompiled regex constants and caches span transformations to prevent
 * frame drops on cursor movements, scrolling, or recomposition.
 * Preserves the exact underlying text verbatim with [OffsetMapping.Identity]
 * to ensure zero cursor drift and 100% exact text fidelity.
 */
class LatexSyntaxVisualTransformation(
    private val searchQuery: String = "",
    private val currentMatchRange: TextRange? = null
) : VisualTransformation {

    companion object {
        private val COMMENT_REGEX = Regex("%.*$", RegexOption.MULTILINE)
        private val COMMAND_REGEX = Regex("""\\[a-zA-Z@]+""")
        private val NUMBER_REGEX = Regex("""\b\d+(\.\d+)?\b""")
        private val KEY_REGEX = Regex("""[\[,]\s*([a-zA-Z0-9_\-]+)\s*=""")
        private val MATH_REGEX = Regex("""\$\$[\s\S]*?\$\$|\$[^\$\n]*?\$""")
    }

    // Memoization cache to make cursor moves, scroll, and identical text transformations instantaneous
    private var lastRawText: String? = null
    private var lastSearchQuery: String? = null
    private var lastMatchRange: TextRange? = null
    private var cachedResult: TransformedText? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        if (raw == lastRawText &&
            searchQuery == lastSearchQuery &&
            currentMatchRange == lastMatchRange &&
            cachedResult != null
        ) {
            return cachedResult!!
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
                for (match in COMMENT_REGEX.findAll(raw)) {
                    safeAddStyle(
                        SpanStyle(color = Color(0xFF64748B), fontStyle = FontStyle.Italic),
                        match.range.first,
                        match.range.last + 1
                    )
                }

                // 2. Control sequences / Commands: \documentclass, \usepackage, etc. (vibrant blue)
                for (match in COMMAND_REGEX.findAll(raw)) {
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
                for (match in NUMBER_REGEX.findAll(raw)) {
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
                for (match in KEY_REGEX.findAll(raw)) {
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
                for (match in MATH_REGEX.findAll(raw)) {
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
                    val qLen = searchQuery.length
                    while (searchIndex < raw.length) {
                        val found = raw.indexOf(searchQuery, searchIndex, ignoreCase = true)
                        if (found == -1) break
                        val end = (found + qLen).coerceAtMost(raw.length)
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
        val result = TransformedText(highlighted, OffsetMapping.Identity)
        lastRawText = raw
        lastSearchQuery = searchQuery
        lastMatchRange = currentMatchRange
        cachedResult = result
        return result
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

    LaunchedEffect(value) {
        if (previousValue != value) {
            previousValue = value
        }
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
        var indentApplied = false
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
                    indentApplied = true
                }
            }
        } catch (_: Exception) {}
        undoRedoManager.push(previousValue, finalValue, forceCheckpoint = indentApplied)
        previousValue = finalValue
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
                        var nextBreak = rawText.indexOf('\n', currentOffset)

                        drawIntoCanvas { canvas ->
                            while (currentOffset <= rawText.length) {
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

                                if (baseline >= -50f && baseline <= size.height + 200f) {
                                    canvas.nativeCanvas.drawText(
                                        lineNum.toString(),
                                        size.width - 8.dp.toPx(),
                                        baseline,
                                        textPaint
                                    )
                                }

                                if (nextBreak == -1) break
                                currentOffset = nextBreak + 1
                                nextBreak = rawText.indexOf('\n', currentOffset)
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
                                            previousValue = undone
                                            onValueChange(undone)
                                            onUndoStateChange?.invoke()
                                            return@onPreviewKeyEvent true
                                        }
                                    } else if (event.isCtrlPressed && (event.key == Key.Y || (event.key == Key.Z && event.isShiftPressed))) {
                                        val redone = undoRedoManager.redo(value)
                                        if (redone != null) {
                                            previousValue = redone
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
                val insertSnippet: (String, (String) -> Pair<String, Int>) -> Unit = { _, transform ->
                    val start = value.selection.start.coerceIn(0, value.text.length)
                    val end = value.selection.end.coerceIn(0, value.text.length)
                    val selected = value.text.substring(start, end)
                    val (replacement, cursorOffset) = transform(selected)
                    val newText = value.text.substring(0, start) + replacement + value.text.substring(end)
                    val newCursor = (start + cursorOffset).coerceIn(0, newText.length)
                    val newVal = TextFieldValue(newText, selection = TextRange(newCursor))
                    undoRedoManager.push(previousValue, newVal, forceCheckpoint = true)
                    previousValue = newVal
                    onUndoStateChange?.invoke()
                    onValueChange(newVal)
                }

                QuickKeyButton(label = "Tab") {
                    insertSnippet("Tab") { "    " to 4 }
                }
                QuickKeyButton(label = "\\") {
                    insertSnippet("\\") { "\\" to 1 }
                }
                QuickKeyButton(label = "{ }") {
                    insertSnippet("{ }") { sel -> if (sel.isEmpty()) "{}" to 1 else "{$sel}" to (sel.length + 2) }
                }
                QuickKeyButton(label = "[ ]") {
                    insertSnippet("[ ]") { sel -> if (sel.isEmpty()) "[]" to 1 else "[$sel]" to (sel.length + 2) }
                }
                QuickKeyButton(label = "$") {
                    insertSnippet("$") { sel -> if (sel.isEmpty()) "$" to 1 else "$$sel$" to (sel.length + 2) }
                }
                QuickKeyButton(label = "^") {
                    insertSnippet("^") { "^" to 1 }
                }
                QuickKeyButton(label = "_") {
                    insertSnippet("_") { "_" to 1 }
                }
                QuickKeyButton(label = "%") {
                    insertSnippet("%") { "%" to 1 }
                }
                QuickKeyButton(label = "&") {
                    insertSnippet("&") { "&" to 1 }
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        val undone = undoRedoManager.undo(value)
                        if (undone != null) {
                            previousValue = undone
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
                            previousValue = redone
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
