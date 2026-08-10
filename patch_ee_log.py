with open("app/src/main/java/com/example/domain/services/export/ExportEngine.kt", "r") as f:
    text = f.read()

target = """        try {
            val safeName = projectName.trim().replace(Regex("[^a-zA-Z0-9.-]"), "_").ifEmpty { "Project" }"""
replacement = """        try {
            com.example.utils.AppLogger.i("ExportEngine", "Exporting project $projectName as ${if (isPdf) "PDF" else "HTML"}")
            val safeName = projectName.trim().replace(Regex("[^a-zA-Z0-9.-]"), "_").ifEmpty { "Project" }"""
if target in text:
    text = text.replace(target, replacement)

target2 = """            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Render Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                onComplete(null, jsonFile)
            }"""
replacement2 = """            } catch (e: Exception) {
                com.example.utils.AppLogger.e("ExportEngine", "Render Error: ${e.localizedMessage}", e)
                e.printStackTrace()
                Toast.makeText(context, "Render Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                onComplete(null, jsonFile)
            }"""
if target2 in text:
    text = text.replace(target2, replacement2)

target3 = """        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }"""
replacement3 = """        } catch (e: Exception) {
            com.example.utils.AppLogger.e("ExportEngine", "Failed: ${e.localizedMessage}", e)
            e.printStackTrace()
            Toast.makeText(context, "Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }"""
if target3 in text:
    text = text.replace(target3, replacement3)

with open("app/src/main/java/com/example/domain/services/export/ExportEngine.kt", "w") as f:
    f.write(text)

