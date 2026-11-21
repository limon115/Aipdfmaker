import os
import re

home_file = "app/src/main/java/com/example/ui/screens/home/HomeScreen.kt"
main_file = "app/src/main/java/com/example/ui/screens/MainScreen.kt"

# 1. Double-check the Home Screen is entirely clean
if os.path.exists(home_file):
    with open(home_file, "r") as f:
        home_content = f.read()
    
    # Strip any lingering Termux cards from the Home LazyColumn
    home_content = re.sub(r'item\s*\{\s*TermuxControlCard[^}]+\}\s*', '', home_content)
    
    with open(home_file, "w") as f:
        f.write(home_content)

# 2. Swap the order in the MainScreen Dashboard
if os.path.exists(main_file):
    with open(main_file, "r") as f:
        main_content = f.read()

    # First, strip out the existing TermuxControlCard call from the top to prevent duplicates
    main_content = re.sub(r'\s*TermuxControlCard\([^)]+\)', '', main_content)
    
    # Now, find the AI Usage card and insert the Termux card right below it
    target = "com.example.ui.screens.settings.AiUsageDashboardCard()"
    replacement = """com.example.ui.screens.settings.AiUsageDashboardCard()
                        Spacer(modifier = Modifier.height(16.dp))
                        TermuxControlCard(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 32.dp))"""
    
    main_content = main_content.replace(target, replacement)

    with open(main_file, "w") as f:
        f.write(main_content)
    print("✅ TermuxControlCard successfully moved to the BOTTOM of the Dashboard!")
else:
    print("⚠️ MainScreen.kt not found.")
