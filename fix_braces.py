with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    content = f.read()

content = content.replace("    }\n}\n\n@Composable\nfun MainScreenContent", "    }\n}\n}\n\n@Composable\nfun MainScreenContent")

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(content)
