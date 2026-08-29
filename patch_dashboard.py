import re

with open('app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'r') as f:
    content = f.read()

dashboard_code = """
@Composable
fun AiUsageDashboardCard() {
    val stats by com.example.domain.services.ai.AiUsageTracker.stats.collectAsStateWithLifecycle()
    
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        com.example.ui.components.glass.GlassCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
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
                    Text("Total Requests:", style = MaterialTheme.typography.bodyMedium)
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
        
        if (stats.tokensByFeature.isNotEmpty()) {
            UsagePieChartCard(stats.tokensByFeature, "Token Distribution")
        }
        
        if (stats.requestsByFeature.isNotEmpty()) {
            FeatureBarChartCard(stats.requestsByFeature, "Requests by Feature")
        }
        
        UsageBarChartCard(stats.requests, stats.cacheHits, stats.rateLimitErrors)
    }
}

@Composable
fun UsagePieChartCard(data: Map<String, Int>, title: String) {
    com.example.ui.components.glass.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            val total = data.values.sum().coerceAtLeast(1)
            val colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary,
                MaterialTheme.colorScheme.error,
                androidx.compose.ui.graphics.Color(0xFF4CAF50),
                androidx.compose.ui.graphics.Color(0xFFFF9800)
            )
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Pie Chart Canvas
                androidx.compose.foundation.Canvas(modifier = Modifier.size(120.dp).padding(8.dp)) {
                    var startAngle = -90f
                    data.entries.forEachIndexed { index, entry ->
                        val sweepAngle = (entry.value.toFloat() / total) * 360f
                        drawArc(
                            color = colors[index % colors.size],
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            size = size
                        )
                        startAngle += sweepAngle
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.entries.forEachIndexed { index, entry ->
                        val percentage = (entry.value.toFloat() / total) * 100
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).background(colors[index % colors.size], androidx.compose.foundation.shape.CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(entry.key, style = MaterialTheme.typography.labelMedium)
                                Text("${entry.value} (${String.format("%.1f", percentage)}%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeatureBarChartCard(data: Map<String, Int>, title: String) {
    com.example.ui.components.glass.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            val maxVal = data.values.maxOrNull()?.coerceAtLeast(1) ?: 1
            val colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary,
                androidx.compose.ui.graphics.Color(0xFF4CAF50)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                data.entries.forEachIndexed { index, entry ->
                    ChartBarRow(entry.key, entry.value, maxVal, colors[index % colors.size])
                }
            }
        }
    }
}
"""

content = re.sub(r'@Composable\nfun AiUsageDashboardCard\(\) \{[\s\S]*?(?=@Composable\nfun UsageBarChartCard)', dashboard_code.strip() + '\n\n', content)

with open('app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'w') as f:
    f.write(content)
