import re
import os

editor_file = "java/com/example/ui/components/LatexCodeEditor.kt"
notes_file = "java/com/example/ui/screens/viewer/NotesViewerScreen.kt"

# --- 1. Fix LatexCodeEditor.kt ---
with open(editor_file, "r") as f:
    code = f.read()

# Add EditorColorScheme import if missing
if "io.github.rosemoe.sora.widget.schemes.EditorColorScheme" not in code:
    code = code.replace("import io.github.rosemoe.sora.widget.EditorColorScheme", "import io.github.rosemoe.sora.widget.schemes.EditorColorScheme")

# Fix props (Sora 0.23+ removed some props, we'll configure what's available)
code = code.replace("props.useSpacesForTabs = true", "// props.useSpacesForTabs = true")
code = code.replace("props.tabSize = 4", "tabWidth = 4")
code = code.replace("props.isAutoIndentEnabled = true", "// auto indent handled differently in newer versions")

# Fix TextRange internal constructor errors and CharPosition comparison
code = code.replace("val left = text.indexer.getCharPosition(cursor.leftLine, cursor.leftColumn)", 
                    "val left = text.indexer.getCharPosition(cursor.leftLine, cursor.leftColumn).index")
code = code.replace("val right = text.indexer.getCharPosition(cursor.rightLine, cursor.rightColumn)", 
                    "val right = text.indexer.getCharPosition(cursor.rightLine, cursor.rightColumn).index")
code = code.replace("val actualStart = editor.text.indexer.getCharPosition(editor.cursor.leftLine, editor.cursor.leftColumn)", 
                    "val actualStart = editor.text.indexer.getCharPosition(editor.cursor.leftLine, editor.cursor.leftColumn).index")
code = code.replace("val actualEnd = editor.text.indexer.getCharPosition(editor.cursor.rightLine, editor.cursor.rightColumn)", 
                    "val actualEnd = editor.text.indexer.getCharPosition(editor.cursor.rightLine, editor.cursor.rightColumn).index")

# Ensure TextRange import is correct
if "import androidx.compose.ui.text.TextRange" not in code:
    code = code.replace("import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.text.TextRange")

with open(editor_file, "w") as f:
    f.write(code)

print("✅ Fixed Sora Editor API changes in LatexCodeEditor.kt")

# --- 2. Fix NotesViewerScreen.kt (Restore LatexSearchIndex locally) ---
if os.path.exists(notes_file):
    with open(notes_file, "r") as f:
        notes_code = f.read()
    
    # We need to inject the LatexSearchIndex class into NotesViewerScreen.kt 
    # since it was deleted from LatexCodeEditor.kt
    search_index_class = """
/**
 * In-memory search index caching full document text and query results.
 */
class LatexSearchIndex(val sourceText: String) {
    private val queryCache = mutableMapOf<String, List<androidx.compose.ui.text.TextRange>>()
    val length: Int get() = sourceText.length
    fun search(query: String): List<androidx.compose.ui.text.TextRange> {
        if (query.isBlank() || sourceText.isEmpty()) return emptyList()
        val normalized = query.lowercase()
        val cached = queryCache[normalized]
        if (cached != null) return cached
        val matches = ArrayList<androidx.compose.ui.text.TextRange>()
        val qLen = query.length
        var idx = 0
        while (idx < sourceText.length) {
            val found = sourceText.indexOf(query, idx, ignoreCase = true)
            if (found == -1) break
            val end = (found + qLen).coerceAtMost(sourceText.length)
            matches.add(androidx.compose.ui.text.TextRange(found, end))
            idx = maxOf(idx + 1, end)
        }
        queryCache[normalized] = matches
        return matches
    }
}
"""
    if "class LatexSearchIndex" not in notes_code:
        # Inject the class right before the @Composable fun NotesViewerScreen
        notes_code = notes_code.replace("@Composable\nfun NotesViewerScreen", search_index_class + "\n@Composable\nfun NotesViewerScreen")
        
        with open(notes_file, "w") as f:
            f.write(notes_code)
        print("✅ Restored LatexSearchIndex in NotesViewerScreen.kt")
    else:
        print("✅ LatexSearchIndex already exists in NotesViewerScreen.kt")
else:
    print("⚠️ NotesViewerScreen.kt not found, skipping.")

