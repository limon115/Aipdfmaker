import re

with open('app/src/main/java/com/example/ui/screens/home/HomeScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('val tabs = listOf("All", "Completed", "Processing")', 'val tabs = listOf("All", "Notes", "Debugged", "Tasks")')

tab_logic = """
        when (selectedTab) {
            1 -> projects.filter { it.status == "Completed" }
            2 -> projects.filter { it.status == "Debugged" }
            3 -> projects.filter { it.status != "Completed" && it.status != "Debugged" }
            else -> projects
        }
"""
content = re.sub(r'when \(selectedTab\) \{[\s\S]*?else -> projects\n\s*\}', tab_logic.strip(), content)

with open('app/src/main/java/com/example/ui/screens/home/HomeScreen.kt', 'w') as f:
    f.write(content)
