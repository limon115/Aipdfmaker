import re

with open('app/src/main/java/com/example/ui/theme/GlassTheme.kt', 'r') as f:
    content = f.read()

# Find @Composable fun GlassCard and remove it and its body
match = re.search(r'@Composable\s*\nfun GlassCard\b.*?(?=\n@Composable|\Z)', content, re.DOTALL)
if match:
    # Just in case there are other things, we might need a more robust removal
    # But looking at GlassTheme.kt, GlassCard is the last function in the file.
    pass

# A simpler way since it is the last function in the file:
target = """@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    elevated: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = AppTheme.colors
    val bgColor = if (elevated) colors.surfaceElevated else colors.surface
    val borderColor = colors.border

    Surface(
        modifier = modifier
            .shadow(
                elevation = if (elevated) 8.dp else 0.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.02f)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = shape
            )
            .clip(shape),
        color = bgColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        content = content
    )
}"""
if target in content:
    content = content.replace(target, "")
    with open('app/src/main/java/com/example/ui/theme/GlassTheme.kt', 'w') as f:
        f.write(content)
else:
    print("Could not find GlassCard to remove")

