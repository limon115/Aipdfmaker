with open("app/src/main/java/com/example/ui/screens/viewer/NotesViewerScreen.kt", "r") as f:
    text = f.read()

import re
text = re.sub(r'(\s*val exportProgress by viewModel\.exportProgress\.collectAsState\(\))+\n', r'\n    val exportProgress by viewModel.exportProgress.collectAsState()\n', text)

with open("app/src/main/java/com/example/ui/screens/viewer/NotesViewerScreen.kt", "w") as f:
    f.write(text)

