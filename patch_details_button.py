with open("app/src/main/java/com/example/ui/screens/project/ProjectDetailsScreen.kt", "r") as f:
    text = f.read()

target = """            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.createProject { id ->
                        onCreateProject(id)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Create Project", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}"""

replacement = """            Spacer(modifier = Modifier.weight(1f))
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
    }
}"""

if target in text:
    text = text.replace(target, replacement)
else:
    print("TARGET NOT FOUND!")

with open("app/src/main/java/com/example/ui/screens/project/ProjectDetailsScreen.kt", "w") as f:
    f.write(text)
