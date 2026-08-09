with open("app/src/main/java/com/example/ui/screens/viewer/NotesViewerViewModel.kt", "r") as f:
    text = f.read()

text = text.replace(
    "exportEngine.exportProjectFiles(_state.value.projectName, _state.value.jsonContent)",
    "exportEngine.exportProjectFiles(_state.value.projectName, _state.value.jsonContent, _state.value.outputFormat.equals(\"pdf\", ignoreCase = true))"
)

with open("app/src/main/java/com/example/ui/screens/viewer/NotesViewerViewModel.kt", "w") as f:
    f.write(text)
