import sys

with open('/app/applet/app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'r') as f:
    screen = f.read()

dashboard_composable = """
@Composable
fun AiUsageDashboardCard() {
    val stats by com.example.domain.services.ai.AiUsageTracker.stats.collectAsStateWithLifecycle()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Session AI Usage",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Requests Made:", style = MaterialTheme.typography.bodyMedium)
                Text("${stats.requests}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Est. Tokens Used:", style = MaterialTheme.typography.bodyMedium)
                Text("${stats.estimatedTokens}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cache Hits (Saved Tokens):", style = MaterialTheme.typography.bodyMedium)
                Text("${stats.cacheHits}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Rate Limit (429) Errors:", style = MaterialTheme.typography.bodyMedium)
                Text("${stats.rateLimitErrors}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (stats.rateLimitErrors > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
"""

if "AiUsageDashboardCard" not in screen:
    screen = screen + dashboard_composable

# Inject it at the top of the column in AiSettingsScreen
old_column = """            Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {"""

new_column = """            Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AiUsageDashboardCard()"""

screen = screen.replace(old_column, new_column)

with open('/app/applet/app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'w') as f:
    f.write(screen)
