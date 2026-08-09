import re

with open("app/src/main/java/com/example/ui/screens/project/NewProjectViewModel.kt", "r") as f:
    text = f.read()

# Add loadProject method
load_project = """
    fun loadProject(projectId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val project = projectDao.getProjectById(projectId)
            if (project != null) {
                _state.update {
                    it.copy(
                        projectTitle = project.title,
                        course = project.course,
                        chapter = project.chapter,
                        noteStyle = project.noteStyle,
                        outputFormat = project.outputFormat,
                        extractedText = project.sourceText
                    )
                }
            }
        }
    }
"""

text = text.replace("fun createProject(onProjectCreated: (Int) -> Unit) {", load_project + "\n    fun createProject(onProjectCreated: (Int) -> Unit) {")

with open("app/src/main/java/com/example/ui/screens/project/NewProjectViewModel.kt", "w") as f:
    f.write(text)
