import sys

with open('/app/applet/app/src/main/java/com/example/ui/screens/processing/NoteGenerationScreen.kt', 'r') as f:
    screen = f.read()

# Add import for lifecycle
if "import androidx.lifecycle.compose.collectAsStateWithLifecycle" not in screen:
    screen = screen.replace("import androidx.lifecycle.viewmodel.compose.viewModel", "import androidx.lifecycle.viewmodel.compose.viewModel\nimport androidx.lifecycle.compose.collectAsStateWithLifecycle")

old_imports = "import com.example.ui.screens.blueprint.BlueprintViewModel"
new_imports = "import com.example.ui.screens.blueprint.BlueprintViewModel\nimport com.example.domain.services.ai.AiUsageTracker"
screen = screen.replace(old_imports, new_imports)

old_ui_start = """            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {"""

new_ui_start = """            val aiStats by AiUsageTracker.stats.collectAsStateWithLifecycle()
            
            if (aiStats.requests > 0 || aiStats.estimatedTokens > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Live AI Usage", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Requests", style = MaterialTheme.typography.bodySmall)
                            Text("${aiStats.requests}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Estimated Tokens", style = MaterialTheme.typography.bodySmall)
                            Text("${aiStats.estimatedTokens}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        if (aiStats.rateLimitErrors > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Rate Limits Hit", style = MaterialTheme.typography.bodySmall)
                                Text("${aiStats.rateLimitErrors} (Auto-retrying)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {"""

screen = screen.replace(old_ui_start, new_ui_start)

with open('/app/applet/app/src/main/java/com/example/ui/screens/processing/NoteGenerationScreen.kt', 'w') as f:
    f.write(screen)
