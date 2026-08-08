import sys

with open('/app/applet/app/src/main/java/com/example/domain/services/ai/TextChunker.kt', 'r') as f:
    content = f.read()

content = content.replace("val lastNewline = text.lastIndexOf('\\n', endIndex)", 'val lastNewline = text.lastIndexOf("\\n", endIndex)')
content = content.replace("val lastPeriod = text.lastIndexOf('.', endIndex)", 'val lastPeriod = text.lastIndexOf(".", endIndex)')

with open('/app/applet/app/src/main/java/com/example/domain/services/ai/TextChunker.kt', 'w') as f:
    f.write(content)


with open('/app/applet/app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt', 'r') as f:
    worker = f.read()

# wait, I did:
#                 val html = service.generateHtmlForTopic(
#                     topicTitle = topic.title,
#                     blueprintContext = blueprintJson,
#                     relevantContext = relevantContext,
#                     ai2Provider = settings.ai2Provider.name,
#                     ...
# But in NoteGenerationWorker.kt it said "No parameter with name 'sourceText' found" which means I missed a spot or it didn't replace.

with open('/app/applet/app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt', 'w') as f:
    f.write(worker.replace("sourceText = sourceText,", "relevantContext = relevantContext,"))

print("Fixed")
