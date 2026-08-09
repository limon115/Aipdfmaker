import re

with open("app/src/main/java/com/example/ui/screens/project/ProjectDetailsScreen.kt", "r") as f:
    text = f.read()

target = r"Spacer\(modifier = Modifier\.weight\(1f\)\)\s*Spacer\(modifier = Modifier\.height\(16\.dp\)\)\s*Button\(\s*onClick = \{\s*viewModel\.createProject \{ id ->\s*onCreateProject\(id\)\s*\}\s*\},\s*modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.height\(56\.dp\),\s*shape = RoundedCornerShape\(16\.dp\),\s*colors = ButtonDefaults\.buttonColors\(\s*containerColor = MaterialTheme\.colorScheme\.primary\s*\)\s*\) \{\s*Text\(\"Create Project\", style = MaterialTheme\.typography\.titleMedium\)\s*\}\s*\}\s*\}"

replacement = """Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))
            
            if (projectId != null) {
                if (documentExists) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Current Content Available", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(documentContent.take(200) + if(documentContent.length > 200) "..." else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Button(
                        onClick = { onNavigateToViewer(projectId) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("View Content", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Button(
                        onClick = { onNavigateToProgress(projectId) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("View Progress", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                Button(
                    onClick = {
                        viewModel.createProject { id ->
                            onCreateProject(id)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Create Project", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }"""

text = re.sub(target, replacement, text)

with open("app/src/main/java/com/example/ui/screens/project/ProjectDetailsScreen.kt", "w") as f:
    f.write(text)
