import sys

with open('/app/applet/app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'r') as f:
    content = f.read()

imports_to_add = """import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Info
"""
content = content.replace("import androidx.compose.runtime.LaunchedEffect", imports_to_add + "import androidx.compose.runtime.LaunchedEffect")

card_call = """                    )
                }
            )
            
            BatteryOptimizationCard()
        }"""
content = content.replace("""                    )
                }
            )
        }""", card_call)

card_impl = """
@Composable
fun BatteryOptimizationCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
    val packageName = context.packageName
    
    // Using a key that changes if the user returns to the screen could be nice, but simple remember is okay 
    // for this settings screen, or we can just calculate it in real time during composition.
    val isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)
    
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BatteryAlert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Background Processing",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Text(
                text = "To ensure long document generations are not killed by the system, please disable battery optimization for this app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (isIgnoringOptimizations) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.Green
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Battery optimization is disabled (Recommended)", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Button(
                    onClick = {
                        val intent = Intent()
                        intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        intent.data = Uri.parse("package:$packageName")
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val alternateIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            try {
                                context.startActivity(alternateIntent)
                            } catch (e2: Exception) {
                                e2.printStackTrace()
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Disable Optimization")
                }
            }
        }
    }
}
"""
content += card_impl

with open('/app/applet/app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'w') as f:
    f.write(content)
