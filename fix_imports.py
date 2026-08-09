with open("app/src/main/java/com/example/data/network/AiNetworkClient.kt", "r") as f:
    text = f.read()

text = text.replace("import kotlinx.serialization.json.JsonObject\n", "")
text = text.replace("import kotlinx.serialization.json.Json", "import kotlinx.serialization.json.Json\nimport kotlinx.serialization.json.JsonObject\nimport kotlinx.serialization.json.JsonPrimitive\nimport kotlinx.serialization.json.buildJsonObject\nimport kotlinx.serialization.json.buildJsonArray")

with open("app/src/main/java/com/example/data/network/AiNetworkClient.kt", "w") as f:
    f.write(text)
