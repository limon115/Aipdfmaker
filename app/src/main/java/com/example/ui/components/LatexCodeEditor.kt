package com.example.ui.components

import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorColorScheme
import io.github.rosemoe.sora.widget.EditorSearcher

// Dummy class to maintain 100% API compatibility with existing callers.
// Real undo/redo logic is now managed natively inside Sora.
class UndoRedoManager(private val maxHistory: Int = 300) {
    var canUndo by mutableStateOf(false)
    var canRedo by mutableStateOf(false)
    fun reset(initial: TextFieldValue) {}
    fun push(value: TextFieldValue) {}
    fun push(oldValue: TextFieldValue, newValue: TextFieldValue, forceCheckpoint: Boolean = false) {}
    fun undo(current: TextFieldValue): TextFieldValue? = null
    fun redo(current: TextFieldValue): TextFieldValue? = null
    fun clear() {}
}

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
    var editorView by remember { mutableStateOf<CodeEditor?>(null) }
    var isSettingText by remember { mutableStateOf(false) }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }

    // Sync Search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            editorView?.searcher?.search(
                searchQuery,
                EditorSearcher.SearchOptions(EditorSearcher.SearchOptions.TYPE_NORMAL, true)
            )
        } else {
            editorView?.searcher?.stopSearch()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    CodeEditor(ctx).apply {
                        typefaceText = Typeface.MONOSPACE
                        setTextSize(fontSize)
                        isLineNumberEnabled = true
                        props.deleteMultiSpaces = 1
                        props.useSpacesForTabs = true
                        props.tabSize = 4
                        props.isAutoIndentEnabled = true

                        // Replicate exactly the legacy visual aesthetics
                        val scheme = colorScheme
                        scheme.setColor(EditorColorScheme.WHOLE_BACKGROUND, android.graphics.Color.TRANSPARENT)
                        scheme.setColor(EditorColorScheme.TEXT_NORMAL, android.graphics.Color.parseColor("#0F172A"))
                        scheme.setColor(EditorColorScheme.LINE_NUMBER_NORMAL, android.graphics.Color.parseColor("#94A3B8"))
                        scheme.setColor(EditorColorScheme.LINE_DIVIDER, android.graphics.Color.parseColor("#E2E8F0"))
                        scheme.setColor(EditorColorScheme.CURRENT_LINE, android.graphics.Color.parseColor("#EFF6FF"))
                        scheme.setColor(EditorColorScheme.SELECTION_INSERT, android.graphics.Color.parseColor("#2563EB"))
                        scheme.setColor(EditorColorScheme.MATCHED_TEXT_BACKGROUND, android.graphics.Color.parseColor("#FEF08A"))

                        subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
                            canUndo = canUndo()
                            canRedo = canRedo()
                            undoRedoManager.canUndo = canUndo
                            undoRedoManager.canRedo = canRedo
                            onUndoStateChange?.invoke()
                            
                            if (!isSettingText) {
                                val txt = text.toString()
                                val left = text.indexer.getCharPosition(cursor.leftLine, cursor.leftColumn)
                                val right = text.indexer.getCharPosition(cursor.rightLine, cursor.rightColumn)
                                onValueChange(TextFieldValue(txt, TextRange(left, right)))
                            }
                        }
                        
                        subscribeEvent(SelectionChangeEvent::class.java) { _, _ ->
                            if (!isSettingText) {
                                val txt = text.toString()
                                val left = text.indexer.getCharPosition(cursor.leftLine, cursor.leftColumn)
                                val right = text.indexer.getCharPosition(cursor.rightLine, cursor.rightColumn)
                                onValueChange(TextFieldValue(txt, TextRange(left, right)))
                            }
                        }
                        
                        editorView = this
                    }
                },
                update = { editor ->
                    if (editor.text.toString() != value.text) {
                        isSettingText = true
                        editor.setText(value.text)
                        isSettingText = false
                    }
                    
                    val expectedStart = value.selection.start.coerceIn(0, editor.text.length)
                    val expectedEnd = value.selection.end.coerceIn(0, editor.text.length)
                    val actualStart = editor.text.indexer.getCharPosition(editor.cursor.leftLine, editor.cursor.leftColumn)
                    val actualEnd = editor.text.indexer.getCharPosition(editor.cursor.rightLine, editor.cursor.rightColumn)
                    
                    if ((expectedStart != actualStart || expectedEnd != actualEnd)) {
                        isSettingText = true
                        try {
                            val startLine = editor.text.indexer.getCharLine(expectedStart)
                            val startCol = editor.text.indexer.getCharColumn(expectedStart)
                            val endLine = editor.text.indexer.getCharLine(expectedEnd)
                            val endCol = editor.text.indexer.getCharColumn(expectedEnd)
                            editor.setSelectionRegion(startLine, startCol, endLine, endCol)
                        } catch (e: Exception) {}
                        isSettingText = false
                    }
                },
                modifier = Modifier.fillMaxSize().padding(bottom = 0.dp)
            )
        }

        // Quick LaTeX Accessory Bar
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
                    onValueChange(newVal)
                }

                QuickKeyButton(label = "Tab") { insertSnippet("Tab") { "    " to 4 } }
                QuickKeyButton(label = "\\") { insertSnippet("\\") { "\\" to 1 } }
                QuickKeyButton(label = "{ }") { insertSnippet("{ }") { sel -> if (sel.isEmpty()) "{}" to 1 else "{$sel}" to (sel.length + 2) } }
                QuickKeyButton(label = "[ ]") { insertSnippet("[ ]") { sel -> if (sel.isEmpty()) "[]" to 1 else "[$sel]" to (sel.length + 2) } }
                QuickKeyButton(label = "$") { insertSnippet("$") { sel -> if (sel.isEmpty()) "$" to 1 else "$$sel$" to (sel.length + 2) } }
                QuickKeyButton(label = "^") { insertSnippet("^") { "^" to 1 } }
                QuickKeyButton(label = "_") { insertSnippet("_") { "_" to 1 } }
                QuickKeyButton(label = "%") { insertSnippet("%") { "%" to 1 } }
                QuickKeyButton(label = "&") { insertSnippet("&") { "&" to 1 } }
                Spacer(modifier = Modifier.width(4.dp))
                
                IconButton(
                    onClick = { editorView?.undo() },
                    enabled = canUndo,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = { editorView?.redo() },
                    enabled = canRedo,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickKeyButton(label: String, onClick: () -> Unit) {
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
