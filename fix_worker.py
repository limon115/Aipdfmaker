import sys
with open('/app/applet/app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt', 'r') as f:
    content = f.read()

content = content.replace('projectDao.updateProject(project.copy', 'project?.let { projectDao.updateProject(it.copy')
content = content.replace('project.title', 'project?.title ?: "Project"')
content = content.replace('status = "Completed", lastUpdated = System.currentTimeMillis()))', 'status = "Completed", lastUpdated = System.currentTimeMillis())) }')
content = content.replace('status = "Failed", lastUpdated = System.currentTimeMillis()))', 'status = "Failed", lastUpdated = System.currentTimeMillis())) }')

with open('/app/applet/app/src/main/java/com/example/domain/services/worker/NoteGenerationWorker.kt', 'w') as f:
    f.write(content)
