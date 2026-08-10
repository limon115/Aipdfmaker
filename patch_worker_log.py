with open("app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt", "r") as f:
    text = f.read()

target = """        try {
            val database = AppDatabase.getDatabase(applicationContext)"""
replacement = """        try {
            com.example.utils.AppLogger.i("NoteGenWorker", "Starting generation for project $projectId")
            val database = AppDatabase.getDatabase(applicationContext)"""
if target in text:
    text = text.replace(target, replacement)

target2 = """            } catch (e: Exception) {
                e.printStackTrace()
                // Update specific snippet as failed"""
replacement2 = """            } catch (e: Exception) {
                com.example.utils.AppLogger.e("NoteGenWorker", "Error generating snippet for topic ${topic.title}", e)
                e.printStackTrace()
                // Update specific snippet as failed"""
if target2 in text:
    text = text.replace(target2, replacement2)

target3 = """            } catch (e: Exception) {
                e.printStackTrace()
            }"""
replacement3 = """            } catch (e: Exception) {
                com.example.utils.AppLogger.e("NoteGenWorker", "Error updating overall failure state", e)
                e.printStackTrace()
            }"""
if target3 in text:
    text = text.replace(target3, replacement3)

target4 = """            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()"""
replacement4 = """            com.example.utils.AppLogger.i("NoteGenWorker", "Finished generation for project $projectId")
            return Result.success()
        } catch (e: Exception) {
            com.example.utils.AppLogger.e("NoteGenWorker", "Fatal error in worker", e)
            e.printStackTrace()"""
if target4 in text:
    text = text.replace(target4, replacement4)

with open("app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt", "w") as f:
    f.write(text)

