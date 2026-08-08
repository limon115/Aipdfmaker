import sys

with open('/app/applet/app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

bad_str = """                        } else {
                            navController.navigate("notes_viewer/$projectId")
                    },
                    onNavigateHome = {
                        navController.navigate(BottomNavItem.Home.route) {
                            popUpTo(BottomNavItem.Home.route) { inclusive = false }
                        }
                        }
                    },"""

good_str = """                        } else {
                            navController.navigate("notes_viewer/$projectId")
                        }
                    },"""

content = content.replace(bad_str, good_str)

with open('/app/applet/app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)
