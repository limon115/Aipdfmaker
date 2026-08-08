import sys

with open('/app/applet/app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'r') as f:
    screen = f.read()

dashboard_composable_old = """
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

dashboard_composable_new = """
@Composable
fun AiUsageDashboardCard() {
    val stats by com.example.domain.services.ai.AiUsageTracker.stats.collectAsStateWithLifecycle()
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
        
        UsageBarChartCard(stats.requests, stats.cacheHits, stats.rateLimitErrors)
    }
}

@Composable
fun UsageBarChartCard(requests: Int, cacheHits: Int, rateLimits: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Usage Breakdown",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            val maxVal = maxOf(requests, cacheHits, rateLimits, 1) // Avoid div by zero
            
            val primaryColor = MaterialTheme.colorScheme.primary
            val successColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
            val errorColor = MaterialTheme.colorScheme.error
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ChartBarRow("Requests", requests, maxVal, primaryColor)
                ChartBarRow("Cache Hits", cacheHits, maxVal, successColor)
                ChartBarRow("Rate Limits", rateLimits, maxVal, errorColor)
            }
        }
    }
}

@Composable
fun ChartBarRow(label: String, value: Int, maxVal: Int, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(80.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .androidx.compose.foundation.background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (value.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f))
                    .androidx.compose.foundation.background(color, RoundedCornerShape(4.dp))
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = if (value > 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp).align(Alignment.CenterStart)
            )
        }
    }
}
"""

screen = screen.replace(dashboard_composable_old, dashboard_composable_new)

with open('/app/applet/app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'w') as f:
    f.write(screen)
