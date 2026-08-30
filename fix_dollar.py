with open('app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '$$ ... $$',
    '${"$$"} ... ${"$$"}'
)

with open('app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt', 'w') as f:
    f.write(content)
