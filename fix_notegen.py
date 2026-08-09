with open("app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt", "r") as f:
    text = f.read()

text = text.replace(
    "val rawResponse = clientForGeneration.generateContent(prompt, systemPrompt)",
    "val rawResponse = clientForGeneration.generateContent(prompt, systemPrompt, \"application/json\", true)"
)

with open("app/src/main/java/com/example/domain/services/ai/NoteGenerationService.kt", "w") as f:
    f.write(text)
