with open("app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt", "r") as f:
    text = f.read()

text = text.replace("model = settings.ai2Model,", 'model = settings.ai2Model.ifBlank { "gemini-1.5-flash" },')

with open("app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt", "w") as f:
    f.write(text)
