import os

file_path = "java/com/example/ui/screens/viewer/NotesViewerScreen.kt"

if os.path.exists(file_path):
    with open(file_path, "r") as f:
        lines = f.readlines()
    
    # 1. Clean up the misplaced @file:OptIn from the previous command
    cleaned_lines = [line for line in lines if "@file:OptIn" not in line]
    
    # 2. Prepend it to the absolute top of the file (before the package declaration)
    final_content = "@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)\n" + "".join(cleaned_lines)
    
    with open(file_path, "w") as f:
        f.write(final_content)
        
    print("✅ Fixed Kotlin file annotation syntax in NotesViewerScreen.kt")
else:
    print("⚠️ File not found!")

