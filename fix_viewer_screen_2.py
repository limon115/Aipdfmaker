import os

with open("app/src/main/java/com/example/ui/screens/viewer/NotesViewerScreen.kt", "r") as f:
    text = f.read()

# Let's find ExportPreviewModal and extract its body.
import re
match = re.search(r'(@Composable\s+fun ExportPreviewModal.*)', text, re.DOTALL)
if match:
    export_modal = match.group(1)
    
    # Let's clean up the extra state.isExporting inside ExportPreviewModal
    idx = export_modal.find('    if (state.isExporting) {')
    if idx != -1:
        clean_modal = export_modal[:idx] + "}\n"
        text = text.replace(export_modal, clean_modal)
    
with open("app/src/main/java/com/example/ui/screens/viewer/NotesViewerScreen.kt", "w") as f:
    f.write(text)

