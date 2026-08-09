with open("app/src/main/java/com/example/ui/screens/project/ProjectDetailsScreen.kt", "r") as f:
    text = f.read()

import re

# Update imports
imports = """import androidx.compose.ui.platform.LocalContext
import android.os.Environment
import java.io.File
"""
if "import java.io.File" not in text:
    text = text.replace("import androidx.compose.ui.unit.dp", "import androidx.compose.ui.unit.dp\n" + imports)

# Update signature
sig_target = r"""@Composable
fun ProjectDetailsScreen\(
    viewModel: NewProjectViewModel,
    onCreateProject: \(Int\) -> Unit,
    onNavigateBack: \(\) -> Unit
\) \{
    val state by viewModel\.state\.collectAsState\(\)
    val scrollState = rememberScrollState\(\)"""

sig_replacement = """@Composable
fun ProjectDetailsScreen(
    viewModel: NewProjectViewModel,
    projectId: Int? = null,
    onCreateProject: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToProgress: (Int) -> Unit = {},
    onNavigateToViewer: (Int) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    
    var documentExists by remember { mutableStateOf(false) }
    var documentContent by remember { mutableStateOf("") }
    
    LaunchedEffect(projectId) {
        if (projectId != null) {
            viewModel.loadProject(projectId)
        }
    }
    
    LaunchedEffect(state.projectTitle, projectId) {
        if (projectId != null && state.projectTitle.isNotEmpty()) {
            val safeName = state.projectTitle.trim().replace(Regex("[^a-zA-Z0-9.-]"), "_").ifEmpty { "Project" }
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val baseDir = File(documentsDir, "aipdfs/$safeName")
            val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val fallbackBaseDir = File(fallbackDir ?: context.filesDir, "aipdfs/$safeName")
            
            val jsonFile = File(baseDir, "document.json")
            val fallbackJsonFile = File(fallbackBaseDir, "document.json")
            
            if (jsonFile.exists()) {
                documentExists = true
                documentContent = jsonFile.readText()
            } else if (fallbackJsonFile.exists()) {
                documentExists = true
                documentContent = fallbackJsonFile.readText()
            } else {
                documentExists = false
                documentContent = ""
            }
        }
    }"""

text = re.sub(sig_target, sig_replacement, text)

# Update the Button at the bottom
button_target = r"""            Spacer\(modifier = Modifier\.weight\(1f\)\)
            Spacer\(modifier = Modifier\.height\(16\.dp\)\)
            Button\(
                onClick = \{
                    viewModel\.createProject \{ id ->
                        onCreateProject\(id\)
                    \}
                \},
                modifier = Modifier
                    \.fillMaxWidth\(\)
                    \.height\(56\.dp\),
                shape = RoundedCornerShape\(16\.dp\),
                colors = ButtonDefaults\.buttonColors\(
                    containerColor = MaterialTheme\.colorScheme\.primary
                \)
            \) \{
                Text\("Create Project", style = MaterialTheme\.typography\.titleMedium\)
            \}"""

button_replacement = """            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(16.dp))
            
            if (projectId != null) {
                if (documentExists) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Current Content Available", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(documentContent.take(200) + if(documentContent.length > 200) "..." else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Button(
                        onClick = { onNavigateToViewer(projectId) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("View Content", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    Button(
                        onClick = { onNavigateToProgress(projectId) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("View Progress", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                Button(
                    onClick = {
                        viewModel.createProject { id ->
                            onCreateProject(id)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Create Project", style = MaterialTheme.typography.titleMedium)
                }
            }"""

text = re.sub(button_target, button_replacement, text)

with open("app/src/main/java/com/example/ui/screens/project/ProjectDetailsScreen.kt", "w") as f:
    f.write(text)
