import re

with open("app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt", "r") as f:
    content = f.read()

# Add ThemeMode import if not present
if "import com.example.domain.models.ThemeMode" not in content:
    content = content.replace("import com.example.domain.models.AiProvider", "import com.example.domain.models.AiProvider\nimport com.example.domain.models.ThemeMode")

# Add GeneralSettingsCard UI component
new_card = """
@Composable
fun ThemeSettingsCard(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    com.example.ui.theme.GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("App Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ThemeOptionButton(
                    text = "System",
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ThemeOptionButton(
                    text = "Light",
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                ThemeOptionButton(
                    text = "Dark",
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { onThemeModeChange(ThemeMode.DARK) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ThemeOptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
    val contentColor = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.outline

    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        contentColor = contentColor,
        border = if (!selected) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}
"""

if "fun ThemeSettingsCard(" not in content:
    content = content + new_card

# Insert the card into the list of cards
# We can find `AiConfigCard(` for AI #1 and put the theme card before it.
insert_target = """            Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {"""

insert_replacement = insert_target + """
            ThemeSettingsCard(
                themeMode = settings.themeMode,
                onThemeModeChange = { viewModel.updateThemeMode(it) }
            )"""

content = content.replace(insert_target, insert_replacement)

with open("app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt", "w") as f:
    f.write(content)

