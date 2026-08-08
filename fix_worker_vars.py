import sys

with open('/app/applet/app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt', 'r') as f:
    worker = f.read()

old = """            setForeground(createForegroundInfo("Starting generation...", 0, totalTopics))
            
            blueprint.topics.forEachIndexed { index, topic ->"""
            
new = """            setForeground(createForegroundInfo("Starting generation...", 0, totalTopics))
            
            val chunker = TextChunker()
            val retriever = TopicContextRetriever()
            val chunks = chunker.chunkText(sourceText, 3000, 300)
            
            blueprint.topics.forEachIndexed { index, topic ->"""

worker = worker.replace(old, new)

with open('/app/applet/app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt', 'w') as f:
    f.write(worker)

print("Fixed")
