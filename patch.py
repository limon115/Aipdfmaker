with open("app/src/main/java/com/example/domain/services/html/DocumentMergeEngine.kt", "r") as f:
    text = f.read()

text = text.replace("import kotlinx.serialization.json.*", "import kotlinx.serialization.json.*\nimport kotlinx.serialization.encodeToString\nimport kotlinx.serialization.decodeFromString")

with open("app/src/main/java/com/example/domain/services/html/DocumentMergeEngine.kt", "w") as f:
    f.write(text)
