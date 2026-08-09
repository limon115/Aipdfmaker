with open("app/src/main/java/com/example/ui/screens/processing/NoteGenerationScreen.kt", "r") as f:
    text = f.read()

target = r"""    LaunchedEffect\(blueprintState, sourceText\) \{
        val blueprint = blueprintState
        if \(blueprint != null && sourceText\.isNotEmpty\(\)\) \{
            viewModel\.startGenerationLoop\(context, projectId, blueprint, sourceText\)
        \}
    \}"""

replacement = """    LaunchedEffect(blueprintState, sourceText) {
        val blueprint = blueprintState
        if (blueprint != null && sourceText.isNotEmpty()) {
            viewModel.startGenerationLoop(context, projectId, blueprint, sourceText)
        } else if (blueprint == null) {
            viewModel.resumeObservation(context, projectId)
        }
    }"""

text = text.replace("    LaunchedEffect(blueprintState, sourceText) {\n        val blueprint = blueprintState\n        if (blueprint != null && sourceText.isNotEmpty()) {\n            viewModel.startGenerationLoop(context, projectId, blueprint, sourceText)\n        }\n    }", replacement)

with open("app/src/main/java/com/example/ui/screens/processing/NoteGenerationScreen.kt", "w") as f:
    f.write(text)
