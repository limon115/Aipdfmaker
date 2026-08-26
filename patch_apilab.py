import re

content = ""
with open('app/src/main/java/com/example/ui/screens/settings/ApiLabScreen.kt', 'r') as f:
    content = f.read()

# For ApiLabScreen success card 1
content = content.replace(
    'Icon(Icons.Default.CheckCircle, "Success", tint = Color(0xFF4CAF50))',
    'val isDark = com.example.ui.theme.AppTheme.colors.isDark\n                            val successColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)\n                            val successColorMuted = if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)\n                            Icon(Icons.Default.CheckCircle, "Success", tint = successColor)'
)

content = content.replace(
    'Text("API KEY VALID", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))',
    'Text("API KEY VALID", fontWeight = FontWeight.Bold, color = successColor)'
)

content = content.replace(
    'Text("Connection successful", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF2E7D32))',
    'Text("Connection successful", style = MaterialTheme.typography.bodyMedium, color = successColor)'
)

content = content.replace(
    'Text("Response time    ${testState.latencyMs} ms", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))',
    'Text("Response time    ${testState.latencyMs} ms", style = MaterialTheme.typography.bodySmall, color = successColor)'
)

content = content.replace(
    'Text("Models found     ${testState.models.size}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32))',
    'Text("Models found     ${testState.models.size}", style = MaterialTheme.typography.bodySmall, color = successColor)'
)


# For ApiLabScreen success card 2
content = content.replace(
    'Icon(Icons.Default.CheckCircle, "Success", tint = Color(0xFF4CAF50))\n                            Spacer(modifier = Modifier.width(8.dp))\n                            Text("MODEL TEST PASSED", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))',
    'val isDark = com.example.ui.theme.AppTheme.colors.isDark\n                            val successColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)\n                            val successColorMuted = if (isDark) Color(0xFFA5D6A7) else Color(0xFF1B5E20)\n                            Icon(Icons.Default.CheckCircle, "Success", tint = successColor)\n                            Spacer(modifier = Modifier.width(8.dp))\n                            Text("MODEL TEST PASSED", fontWeight = FontWeight.Bold, color = successColor)'
)

content = content.replace(
    'Text("Response", style = MaterialTheme.typography.labelMedium, color = Color(0xFF2E7D32))',
    'Text("Response", style = MaterialTheme.typography.labelMedium, color = successColor)'
)

content = content.replace(
    'Text(testState.response, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1B5E20))',
    'Text(testState.response, style = MaterialTheme.typography.bodyMedium, color = successColorMuted)'
)

content = content.replace(
    'Text("Latency", style = MaterialTheme.typography.labelMedium, color = Color(0xFF2E7D32))',
    'Text("Latency", style = MaterialTheme.typography.labelMedium, color = successColor)'
)

content = content.replace(
    'Text("${testState.latencyMs} ms", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1B5E20))',
    'Text("${testState.latencyMs} ms", style = MaterialTheme.typography.bodyMedium, color = successColorMuted)'
)


# Error Diagnostics Card
content = content.replace(
    'val containerColor = if (isNetwork || isRateLimit) Color(0xFFFFF3E0) else Color(0xFFFFEBEE)\n    val contentColor = if (isNetwork || isRateLimit) Color(0xFFE65100) else Color(0xFFC62828)',
    'val isDark = com.example.ui.theme.AppTheme.colors.isDark\n    val containerColor = if (isNetwork || isRateLimit) Color(0xFFFFF3E0) else Color(0xFFFFEBEE)\n    val contentColor = if (isDark) {\n        if (isNetwork || isRateLimit) Color(0xFFFFB74D) else Color(0xFFE57373)\n    } else {\n        if (isNetwork || isRateLimit) Color(0xFFE65100) else Color(0xFFC62828)\n    }'
)

with open('app/src/main/java/com/example/ui/screens/settings/ApiLabScreen.kt', 'w') as f:
    f.write(content)
