import os

filepath = "app/src/main/java/com/example/ui/screens/viewer/NotesViewerViewModel.kt"
with open(filepath, "r") as f:
    text = f.read()

# Expose exportProgress
progress_str = """    val state: StateFlow<NotesViewerState> = _state.asStateFlow()

    val exportProgress: StateFlow<Float> = exportEngine.exportProgress
"""
text = text.replace('    val state: StateFlow<NotesViewerState> = _state.asStateFlow()\n', progress_str)

with open(filepath, "w") as f:
    f.write(text)
