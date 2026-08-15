import os

filepath = "app/src/main/java/com/example/ui/screens/viewer/NotesViewerViewModel.kt"
with open(filepath, 'r') as f:
    code = f.read()

# Fix the escaped dollar sign in the string interpolation
old_line = 'val tex = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS), "aipdfs/\\$safeName/document.tex")'
new_line = 'val tex = File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOCUMENTS), "aipdfs/$safeName/document.tex")'

if old_line in code:
    code = code.replace(old_line, new_line)
    with open(filepath, 'w') as f:
        f.write(code)
    print("✅ NotesViewerViewModel patched: String interpolation bug fixed!")
else:
    print("⚡ Warning: Could not find the exact line to fix. Did it already get formatted?")
