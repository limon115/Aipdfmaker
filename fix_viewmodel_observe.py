import sys

with open('/app/applet/app/src/main/java/com/example/ui/screens/processing/NoteGenerationViewModel.kt', 'r') as f:
    content = f.read()

bad_str = """        _state.update { it.copy(workId = workRequest.id) }
        
        viewModelScope.launch {
            workManager.getWorkInfoByIdLiveData(workRequest.id).asFlow().collect { workInfo ->
                if (workInfo != null) {"""

good_str = """        _state.update { it.copy(workId = workRequest.id) }
        
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkLiveData("NoteGen_${projectId}").asFlow().collect { workInfos ->
                val workInfo = workInfos.firstOrNull()
                if (workInfo != null) {"""

content = content.replace(bad_str, good_str)

with open('/app/applet/app/src/main/java/com/example/ui/screens/processing/NoteGenerationViewModel.kt', 'w') as f:
    f.write(content)
