import re

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    text = f.read()

# I need to change composable("project_details") to accept an optional argument
target_project_details = r"""            composable\("project_details"\) \{
                ProjectDetailsScreen\(
                    viewModel = newProjectViewModel,
                    onCreateProject = \{ projectId ->
                        navController\.navigate\("processing/\$projectId"\)
                    \},
                    onNavigateBack = \{ navController\.popBackStack\(\) \}
                \)
            \}"""

replacement_project_details = """            composable(
                route = "project_details?projectId={projectId}",
                arguments = listOf(androidx.navigation.navArgument("projectId") { 
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                })
            ) { backStackEntry ->
                val projectIdStr = backStackEntry.arguments?.getString("projectId")
                val projectId = projectIdStr?.toIntOrNull()
                
                ProjectDetailsScreen(
                    viewModel = newProjectViewModel,
                    projectId = projectId,
                    onCreateProject = { newId ->
                        navController.navigate("processing/$newId")
                    },
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToProgress = { id -> 
                        navController.navigate("note_generation/$id")
                    },
                    onNavigateToViewer = { id -> 
                        navController.navigate("notes_viewer/$id")
                    }
                )
            }"""

text = re.sub(target_project_details, replacement_project_details, text)

# I also need to change the Home screen to route to project_details?projectId=id
target_home_nav = r"""                    onNavigateToProject = \{ projectId, status ->
                        if \(status == "Processing"\) \{
                            kotlinx\.coroutines\.GlobalScope\.launch\(kotlinx\.coroutines\.Dispatchers\.IO\) \{
                                val workManager = androidx\.work\.WorkManager\.getInstance\(context\)
                                val workInfos = workManager\.getWorkInfosForUniqueWork\("NoteGen_\$\{projectId\}"\)\.get\(\)
                                val isGenerating = workInfos\.isNotEmpty\(\)
                                kotlinx\.coroutines\.withContext\(kotlinx\.coroutines\.Dispatchers\.Main\) \{
                                    if \(isGenerating\) \{
                                        navController\.navigate\("note_generation/\$projectId"\)
                                    \} else \{
                                        navController\.navigate\("processing/\$projectId"\)
                                    \}
                                \}
                            \}
                        \} else \{
                            navController\.navigate\("notes_viewer/\$projectId"\)
                        \}
                    \},"""

replacement_home_nav = """                    onNavigateToProject = { projectId, status ->
                        navController.navigate("project_details?projectId=$projectId")
                    },"""

text = re.sub(target_home_nav, replacement_home_nav, text)

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(text)
