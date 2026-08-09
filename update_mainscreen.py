import re

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    text = f.read()

target = r"""            composable\(BottomNavItem\.Home\.route\) \{
                HomeScreen\(
                    viewModel = homeViewModel,
                    onNavigateToProject = \{ projectId, status ->
                        if \(status == "Processing"\) \{
                            navController\.navigate\("processing/\$projectId"\)
                        \} else \{
                            navController\.navigate\("notes_viewer/\$projectId"\)
                        \}
                    \},
                    onNavigateToNewProject = \{
                        navController\.navigate\("input_source"\)
                    \}
                \)
            \}"""

replacement = """            composable(BottomNavItem.Home.route) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToProject = { projectId, status ->
                        if (status == "Processing") {
                            coroutineScope.kotlinx.coroutines.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val workManager = androidx.work.WorkManager.getInstance(context)
                                val workInfos = workManager.getWorkInfosForUniqueWork("NoteGen_${projectId}").get()
                                val isGenerating = workInfos.isNotEmpty() && !workInfos.first().state.isFinished
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    if (isGenerating) {
                                        navController.navigate("note_generation/$projectId")
                                    } else {
                                        navController.navigate("processing/$projectId")
                                    }
                                }
                            }
                        } else {
                            navController.navigate("notes_viewer/$projectId")
                        }
                    },
                    onNavigateToNewProject = {
                        navController.navigate("input_source")
                    }
                )
            }"""

text = re.sub(target, replacement, text)

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(text)
