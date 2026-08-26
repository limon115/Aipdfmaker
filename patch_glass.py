import re

with open('app/src/main/java/com/example/ui/components/glass/GlassSurface.kt', 'r') as f:
    content = f.read()

content = content.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.graphics.graphicsLayer\nimport androidx.compose.ui.draw.BlurredEdgeTreatment\nimport androidx.compose.ui.graphics.CompositingStrategy')

pattern = r'modifier\s*=\s*modifier\s*\.clip\(shape\)'
replacement = """modifier = modifier
            .graphicsLayer {
                clip = true
                this.shape = shape
                compositingStrategy = CompositingStrategy.Offscreen
            }"""
content = re.sub(pattern, replacement, content)

pattern2 = r'\.blur\(16\.dp\)'
replacement2 = '.blur(16.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)'
content = re.sub(pattern2, replacement2, content)

with open('app/src/main/java/com/example/ui/components/glass/GlassSurface.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/example/ui/components/glass/GlassCard.kt', 'r') as f:
    content2 = f.read()

content2 = content2.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.graphics.graphicsLayer\nimport androidx.compose.ui.graphics.CompositingStrategy')

pattern_card = r'modifier\s*=\s*modifier\s*\.clip\(shape\)'
replacement_card = """modifier = modifier
            .graphicsLayer {
                clip = true
                this.shape = shape
                compositingStrategy = CompositingStrategy.Offscreen
            }"""
content2 = re.sub(pattern_card, replacement_card, content2)

with open('app/src/main/java/com/example/ui/components/glass/GlassCard.kt', 'w') as f:
    f.write(content2)

