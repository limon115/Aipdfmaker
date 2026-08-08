import sys

with open('/app/applet/app/src/main/java/com/example/ui/navigation/BottomNavItem.kt', 'r') as f:
    nav = f.read()

nav = nav.replace("androidx.compose.material.icons.filled.Info", "androidx.compose.material.icons.filled.Info")
# Wait, I had: nav.replace("androidx.compose.material.icons.filled.Analytics", "androidx.compose.material.icons.filled.Info") which resulted in "androidx.compose.material.icons.filled.Info" literally? Wait, the compiler said "Unresolved reference 'Info'".
# Ah, the Kotlin code was: object Dashboard : BottomNavItem("dashboard", "Dashboard", androidx.compose.material.icons.filled.Info, androidx.compose.material.icons.outlined.Info)
# Wait, in Kotlin, Icons.Filled is an object, and Info is an extension property. So it's `androidx.compose.material.icons.filled.Info`. Wait, no, it's `androidx.compose.material.icons.Icons.Filled.Info`!
# Let's fix `BottomNavItem.kt` completely.

nav_code = """package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Dashboard : BottomNavItem("dashboard", "Dashboard", Icons.Filled.Info, Icons.Outlined.Info)
    object Settings : BottomNavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}
"""

with open('/app/applet/app/src/main/java/com/example/ui/navigation/BottomNavItem.kt', 'w') as f:
    f.write(nav_code)

with open('/app/applet/app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    main_code = f.read()

# Fix composable context
if "import androidx.compose.foundation.layout.Column" not in main_code:
    main_code = "import androidx.compose.foundation.layout.Column\n" + main_code
if "import androidx.compose.ui.unit.dp" not in main_code:
    main_code = "import androidx.compose.ui.unit.dp\n" + main_code

with open('/app/applet/app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(main_code)

print("Fixed")
