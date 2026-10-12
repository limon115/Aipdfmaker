package com.example.ui.screens

import androidx.compose.material.icons.filled.Build
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.blur
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
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
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(projectDao) as T
        }
    }
    val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)
    
    val newProjectViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return NewProjectViewModel(projectDao) as T
        }
    }
    val newProjectViewModel: NewProjectViewModel = viewModel(factory = newProjectViewModelFactory)
    
    val blueprintViewModel: BlueprintViewModel = viewModel()

    MainScreenContent(
        navController = navController,
        currentRoute = currentRoute,
        onNavigate = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        onNavigateToNewProject = {
            navController.navigate("input_source")
        }
    ) { innerPadding ->
        val bottomNavRoutes = listOf(
            BottomNavItem.Home.route,
            BottomNavItem.Dashboard.route,
            BottomNavItem.Settings.route
        )

        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                val fromIdx = bottomNavRoutes.indexOf(initialState.destination.route)
                val toIdx = bottomNavRoutes.indexOf(targetState.destination.route)
                if (fromIdx != -1 && toIdx != -1) {
                    if (toIdx > fromIdx) {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth / 3 },
                            animationSpec = tween(240, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing))
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth / 3 },
                            animationSpec = tween(240, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing))
                    }
                } else {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth / 4 },
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing))
                }
            },
            exitTransition = {
                val fromIdx = bottomNavRoutes.indexOf(initialState.destination.route)
                val toIdx = bottomNavRoutes.indexOf(targetState.destination.route)
                if (fromIdx != -1 && toIdx != -1) {
                    if (toIdx > fromIdx) {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 3 },
                            animationSpec = tween(240, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing), targetAlpha = 0.2f)
                    } else {
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth / 3 },
                            animationSpec = tween(240, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing), targetAlpha = 0.2f)
                    }
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth / 6 },
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing), targetAlpha = 0.2f)
                }
            },
            popEnterTransition = {
                val fromIdx = bottomNavRoutes.indexOf(initialState.destination.route)
                val toIdx = bottomNavRoutes.indexOf(targetState.destination.route)
                if (fromIdx != -1 && toIdx != -1) {
                    if (toIdx > fromIdx) {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth / 3 },
                            animationSpec = tween(240, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing))
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth / 3 },
                            animationSpec = tween(240, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing))
                    }
                } else {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth / 6 },
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing))
                }
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth / 4 },
                    animationSpec = tween(240, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing), targetAlpha = 0.2f)
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
                    }
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
                Column(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxSize()
                        .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 48.dp)
                ) {
                                        com.example.ui.screens.settings.AiUsageDashboardCard()
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    
                    com.example.ui.components.glass.GlassCard(
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("math_solver") }
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
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("latex_debugger") }
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
            composable(BottomNavItem.Settings.route) {
                AiSettingsScreen(
                    onNavigateToProviderSelection = { aiIndex ->
                        navController.navigate("provider_selection/$aiIndex")
                    },
                    onNavigateToApiLab = {
                        navController.navigate("api_lab")
                    },
                    onNavigateToLogs = {
                        navController.navigate("app_logs")
                    }
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Import from File (Placeholder)")
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
                val db = com.example.data.database.AppDatabase.getDatabase(context)
                
                val compilerRepository = com.example.domain.repository.LatexCompilerRepository(context)
                
                val factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return com.example.ui.screens.viewer.NotesViewerViewModel(db.documentSnippetDao(), compilerRepository, db.projectDao()) as T
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
    modifier: Modifier = Modifier,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val activeRoute = navBackStackEntry?.destination?.route ?: currentRoute

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Dashboard,
        BottomNavItem.Settings
    )

    com.example.ui.components.glass.LiquidBackground {
        Scaffold(
            modifier = modifier,
            containerColor = Color.Transparent,
            bottomBar = {
                val isBottomBarVisible = activeRoute in items.map { it.route }
                AnimatedVisibility(
                    visible = isBottomBarVisible,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(200)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(180, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(150))
                ) {
                    val isDark = com.example.ui.theme.AppTheme.colors.isDark
                    val capsuleShape = RoundedCornerShape(percent = 50)

                    val bgGradient = Brush.linearGradient(
                        colors = listOf(
                            if (isDark) Color(0xFF161924).copy(alpha = 0.70f)
                            else Color.White.copy(alpha = 0.75f),
                            if (isDark) Color(0xFF10121A).copy(alpha = 0.55f)
                            else Color.White.copy(alpha = 0.65f)
                        )
                    )

                    val borderGradient = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isDark) 0.40f else 0.80f),
                            Color.White.copy(alpha = if (isDark) 0.08f else 0.20f),
                            Color.White.copy(alpha = if (isDark) 0.25f else 0.50f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(400f, 400f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                    ) {
                        // 1. Floating Capsule Navigation Container with Glassmorphism
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
                                .border(1.dp, borderGradient, capsuleShape)
                        ) {
                            // Liquid glass background with semi-transparent color and background blur
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .blur(radius = 16.dp)
                                    .background(bgGradient)
                            )

                            // Right-to-Left (RTL) Navigation Items: Home -> Dashboard -> Settings
                            // Home is on the far RIGHT, Dashboard in the middle, Settings on the LEFT
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val rtlItems = listOf(
                                        BottomNavItem.Home,
                                        BottomNavItem.Dashboard,
                                        BottomNavItem.Settings
                                    )
                                    rtlItems.forEach { item ->
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
                        }

                        // 2. Separate circular Floating Action Button (FAB) immediately to the right of the main capsule
                        val isHomeActive = navBackStackEntry?.destination?.route == BottomNavItem.Home.route ||
                                activeRoute == BottomNavItem.Home.route
                        AnimatedVisibility(
                            visible = isHomeActive,
                            enter = fadeIn(animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing))
                        ) {
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
            }
        ) { innerPadding ->
            content(innerPadding)
        }
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
        targetValue = if (selected) 1.15f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navIconScale"
    )
    val translationY by animateFloatAsState(
        targetValue = if (selected) -2f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "navIconTranslationY"
    )
    val alpha by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.65f,
        animationSpec = tween(durationMillis = 200),
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
        if (selected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 6.dp, horizontal = 4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
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
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1
                )
            }
        }
    }
}