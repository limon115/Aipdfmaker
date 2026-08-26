import re

with open("/tmp/MainScreen.kt", "r") as f:
    content = f.read()

# Locate the Scaffold block inside MainScreen
scaffold_start = content.find("    com.example.ui.theme.GlassBackground {\n    Scaffold(")
if scaffold_start == -1:
    print("Could not find GlassBackground { Scaffold(")
    exit(1)

# Extract everything up to the Scaffold to keep the ViewModels intact
main_screen_start = content[:scaffold_start]

# Now we need to extract the Scaffold block and replace it with MainScreenContent
# Let's find the closing brace of the NavHost and the closing braces of Scaffold / GlassBackground / MainScreen

navhost_end = content.rfind("        }\n    }\n}")
if navhost_end == -1:
    print("Could not find end of NavHost")
    exit(1)

# Wait, the closing braces at the end are:
# 1. NavHost closing
# 2. Scaffold content lambda closing
# 3. GlassBackground closing
# 4. MainScreen closing

# We want to insert MainScreenContent definition at the very bottom, after CenteredText if it exists.
# And inside MainScreen, we replace the GlassBackground + Scaffold with MainScreenContent(...) { NavHost(...) }

# Let's do a regex replacement for the bottom bar part.
# The whole bottom bar is statically defined in Scaffold.

new_main_screen = main_screen_start + """    MainScreenContent(
        currentRoute = currentRoute,
        onNavigate = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    ) { innerPadding ->
"""

# Now we just need to append the NavHost block.
# Let's extract the NavHost block from the original content.
navhost_start = content.find("        NavHost(\n            navController = navController")
if navhost_start == -1:
    print("Could not find NavHost")
    exit(1)

navhost_block = content[navhost_start:]
# The navhost_block currently ends with:
#         }
#     }
# }
# @Composable
# fun CenteredText(text: String) { ... }

# Find the end of the MainScreen function. It's right before @Composable fun CenteredText
centered_text_idx = navhost_block.find("@Composable\nfun CenteredText")
if centered_text_idx != -1:
    # Remove the last 3 closing braces from the navhost block that belong to Scaffold, GlassBackground, MainScreen
    navhost_content = navhost_block[:centered_text_idx].rstrip()
    # It ends with:
    #         }
    #     }
    # }
    if navhost_content.endswith("}"):
        navhost_content = navhost_content[:-1].rstrip()
    if navhost_content.endswith("}"):
        navhost_content = navhost_content[:-1].rstrip()
    if navhost_content.endswith("}"):
        navhost_content = navhost_content[:-1].rstrip()
        
    main_screen_complete = new_main_screen + navhost_content + "\n    }\n}\n\n"
    
    rest_of_file = navhost_block[centered_text_idx:]
else:
    # Just trim the last 3 braces
    navhost_content = navhost_block.rstrip()
    if navhost_content.endswith("}"):
        navhost_content = navhost_content[:-1].rstrip()
    if navhost_content.endswith("}"):
        navhost_content = navhost_content[:-1].rstrip()
    if navhost_content.endswith("}"):
        navhost_content = navhost_content[:-1].rstrip()
    main_screen_complete = new_main_screen + navhost_content + "\n    }\n}\n\n"
    rest_of_file = ""

main_screen_content_composable = """
@Composable
fun MainScreenContent(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Dashboard,
        BottomNavItem.Settings
    )

    com.example.ui.theme.GlassBackground {
        Scaffold(
            modifier = modifier,
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            bottomBar = {
                if (currentRoute in items.map { it.route }) {
                    val colors = com.example.ui.theme.AppTheme.colors
                    androidx.compose.material3.Surface(
                        modifier = Modifier
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                            .shadow(
                                elevation = 8.dp, 
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                                spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.05f),
                                ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.05f)
                            )
                            .border(
                                width = 1.dp,
                                color = colors.border,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                            ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        color = colors.surfaceElevated,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        NavigationBar(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            tonalElevation = 0.dp,
                            windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
                        ) {
                            items.forEach { item ->
                                NavigationBarItem(
                                    icon = { Icon(if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon, contentDescription = item.title) },
                                    label = { Text(item.title) },
                                    selected = currentRoute == item.route,
                                    onClick = { onNavigate(item.route) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}
"""

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(main_screen_complete + main_screen_content_composable + rest_of_file)
