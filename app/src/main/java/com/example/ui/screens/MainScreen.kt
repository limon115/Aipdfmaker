package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.BottomNavItem
import com.example.ui.screens.blueprint.BlueprintSummaryScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.processing.ProcessingScreen
import com.example.ui.screens.processing.TopicsDetectedScreen
import com.example.ui.screens.project.InputSourceScreen
import com.example.ui.screens.project.NewProjectViewModel
import com.example.ui.screens.project.PdfUploadScreen
import com.example.ui.screens.project.ProjectDetailsScreen
import com.example.ui.screens.project.YouTubeInputScreen
import com.example.ui.screens.settings.AiSettingsScreen
import com.example.ui.screens.settings.ProviderSelectionScreen
import com.example.ui.screens.settings.SettingsViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.AppDatabase
import com.example.ui.screens.home.HomeViewModel

import com.example.ui.screens.processing.ProcessingViewModel

@Composable
fun MainScreen(navController: NavHostController = rememberNavController()) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Bookmarks,
        BottomNavItem.Settings
    )

    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val projectDao = db.projectDao()

    val homeViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(projectDao) as T
        }
    }
    val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)

    val newProjectViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return NewProjectViewModel(projectDao) as T
        }
    }
    val newProjectViewModel: NewProjectViewModel = viewModel(factory = newProjectViewModelFactory)

    val blueprintViewModel: com.example.ui.screens.blueprint.BlueprintViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(text = item.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToNewProject = {
                        navController.navigate("input_source")
                    }
                )
            }
            composable(BottomNavItem.Bookmarks.route) {
                CenteredText("Bookmarks Screen")
            }
            composable(BottomNavItem.Settings.route) {
                AiSettingsScreen(
                    onNavigateToProviderSelection = { isAi1 ->
                        navController.navigate("provider_selection/$isAi1")
                    }
                )
            }
            composable("input_source") {
                InputSourceScreen(
                    onNavigate = { route -> navController.navigate(route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("youtube_input") {
                YouTubeInputScreen(
                    viewModel = newProjectViewModel,
                    onNavigateNext = { navController.navigate("project_details") },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("pdf_input") {
                PdfUploadScreen(
                    viewModel = newProjectViewModel,
                    onNavigateNext = { navController.navigate("project_details") },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("paste_input") {
                CenteredText("Paste Transcript (Placeholder)")
            }
            composable("file_input") {
                CenteredText("Import from File (Placeholder)")
            }
            composable("project_details") {
                ProjectDetailsScreen(
                    viewModel = newProjectViewModel,
                    onCreateProject = { projectId ->
                        navController.navigate("processing/$projectId")
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("processing/{projectId}") { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId")?.toIntOrNull() ?: 1
                val state by newProjectViewModel.state.collectAsStateWithLifecycle()
                val processingViewModel: ProcessingViewModel = viewModel()
                ProcessingScreen(
                    viewModel = processingViewModel,
                    extractedText = state.extractedText,
                    onProcessingFinished = { summary ->
                        if (summary != null) {
                            blueprintViewModel.setBlueprintSummary(summary)
                        }
                        navController.navigate("topics_detected/$projectId") {
                            popUpTo("processing/$projectId") { inclusive = true }
                        }
                    }
                )
            }
            composable("topics_detected/{projectId}") { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId")?.toIntOrNull() ?: 1
                TopicsDetectedScreen(
                    onNavigateNext = {
                        navController.navigate("blueprint_summary/$projectId")
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("blueprint_summary/{projectId}") { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId")?.toIntOrNull() ?: 1
                BlueprintSummaryScreen(
                    viewModel = blueprintViewModel,
                    onNavigateNext = {
                        navController.navigate("notes_viewer/$projectId")
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("notes_viewer/{projectId}") { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId")?.toIntOrNull() ?: 1
                val context = androidx.compose.ui.platform.LocalContext.current
                val db = androidx.room.Room.databaseBuilder(
                    context,
                    com.example.data.database.AppDatabase::class.java, "docmorph-db"
                ).build()
                val htmlMergeEngine = com.example.domain.services.html.HtmlMergeEngine(db.projectDao())
                val exportEngine = com.example.domain.services.export.ExportEngine(context)
                
                val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return com.example.ui.screens.viewer.NotesViewerViewModel(htmlMergeEngine, exportEngine) as T
                    }
                }
                val notesViewerViewModel: com.example.ui.screens.viewer.NotesViewerViewModel = 
                    androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                
                com.example.ui.screens.viewer.NotesViewerScreen(
                    viewModel = notesViewerViewModel,
                    projectId = projectId,
                    projectName = "Project",
                    outputFormat = "pdf",
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("provider_selection/{isAi1}") { backStackEntry ->
                val isAi1 = backStackEntry.arguments?.getString("isAi1")?.toBoolean() ?: true
                val settingsViewModel: SettingsViewModel = viewModel()
                val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
                
                ProviderSelectionScreen(
                    currentProvider = if (isAi1) settings.ai1Provider else settings.ai2Provider,
                    onProviderSelected = { provider ->
                        if (isAi1) {
                            settingsViewModel.updateAi1Provider(provider)
                        } else {
                            settingsViewModel.updateAi2Provider(provider)
                        }
                        navController.popBackStack()
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun CenteredText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.titleLarge)
    }
}
