import os

file_path = "app/src/main/java/com/example/ui/screens/home/HomeScreen.kt"

if os.path.exists(file_path):
    with open(file_path, "r") as f:
        content = f.read()

    if "TermuxControlCard" not in content:
        # 1. Add the UI import
        content = content.replace(
            "import com.example.ui.components.glass.GlassCard", 
            "import com.example.ui.components.glass.GlassCard\nimport com.example.ui.components.glass.TermuxControlCard"
        )
        
        # 2. Inject the Termux Card directly above the project list
        # By replacing the first 'items(', we safely drop it into the LazyColumn
        injection = """item {
                TermuxControlCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            
            items("""
            
        content = content.replace("items(", injection, 1)
        
        with open(file_path, "w") as f:
            f.write(content)
        print("✅ TermuxControlCard successfully injected into HomeScreen!")
    else:
        print("⚠️ TermuxControlCard is already in HomeScreen.")
else:
    print("⚠️ HomeScreen.kt not found.")
