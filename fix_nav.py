import sys

with open('/app/applet/app/src/main/java/com/example/ui/navigation/BottomNavItem.kt', 'r') as f:
    nav = f.read()

nav = nav.replace("Icons.Filled.Bookmark", "androidx.compose.material.icons.filled.Analytics").replace("Icons.Outlined.BookmarkBorder", "androidx.compose.material.icons.outlined.Analytics")
nav = nav.replace('object Bookmarks : BottomNavItem("bookmarks", "Bookmarks",', 'object Dashboard : BottomNavItem("dashboard", "Dashboard",')
nav = nav.replace("import androidx.compose.material.icons.filled.Bookmark\n", "")
nav = nav.replace("import androidx.compose.material.icons.outlined.BookmarkBorder\n", "")

with open('/app/applet/app/src/main/java/com/example/ui/navigation/BottomNavItem.kt', 'w') as f:
    f.write(nav)

print("Fixed")
