import sys

with open('/app/applet/app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'r') as f:
    screen = f.read()

old_card = """        Column(
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
        }"""

new_card = """        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Session AI Usage",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            val requestData = mapOf(
                "Requests" to stats.requests,
                "Cache" to stats.cacheHits,
                "Limits" to stats.rateLimitErrors
            )
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "API Calls Breakdown", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                SimpleBarChart(
                    data = requestData, 
                    modifier = Modifier.fillMaxWidth(),
                    barColor = MaterialTheme.colorScheme.primary
                )
            }
            
            androidx.compose.material3.Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Token Usage Overview", 
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                SimpleBarChart(
                    data = mapOf("Tokens Used" to stats.estimatedTokens), 
                    modifier = Modifier.fillMaxWidth(),
                    barColor = androidx.compose.ui.graphics.Color(0xFF4CAF50)
                )
            }
            
            androidx.compose.material3.Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }"""

screen = screen.replace(old_card, new_card)
with open('/app/applet/app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'w') as f:
    f.write(screen)
