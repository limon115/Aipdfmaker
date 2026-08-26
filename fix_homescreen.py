with open("app/src/main/java/com/example/ui/screens/home/HomeScreen.kt", "r") as f:
    content = f.read()

project_card_start = content.find("@Composable\nfun ProjectCard")
if project_card_start != -1:
    imports = content[:content.find("@OptIn(")]
    
    new_homescreen = """@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToNewProject: () -> Unit = {},
    onNavigateToProject: (Int, String) -> Unit = { _, _ -> }
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    
    HomeScreenContent(
        projects = projects,
        onNavigateToNewProject = onNavigateToNewProject,
        onNavigateToProject = onNavigateToProject,
        onDeleteProject = viewModel::deleteProject
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    projects: List<ProjectEntity>,
    onNavigateToNewProject: () -> Unit = {},
    onNavigateToProject: (Int, String) -> Unit = { _, _ -> },
    onDeleteProject: (ProjectEntity) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val tabs = listOf("All", "Completed", "Processing")
    val filteredProjects = remember(projects, selectedTab) {
        when (selectedTab) {
            1 -> projects.filter { it.status == "Completed" }
            2 -> projects.filter { it.status == "Processing" }
            else -> projects
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNewProject,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Project")
            }
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredProjects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project, 
                        onDelete = { onDeleteProject(it) },
                        onClick = { onNavigateToProject(project.id, project.status) }
                    )
                }
            }
        }
    }
}
"""
    
    with open("app/src/main/java/com/example/ui/screens/home/HomeScreen.kt", "w") as f:
        f.write(imports + new_homescreen + content[project_card_start:])
