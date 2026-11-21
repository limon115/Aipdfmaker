import os
import re

home_file = "app/src/main/java/com/example/ui/screens/home/HomeScreen.kt"
main_file = "app/src/main/java/com/example/ui/screens/MainScreen.kt"

# 1. Clean up HomeScreen
if os.path.exists(home_file):
    with open(home_file, "r") as f:
        home_content = f.read()
    
    # Surgically remove the TermuxControlCard block using regex to ignore exact whitespace
    home_content = re.sub(r'item\s*\{\s*TermuxControlCard\([^)]+\)\s*\}', '', home_content)
    
    with open(home_file, "w") as f:
        f.write(home_content)
    print("✅ Removed TermuxControlCard from HomeScreen")

# 2. Inject into Dashboard (MainScreen)
if os.path.exists(main_file):
    with open(main_file, "r") as f:
        main_content = f.read()

    if "TermuxControlCard" not in main_content:
        # Add the import at the top of the file
        main_content = main_content.replace(
            "package com.example.ui.screens", 
            "package com.example.ui.screens\n\nimport com.example.ui.components.glass.TermuxControlCard"
        )
        
        # Drop the Termux card right above the AiUsageDashboardCard
        main_content = main_content.replace(
            "com.example.ui.screens.settings.AiUsageDashboardCard()",
            "TermuxControlCard(modifier = Modifier.padding(16.dp))\n                        com.example.ui.screens.settings.AiUsageDashboardCard()"
        )

        with open(main_file, "w") as f:
            f.write(main_content)
        print("✅ Successfully injected TermuxControlCard into Dashboard!")
    else:
        print("⚠️ TermuxControlCard is already in the Dashboard.")
else:
    print("⚠️ MainScreen.kt not found.")
