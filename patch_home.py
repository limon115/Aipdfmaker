import re
with open('app/src/main/java/com/example/ui/screens/home/HomeScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'import com.example.ui.components.glass.GlassCard',
    'import com.example.ui.components.glass.GlassCard\nimport com.example.ui.components.PdfIcon'
)

old_box = """            // Image Placeholder
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = "Project Image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }"""

new_box = """            // Image Placeholder
            PdfIcon(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
            )"""

content = content.replace(old_box, new_box)

with open('app/src/main/java/com/example/ui/screens/home/HomeScreen.kt', 'w') as f:
    f.write(content)
