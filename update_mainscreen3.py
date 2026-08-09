with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "r") as f:
    text = f.read()

text = text.replace("val isGenerating = workInfos.isNotEmpty() && !workInfos.first().state.isFinished", "val isGenerating = workInfos.isNotEmpty()")

with open("app/src/main/java/com/example/ui/screens/MainScreen.kt", "w") as f:
    f.write(text)
