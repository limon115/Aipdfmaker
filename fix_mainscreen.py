import re

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'r') as f:
    content = f.read()

fixed = """
                    com.example.ui.components.glass.GlassCard(
                        modifier = Modifier.fillMaxWidth().androidx.compose.foundation.clickable.clickable { navController.navigate("latex_debugger") }
                    ) {
                        androidx.compose.foundation.layout.Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(androidx.compose.material.icons.Icons.Default.androidx.compose.material.icons.filled.BugReport, contentDescription = "LaTeX Debugger", tint = MaterialTheme.colorScheme.primary)
                            androidx.compose.foundation.layout.Spacer(Modifier.androidx.compose.foundation.layout.width(16.dp))
                            Column {
                                Text("LaTeX Debugger", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text("Debug and fix LaTeX compilation errors using AI.", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
"""

content = re.sub(
    r'com\.example\.ui\.components\.glass\.GlassCard\(\s*modifier = Modifier\.fillMaxWidth\(\)\.clickable \{ navController\.navigate\("latex_debugger"\) \}\s*\) \{[\s\S]*?Text\("Debug and fix LaTeX compilation errors using AI\.", style = MaterialTheme\.typography\.bodyMedium\)\n\s*\}\n\s*\}\n\s*\}',
    fixed.strip(),
    content
)

# wait, clickable and width need to be fixed
content = content.replace("androidx.compose.foundation.clickable.clickable", "clickable")
content = content.replace("Modifier.androidx.compose.foundation.layout.width(16.dp)", "androidx.compose.foundation.layout.width(16.dp)")
content = content.replace("androidx.compose.material.icons.Icons.Default.androidx.compose.material.icons.filled.BugReport", "androidx.compose.material.icons.Icons.Default.Build")

# Let's just add imports to the top of MainScreen.kt instead
imports = """
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.Icons
"""
content = imports + content

with open('app/src/main/java/com/example/ui/screens/MainScreen.kt', 'w') as f:
    f.write(content)
