import sys

with open('app/src/main/java/com/example/ui/screens/settings/ApiLabScreen.kt', 'r') as f:
    screen = f.read()

old_build = "import androidx.compose.material.icons.filled.Build\n"
if old_build in screen:
    screen = screen.replace(old_build, "")
    
old_chevron = "import androidx.compose.material.icons.filled.ChevronRight\n"
if old_chevron in screen:
    screen = screen.replace(old_chevron, "")

with open('app/src/main/java/com/example/ui/screens/settings/ApiLabScreen.kt', 'w') as f:
    f.write(screen)
