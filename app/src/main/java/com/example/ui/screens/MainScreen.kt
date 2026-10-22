package com.example.ui.screens

import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Forum
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
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
    val db = remember { AppDatabase.getDatabase(context) }
    val projectDao = remember(db) { db.projectDao() }
    
    val homeViewModelFactory = remember(projectDao) {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(projectDao) as T
            }
        }
    }
    val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)
    
    val newProjectViewModelFactory = remember(projectDao) {
        object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return NewProjectViewModel(projectDao) as T
            }
        }
    }
    val newProjectViewModel: NewProjectViewModel = viewModel(factory = newProjectViewModelFactory)
    
    val blueprintViewModel: BlueprintViewModel = viewModel()

    val onNavigate: (String) -> Unit = remember(navController) {
        { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val onNavigateToNewProject: () -> Unit = remember(navController) {
        {
            navController.navigate("input_source")
        }
    }

    var isHomeAtBottom by remember { mutableStateOf(false) }
    var isDashboardAtBottom by remember { mutableStateOf(false) }
    var isSettingsAtBottom by remember { mutableStateOf(false) }
    var isNestedAtBottom by remember { mutableStateOf(false) }

    LaunchedEffect(currentRoute) {
        isNestedAtBottom = false
        isHomeAtBottom = false
        isDashboardAtBottom = false
        isSettingsAtBottom = false
    }

    val isBottomReached = when (currentRoute) {
        BottomNavItem.Home.route -> isHomeAtBottom || isNestedAtBottom
        BottomNavItem.Dashboard.route -> isDashboardAtBottom || isNestedAtBottom
        BottomNavItem.Settings.route -> isSettingsAtBottom || isNestedAtBottom
        else -> isNestedAtBottom
    }

    MainScreenContent(
        navController = navController,
        currentRoute = currentRoute,
        onNavigate = onNavigate,
        onNavigateToNewProject = onNavigateToNewProject,
        isBottomReached = isBottomReached,
        onNestedBottomReached = { atBottom -> isNestedAtBottom = atBottom }
    ) { innerPadding ->
        val bottomNavRoutes = remember {
            listOf(
                BottomNavItem.Home.route,
                BottomNavItem.Dashboard.route,
                BottomNavItem.Settings.route
            )
        }

        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                val fromIdx = bottomNavRoutes.indexOf(initialState.destination.route)
                val toIdx = bottomNavRoutes.indexOf(targetState.destination.route)
                if (fromIdx != -1 && toIdx != -1) {
                    fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))
                } else {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing))
                }
            },
            exitTransition = {
                val fromIdx = bottomNavRoutes.indexOf(initialState.destination.route)
                val toIdx = bottomNavRoutes.indexOf(targetState.destination.route)
                if (fromIdx != -1 && toIdx != -1) {
                    fadeOut(animationSpec = tween(140, easing = FastOutLinearInEasing))
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> (-fullWidth * 0.15f).toInt() },
                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing))
                }
            },
            popEnterTransition = {
                val fromIdx = bottomNavRoutes.indexOf(initialState.destination.route)
                val toIdx = bottomNavRoutes.indexOf(targetState.destination.route)
                if (fromIdx != -1 && toIdx != -1) {
                    fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))
                } else {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> (-fullWidth * 0.15f).toInt() },
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing))
                }
            },
            popExitTransition = {
                val fromIdx = bottomNavRoutes.indexOf(initialState.destination.route)
                val toIdx = bottomNavRoutes.indexOf(targetState.destination.route)
                if (fromIdx != -1 && toIdx != -1) {
                    fadeOut(animationSpec = tween(140, easing = FastOutLinearInEasing))
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
                        animationSpec = tween(200, easing = FastOutLinearInEasing)
                    ) + fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing))
                }
            }
        ) {
            composable(BottomNavItem.Home.route) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToProject = { projectId, status ->
                        if (status == "Processing") {
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val workManager = androidx.work.WorkManager.getInstance(context)
                                val workInfos = workManager.getWorkInfosForUniqueWork("NoteGen_${projectId}").get()
                                val isGenerating = workInfos.isNotEmpty()
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
                    onBottomReached = { atBottom -> isHomeAtBottom = atBottom }
                )
            }
            composable(BottomNavItem.Chat.route) {
                com.example.ui.screens.chat.LatexChatScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("latex_debugger") {
                com.example.ui.screens.debugger.LatexDebuggerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToViewer = { projectId -> 
                        navController.navigate("notes_viewer/$projectId")
                    }
                )
            }
            composable(BottomNavItem.Dashboard.route) {
                val dashboardScrollState = androidx.compose.foundation.rememberScrollState()
                val isDashboardAtEnd by remember {
                    derivedStateOf {
                        dashboardScrollState.value > 0 && 
                            dashboardScrollState.maxValue > 0 && 
                            (dashboardScrollState.value >= dashboardScrollState.maxValue - 20 || !dashboardScrollState.canScrollForward)
                    }
                }
                LaunchedEffect(isDashboardAtEnd) {
                    isDashboardAtBottom = isDashboardAtEnd
                }
                com.example.ui.components.glass.GlassBackground(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxSize()
                            .verticalScroll(dashboardScrollState)
                            .padding(start = 16.dp, end = 16.dp, top = 48.dp)
                    ) {
                    com.example.ui.screens.settings.AiUsageDashboardCard()
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    
                    com.example.ui.components.glass.GlassCard(
                        onClick = { navController.navigate(BottomNavItem.Chat.route) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        androidx.compose.foundation.layout.Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(androidx.compose.material.icons.Icons.Default.Forum, contentDescription = "LaTeX AI ChatBot", tint = MaterialTheme.colorScheme.primary)
                            androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
                            Column {
                                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("LaTeX AI ChatBot", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                    androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            "NEW",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("Ask for custom LaTeX documents and compile to PDF in real-time.", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    
                    com.example.ui.components.glass.GlassCard(
                        onClick = { navController.navigate("math_solver") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        androidx.compose.foundation.layout.Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(androidx.compose.material.icons.Icons.Default.Build, contentDescription = "Math Solver", tint = MaterialTheme.colorScheme.primary)
                            androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
                            Column {
                                Text("AI Math Solver", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text("Generate step-by-step PDF solutions.", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))

                    
                    com.example.ui.components.glass.GlassCard(
                        onClick = { navController.navigate("latex_debugger") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        androidx.compose.foundation.layout.Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(androidx.compose.material.icons.Icons.Default.Build, contentDescription = "LaTeX Debugger", tint = MaterialTheme.colorScheme.primary)
                            androidx.compose.foundation.layout.Spacer(Modifier.width(16.dp))
                            Column {
                                Text("LaTeX Debugger", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text("Debug and fix LaTeX compilation errors using AI.", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(100.dp))
                }
                }
            }
            composable(BottomNavItem.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel()
                AiSettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToTheme = { navController.navigate("settings_theme") },
                    onNavigateToFonts = { navController.navigate("settings_fonts") },
                    onNavigateToAiConfig = { aiIndex -> navController.navigate("settings_ai/$aiIndex") },
                    onNavigateToAdvanced = { navController.navigate("settings_advanced") },
                    onNavigateToBackground = { navController.navigate("settings_background") },
                    onNavigateToUpdates = { navController.navigate("settings_updates") },
                    onNavigateToApiLab = { navController.navigate("api_lab") },
                    onNavigateToLogs = { navController.navigate("app_logs") },
                    onNavigateToPrompts = { navController.navigate("settings_prompts") },
                    onNavigateToProviderSelection = { aiIndex ->
                        navController.navigate("provider_selection/$aiIndex")
                    },
                    onBottomReached = { atBottom -> isSettingsAtBottom = atBottom }
                )
            }
            
            composable("settings_prompts") {
                val settingsViewModel: SettingsViewModel = viewModel()
                com.example.ui.screens.settings.subscreens.AiPromptsConfigScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable("settings_theme") {
                val settingsViewModel: SettingsViewModel = viewModel()
                com.example.ui.screens.settings.subscreens.AppThemeScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings_fonts") {
                val settingsViewModel: SettingsViewModel = viewModel()
                com.example.ui.screens.settings.subscreens.AppFontsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings_ai/{aiIndex}") { backStackEntry ->
                val aiIndex = backStackEntry.arguments?.getString("aiIndex")?.toIntOrNull() ?: 1
                val settingsViewModel: SettingsViewModel = viewModel()
                com.example.ui.screens.settings.subscreens.AiAssistantConfigScreen(
                    aiIndex = aiIndex,
                    viewModel = settingsViewModel,
                    onNavigateToProviderSelection = { slotId ->
                        navController.navigate("provider_selection/$slotId")
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings_advanced") {
                val settingsViewModel: SettingsViewModel = viewModel()
                com.example.ui.screens.settings.subscreens.AdvancedSettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings_background") {
                com.example.ui.screens.settings.subscreens.BackgroundProcessingScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings_updates") {
                val settingsViewModel: SettingsViewModel = viewModel()
                com.example.ui.screens.settings.subscreens.AppUpdatesScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
                        composable("math_solver") {
                com.example.ui.screens.math.MathSolverScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            
            composable("app_logs") {
                com.example.ui.screens.settings.AppLogsScreen(
                    onNavigateBack = { navController.popBackStack() }
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
                com.example.ui.components.glass.GlassBackground(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Import from File (Placeholder)")
                    }
                }
            }
            composable(
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
            }
            composable("processing/{projectId}") { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId")?.toIntOrNull() ?: 1
                val processingViewModel: ProcessingViewModel = viewModel()
                ProcessingScreen(
                    viewModel = processingViewModel,
                    projectId = projectId,
                    onNavigateBack = { navController.popBackStack() },
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
            composable("note_generation/{projectId}", deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "docmorph://note_generation/{projectId}" })) { backStackEntry ->
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
            composable("notes_viewer/{projectId}", deepLinks = listOf(androidx.navigation.navDeepLink { uriPattern = "docmorph://notes_viewer/{projectId}" })) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId")?.toIntOrNull() ?: 1
                val context = androidx.compose.ui.platform.LocalContext.current
                val db = remember(context) { com.example.data.database.AppDatabase.getDatabase(context) }
                val compilerRepository = remember(context) { com.example.domain.repository.LatexCompilerRepository(context) }
                
                val factory = remember(db, compilerRepository) {
                    object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return com.example.ui.screens.viewer.NotesViewerViewModel(db.documentSnippetDao(), compilerRepository, db.projectDao()) as T
                        }
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
            composable("provider_selection/{aiIndex}") { backStackEntry ->
                val aiIndex = backStackEntry.arguments?.getString("aiIndex")?.toIntOrNull() ?: 1
                val settingsViewModel: SettingsViewModel = viewModel()
                val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
                
                ProviderSelectionScreen(
                    currentProvider = when (aiIndex) {
                        1 -> settings?.ai1Provider ?: com.example.domain.models.AiProvider.GOOGLE_GEMINI
                        2 -> settings?.ai2Provider ?: com.example.domain.models.AiProvider.GOOGLE_GEMINI
                        else -> settings?.ai3Provider ?: com.example.domain.models.AiProvider.GOOGLE_GEMINI
                    },
                    onProviderSelected = { provider ->
                        when (aiIndex) {
                            1 -> settingsViewModel.updateAi1Provider(provider)
                            2 -> settingsViewModel.updateAi2Provider(provider)
                            else -> settingsViewModel.updateAi3Provider(provider)
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
fun MainScreenContent(
    navController: androidx.navigation.NavHostController,
    currentRoute: String? = null,
    onNavigate: (String) -> Unit,
    onNavigateToNewProject: () -> Unit = {},
    isBottomReached: Boolean = false,
    onNestedBottomReached: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val activeRoute = navBackStackEntry?.destination?.route ?: currentRoute

    val items = remember {
        listOf(
            BottomNavItem.Home,
            BottomNavItem.Dashboard,
            BottomNavItem.Settings
        )
    }

    val bottomNavRoutes = remember(items) { items.map { it.route }.toSet() }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y < -0.5f) {
                    onNestedBottomReached(true)
                } else if (consumed.y > 0.5f || available.y > 0.5f) {
                    onNestedBottomReached(false)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y < -1f) {
                    onNestedBottomReached(true)
                } else if (consumed.y > 1f || available.y > 1f) {
                    onNestedBottomReached(false)
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(nestedScrollConnection),
        containerColor = Color.Transparent,
        bottomBar = {
            val isBottomBarVisible = (activeRoute in bottomNavRoutes) && 
                activeRoute != BottomNavItem.Chat.route && 
                activeRoute != "latex_chat" &&
                !isBottomReached
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(180, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(140)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(140, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(100))
            ) {
                val isDark = com.example.ui.theme.AppTheme.colors.isDark
                val capsuleShape = remember { RoundedCornerShape(percent = 50) }

                val bgGradient = remember(isDark) {
                    Brush.linearGradient(
                        colors = listOf(
                            if (isDark) Color(0xFF161924).copy(alpha = 0.85f)
                            else Color.White.copy(alpha = 0.88f),
                            if (isDark) Color(0xFF10121A).copy(alpha = 0.75f)
                            else Color.White.copy(alpha = 0.80f)
                        )
                    )
                }

                val borderGradient = remember(isDark) {
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.40f else 0.80f),
                            Color.White.copy(alpha = if (isDark) 0.08f else 0.20f),
                            Color.White.copy(alpha = if (isDark) 0.25f else 0.50f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(400f, 400f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    // 1. Floating Capsule Navigation Container with Glassmorphism (Home First)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .shadow(
                                elevation = if (isDark) 12.dp else 8.dp,
                                shape = capsuleShape,
                                spotColor = if (isDark) Color.Black.copy(alpha = 0.45f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                ambientColor = if (isDark) Color.Black.copy(alpha = 0.25f)
                                else Color.Black.copy(alpha = 0.05f)
                            )
                            .clip(capsuleShape)
                            .background(bgGradient)
                            .border(1.dp, borderGradient, capsuleShape)
                    ) {
                        // Home is FIRST on the left, Dashboard in middle, Settings on the right
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items.forEach { item ->
                                val isSelected = activeRoute == item.route
                                CapsuleNavItem(
                                    item = item,
                                    selected = isSelected,
                                    onClick = { onNavigate(item.route) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 2. ChatBot circular button placed outside the nav bar, alongside the plus button with similar circular shape
                    Surface(
                        onClick = { onNavigate(BottomNavItem.Chat.route) },
                        shape = CircleShape,
                        color = if (isDark) Color(0xFF262A40) else Color(0xFFEDE9FE),
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = BottomNavItem.Chat.selectedIcon,
                                contentDescription = "ChatBot",
                                tint = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // 3. Separate circular Floating Action Button (FAB) immediately to the right
                    FloatingActionButton(
                        onClick = onNavigateToNewProject,
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 10.dp
                        ),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Project",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}

@Composable
fun CenteredText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun CapsuleNavItem(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "navIconScale"
    )
    val translationY by animateFloatAsState(
        targetValue = if (selected) -1.5f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "navIconTranslationY"
    )
    val alpha by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.65f,
        animationSpec = tween(durationMillis = 150),
        label = "navItemAlpha"
    )

    val contentColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.88f),
            exit = fadeOut(animationSpec = tween(120)) + scaleOut(targetScale = 0.88f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 6.dp, horizontal = 4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.translationY = translationY
                    this.alpha = alpha
                },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.title,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}