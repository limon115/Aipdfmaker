import sys

with open('/app/applet/app/src/main/java/com/example/ui/navigation/BottomNavItem.kt', 'r') as f:
    nav = f.read()

nav = nav.replace("androidx.compose.material.icons.filled.Analytics", "androidx.compose.material.icons.filled.Info")
nav = nav.replace("androidx.compose.material.icons.outlined.Analytics", "androidx.compose.material.icons.outlined.Info")

with open('/app/applet/app/src/main/java/com/example/ui/navigation/BottomNavItem.kt', 'w') as f:
    f.write(nav)

with open('/app/applet/app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("BottomNavItem.Bookmarks,", "BottomNavItem.Dashboard,")
content = content.replace("BottomNavItem.Bookmarks.route", "BottomNavItem.Dashboard.route")

old_screen = 'CenteredText("Bookmarks Screen")'
new_screen = """
                Column(modifier = androidx.compose.ui.Modifier.fillMaxSize().padding(16.dp).padding(top = 48.dp)) {
                    com.example.ui.screens.settings.AiUsageDashboardCard()
                }"""

content = content.replace(old_screen, new_screen)

with open('/app/applet/app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)

print("Fixed again")
