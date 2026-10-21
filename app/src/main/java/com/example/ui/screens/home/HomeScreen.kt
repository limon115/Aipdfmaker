package com.example.ui.screens.home

import com.example.ui.components.glass.GlassBackground
import com.example.ui.components.glass.GlassCard
import com.example.ui.components.glass.GlassTextField
import com.example.ui.components.PdfIcon

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import com.example.utils.FileUtils
import java.io.File
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.ProjectEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToProject: (Int, String) -> Unit = { _, _ -> },
    onBottomReached: (Boolean) -> Unit = {}
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    
    HomeScreenContent(
        projects = projects,
        onNavigateToProject = onNavigateToProject,
        onDeleteProject = viewModel::deleteProject,
        onBottomReached = onBottomReached
    )
}

data class HomeTabItem(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val count: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    projects: List<ProjectEntity>,
    onNavigateToProject: (Int, String) -> Unit = { _, _ -> },
    onDeleteProject: (ProjectEntity) -> Unit = {},
    onBottomReached: (Boolean) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }
    var projectForPdfAction by remember { mutableStateOf<Pair<ProjectEntity, File>?>(null) }
    
    val allCount = projects.size
    val notesCount = remember(projects) { projects.count { it.status == "Completed" } }
    val debugCount = remember(projects) { projects.count { it.status == "Debugged" } }
    val tasksCount = remember(projects) { projects.count { it.status != "Completed" && it.status != "Debugged" } }

    val tabs = remember(allCount, notesCount, debugCount, tasksCount) {
        listOf(
            HomeTabItem("All", Icons.Default.GridView, allCount),
            HomeTabItem("Notes", Icons.Default.Description, notesCount),
            HomeTabItem("Debug", Icons.Default.Code, debugCount),
            HomeTabItem("Tasks", Icons.Default.CheckBox, tasksCount)
        )
    }

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf false

            val hasScrolled = listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
            if (!hasScrolled) return@derivedStateOf false

            if (!listState.canScrollForward) return@derivedStateOf true

            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf false
            lastVisible.index == totalItems - 1 && (lastVisible.offset + lastVisible.size <= layoutInfo.viewportEndOffset)
        }
    }

    LaunchedEffect(isAtBottom) {
        onBottomReached(isAtBottom)
    }

    LaunchedEffect(selectedTab, searchQuery) {
        onBottomReached(false)
    }

    LaunchedEffect(projects.isEmpty()) {
        if (projects.isEmpty()) {
            onBottomReached(false)
        }
    }

    if (projectForPdfAction != null) {
        val (actionProject, pdfFile) = projectForPdfAction!!
        AlertDialog(
            onDismissRequest = { projectForPdfAction = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = actionProject.title,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "A compiled PDF is available (${FileUtils.formatFileSize(pdfFile.length())}).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val targetPdf = pdfFile
                            val title = actionProject.title
                            projectForPdfAction = null
                            FileUtils.openPdf(context, targetPdf, "Open $title")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View PDF", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            val projId = actionProject.id
                            val projStatus = actionProject.status
                            projectForPdfAction = null
                            onNavigateToProject(projId, projStatus)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View LaTeX Notes")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { projectForPdfAction = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project?") },
            text = { Text("Are you sure you want to delete '${projectToDelete?.title}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        projectToDelete?.let { onDeleteProject(it) }
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    GlassBackground(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(
                        targetState = isSearchActive,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(200)) + slideInHorizontally { width -> width / 3 })
                                .togetherWith(fadeOut(animationSpec = tween(150)) + slideOutHorizontally { width -> -width / 3 })
                        },
                        label = "searchBarTransition"
                    ) { searchActive ->
                        if (searchActive) {
                            GlassTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search projects...") },
                                trailingIcon = if (searchQuery.isNotEmpty()) {
                                    {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                } else null,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                singleLine = true
                            )
                        } else {
                            Text(
                                text = "Projects",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { /* Menu placeholder */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) searchQuery = ""
                    }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearchActive) "Close Search" else "Search"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isDarkTheme = com.example.ui.theme.AppTheme.colors.isDark

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(percent = 50),
                color = if (isDarkTheme) Color(0xFF1E2235).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f),
                border = BorderStroke(
                    1.dp,
                    if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0)
                ),
                shadowElevation = if (isDarkTheme) 4.dp else 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(
                                    if (isSelected) {
                                        if (isDarkTheme) Color(0xFF1E3A8A).copy(alpha = 0.60f) else Color(0xFFDBEAFE)
                                    } else Color.Transparent
                                )
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                    selectedTab = index
                                }
                                .padding(vertical = 8.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    tint = if (isSelected) Color(0xFF2563EB) else if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isSelected) Color(0xFF2563EB) else if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFF2563EB)
                                            else if (isDarkTheme) Color(0xFF334155)
                                            else Color(0xFFE2E8F0)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tab.count.toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isSelected) Color.White else if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val slideDirection = if (targetState > initialState) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }
                    (slideIntoContainer(
                        towards = slideDirection,
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)))
                        .togetherWith(
                            slideOutOfContainer(
                                towards = slideDirection,
                                animationSpec = tween(200, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(180, easing = FastOutSlowInEasing))
                        )
                },
                label = "HomeScreenSectionContent"
            ) { currentTab ->
                val tabFiltered = remember(projects, currentTab, searchQuery) {
                    val baseFiltered = when (currentTab) {
                        1 -> projects.filter { it.status == "Completed" }
                        2 -> projects.filter { it.status == "Debugged" }
                        3 -> projects.filter { it.status != "Completed" && it.status != "Debugged" }
                        else -> projects
                    }
                    if (searchQuery.isBlank()) {
                        baseFiltered
                    } else {
                        baseFiltered.filter {
                            it.title.contains(searchQuery, ignoreCase = true) ||
                            it.course.contains(searchQuery, ignoreCase = true) ||
                            it.description.contains(searchQuery, ignoreCase = true)
                        }
                    }
                }

                if (tabFiltered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (searchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.Outlined.FolderOpen,
                                    contentDescription = "Empty Projects",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No Matching Projects Found" else "No Projects Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "Try searching for a different title or course keyword." else "Create a new project or solve a math problem to generate your AI notes and PDFs.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = tabFiltered,
                            key = { it.id },
                            contentType = { "project_card" }
                        ) { project ->
                            val compiledPdf = remember(project.id, project.lastUpdated) {
                                FileUtils.findCompiledPdf(context, project.title, project.lastUpdated)
                            }
                            ProjectCard(
                                project = project,
                                compiledPdf = compiledPdf,
                                onDelete = { projectToDelete = it },
                                onClick = {
                                    if (project.status == "Processing") {
                                        onNavigateToProject(project.id, project.status)
                                    } else if (compiledPdf != null && compiledPdf.exists()) {
                                        projectForPdfAction = Pair(project, compiledPdf)
                                    } else {
                                        onNavigateToProject(project.id, project.status)
                                    }
                                },
                                onViewPdf = { pdf ->
                                    FileUtils.openPdf(context, pdf, "Open ${project.title}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
}
@Composable
fun ProjectCard(
    project: ProjectEntity,
    compiledPdf: File? = null,
    onDelete: (ProjectEntity) -> Unit,
    onClick: () -> Unit,
    onViewPdf: (File) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image Placeholder
            PdfIcon(
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Text Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = project.course,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val daysAgo = ((System.currentTimeMillis() - project.lastUpdated) / (1000 * 60 * 60 * 24)).toInt()
                    val timeText = when (daysAgo) {
                        0 -> "Today"
                        1 -> "1d ago"
                        else -> "${daysAgo}d ago"
                    }
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val isDark = com.example.ui.theme.AppTheme.colors.isDark
                    val (statusBg, statusTextColor) = when (project.status) {
                        "Completed" -> if (isDark) Color(0xFF1B5E20).copy(alpha = 0.5f) to Color(0xFF81C784) else Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                        "Debugged" -> if (isDark) Color(0xFF4A148C).copy(alpha = 0.5f) to Color(0xFFCE93D8) else Color(0xFFF3E8FF) to Color(0xFF7E22CE)
                        else -> if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) to MaterialTheme.colorScheme.primary else Color(0xFFEFF6FF) to Color(0xFF2563EB)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusBg)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = project.status,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = statusTextColor
                        )
                    }
                    if (compiledPdf != null && compiledPdf.exists()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0xFF0C4A6E).copy(alpha = 0.5f) else Color(0xFFE0F2FE))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PDF",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
                            )
                        }
                    }
                }
            }

            // More Options
            val context = androidx.compose.ui.platform.LocalContext.current
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (compiledPdf != null && compiledPdf.exists()) {
                        DropdownMenuItem(
                            text = { Text("View PDF") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                expanded = false
                                onViewPdf(compiledPdf)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Copy Title") },
                        onClick = {
                            expanded = false
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Project Title", project.title)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "Title copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { 
                            expanded = false
                            onDelete(project)
                        }
                    )
                }
            }
        }
    }
}
