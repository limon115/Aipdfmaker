with open('app/src/main/java/com/example/ui/screens/viewer/NotesViewerViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'projectName: String = "Project",',
    'project: com.example.data.database.ProjectEntity? = null,'
)

content = content.replace(
    'projectName = project?.title ?: "Project",',
    'project = project,'
)

content = content.replace(
    'projectName = _state.value.projectName,',
    'project = _state.value.project!!,'
)

content = content.replace(
    'val safeName = _state.value.projectName.trim()',
    'val safeName = _state.value.project?.title?.trim() ?: "Project"'
)

with open('app/src/main/java/com/example/ui/screens/viewer/NotesViewerViewModel.kt', 'w') as f:
    f.write(content)
