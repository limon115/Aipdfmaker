import re

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

dashboard_content = """
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
                        modifier = Modifier.fillMaxWidth().clickable { navController.navigate("latex_debugger") }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BugReport, contentDescription = "LaTeX Debugger", tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("LaTeX Debugger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Debug and fix LaTeX compilation errors using AI.", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(100.dp))
                }
            }
"""

content = re.sub(r'composable\(BottomNavItem\.Dashboard\.route\) \{[\s\S]*?androidx\.compose\.foundation\.layout\.Spacer\(modifier = Modifier\.height\(100\.dp\)\)\n\s*\}\n\s*\}', dashboard_content.strip(), content)

debugger_composable = """
            composable("latex_debugger") {
                com.example.ui.screens.debugger.LatexDebuggerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToViewer = { projectId -> 
                        navController.navigate("notes_viewer/$projectId")
                    }
                )
            }
"""

content = content.replace('composable(BottomNavItem.Dashboard.route)', debugger_composable.strip() + '\n            composable(BottomNavItem.Dashboard.route)')

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)
