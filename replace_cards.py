import os
import re

directory = 'app/src/main/java/com/example/ui'

pattern = re.compile(r'\b(?<!Glass)(Card|ElevatedCard|OutlinedCard)\s*\(')

for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith('.kt') and file != 'GlassCard.kt':
            filepath = os.path.join(root, file)
            with open(filepath, 'r') as f:
                content = f.read()
            
            new_content = pattern.sub('com.example.ui.components.glass.GlassCard(', content)
            
            if new_content != content:
                with open(filepath, 'w') as f:
                    f.write(new_content)
                print(f'Updated {filepath}')
