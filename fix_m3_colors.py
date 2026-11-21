import os
import re

file_path = "app/src/main/java/com/example/ui/components/glass/TermuxControlCard.kt"

if os.path.exists(file_path):
    with open(file_path, "r") as f:
        content = f.read()

    # Surgically replace the old Material 2 color API with the strict Material 3 API
    updated_content = re.sub(
        r"TextFieldDefaults\.outlinedTextFieldColors\s*\([^)]+\)",
        r"OutlinedTextFieldDefaults.colors(focusedContainerColor = Color(0x33FFFFFF), unfocusedContainerColor = Color(0x33FFFFFF), unfocusedBorderColor = Color(0xFFE2E8F0), focusedBorderColor = Color(0xFF2563EB))",
        content
    )
    
    with open(file_path, "w") as f:
        f.write(updated_content)
        
    print("✅ Fixed Material 3 TextField colors API!")
else:
    print("⚠️ TermuxControlCard.kt not found.")
