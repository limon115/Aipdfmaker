package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.database.AppDatabase
import com.example.ui.navigation.BottomNavItem
import com.example.ui.screens.blueprint.BlueprintSummaryScreen
import com.example.ui.screens.blueprint.BlueprintViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.processing.ProcessingScreen
import com.example.ui.screens.processing.ProcessingViewModel
import com.example.ui.screens.processing.TopicsDetectedScreen
import com.example.ui.screens.project.InputSourceScreen
import com.example.ui.screens.project.NewProjectViewModel
import com.example.ui.screens.project.PdfUploadScreen
import com.example.ui.screens.project.ProjectDetailsScreen
import com.example.ui.screens.settings.AiSettingsScreen
import com.example.ui.screens.settings.ApiLabScreen
import com.example.ui.screens.settings.ProviderSelectionScreen
import com.example.ui.screens.settings.SettingsViewModel

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Dashboard,
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
    
    val blueprintViewModel: BlueprintViewModel = viewModel()

    Scaffold(
        bottomBar = {
            if (currentRoute in items.map { it.route }) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(if (currentRoute == item.route) item.selectedIcon else item.unselectedIcon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
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
                    onNavigateToProject = { projectId, status ->
                        if (status == "Processing") {
                            navController.navigate("processing/$projectId")
                        } else {
                            navController.navigate("notes_viewer/$projectId")
                        }
                    },
                    onNavigateToNewProject = {
                        navController.navigate("input_source")
                    }
                )
            }
            composable(BottomNavItem.Dashboard.route) {
                
                Column(modifier = androidx.compose.ui.Modifier.fillMaxSize().padding(16.dp).padding(top = 48.dp)) {
                    com.example.ui.screens.settings.AiUsageDashboardCard()
                }
            }
            composable(BottomNavItem.Settings.route) {
                AiSettingsScreen(
                    onNavigateToProviderSelection = { isAi1 ->
                        navController.navigate("provider_selection/$isAi1")
                    },
                    onNavigateToApiLab = {
                        navController.navigate("api_lab")
                    }
                )
            }
            
            composable("api_lab") {
                ApiLabScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("input_source") {
                InputSourceScreen(
                    viewModel = newProjectViewModel,
                    onNavigate = { route -> navController.navigate(route) },
                    onNavigateNext = { navController.navigate("project_details") },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("youtube_link") {
                com.example.ui.screens.project.YouTubeLinkScreen(
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
                com.example.ui.screens.project.PasteTranscriptScreen(
                    viewModel = newProjectViewModel,
                    onNavigateNext = { navController.navigate("project_details") },
                    onNavigateBack = { navController.popBackStack() }
                )
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
                val processingViewModel: ProcessingViewModel = viewModel()
                ProcessingScreen(
                    viewModel = processingViewModel,
                    projectId = projectId,
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
                    blueprintViewModel = blueprintViewModel,
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
                        navController.navigate("note_generation/$projectId")
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("note_generation/{projectId}") { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId")?.toIntOrNull() ?: 1
                com.example.ui.screens.processing.NoteGenerationScreen(
                    projectId = projectId,
                    blueprintViewModel = blueprintViewModel,
                    onNavigateNext = {
                        navController.navigate("notes_viewer/$projectId")
                    },
                    onNavigateHome = {
                        navController.navigate(BottomNavItem.Home.route) {
                            popUpTo(BottomNavItem.Home.route) { inclusive = false }
                        }
                    }
                )
            }
            composable("notes_viewer/{projectId}") { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId")?.toIntOrNull() ?: 1
                val context = androidx.compose.ui.platform.LocalContext.current
                val db = com.example.data.database.AppDatabase.getDatabase(context)
                val htmlMergeEngine = com.example.domain.services.html.HtmlMergeEngine(db.htmlSnippetDao())
                val exportEngine = com.example.domain.services.export.ExportEngine(context)
                
                val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return com.example.ui.screens.viewer.NotesViewerViewModel(htmlMergeEngine, exportEngine, db.projectDao()) as T
                    }
                }
                val notesViewerViewModel: com.example.ui.screens.viewer.NotesViewerViewModel = 
                    androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                
                com.example.ui.screens.viewer.NotesViewerScreen(
                    viewModel = notesViewerViewModel,
                    projectId = projectId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateHome = { navController.navigate(BottomNavItem.Home.route) { popUpTo(BottomNavItem.Home.route) { inclusive = false } } }
                )
            }
            composable("provider_selection/{isAi1}") { backStackEntry ->
                val isAi1 = backStackEntry.arguments?.getString("isAi1")?.toBoolean() ?: true
                val settingsViewModel: SettingsViewModel = viewModel()
                val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
                
                ProviderSelectionScreen(
                    currentProvider = if (isAi1) settings?.ai1Provider ?: com.example.domain.models.AiProvider.GOOGLE_GEMINI else settings?.ai2Provider ?: com.example.domain.models.AiProvider.GOOGLE_GEMINI,
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
