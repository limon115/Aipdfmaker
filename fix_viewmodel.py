import sys

with open('/app/applet/app/src/main/java/com/example/ui/screens/processing/NoteGenerationViewModel.kt', 'r') as f:
    content = f.read()

bad_str = """        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(workRequest)"""

good_str = """        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            "NoteGen_${projectId}",
            androidx.work.ExistingWorkPolicy.KEEP,
            workRequest
        )"""

content = content.replace(bad_str, good_str)

with open('/app/applet/app/src/main/java/com/example/ui/screens/processing/NoteGenerationViewModel.kt', 'w') as f:
    f.write(content)
