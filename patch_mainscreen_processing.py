import os

filepath = "app/src/main/java/com/example/ui/screens/MainScreen.kt"
with open(filepath, "r") as f:
    text = f.read()

import re

target = r'''                ProcessingScreen\(
                    viewModel = processingViewModel,
                    projectId = projectId,
                    onProcessingFinished = \{ summary ->
                        if \(summary != null\) \{
                            blueprintViewModel\.setBlueprintSummary\(summary\)
                            navController\.navigate\("\$ROUTE_BLUEPRINT_VIEW/\$projectId"\) \{
                                popUpTo\(ROUTE_PROCESSING\) \{ inclusive = true \}
                            \}
                        \}
                    \}
                \)'''

replacement = '''                ProcessingScreen(
                    viewModel = processingViewModel,
                    projectId = projectId,
                    onProcessingFinished = { summary ->
                        if (summary != null) {
                            blueprintViewModel.setBlueprintSummary(summary)
                            navController.navigate("$ROUTE_BLUEPRINT_VIEW/$projectId") {
                                popUpTo(ROUTE_PROCESSING) { inclusive = true }
                            }
                        }
                    },
                    onNavigateBack = {
                        navController.navigate(ROUTE_HOME) {
                            popUpTo(0)
                        }
                    }
                )'''

if re.search(target, text):
    text = re.sub(target, replacement, text)
else:
    print("Target not found. Will just append onNavigateBack parameter.")
    text = text.replace('onProcessingFinished = { summary ->', 'onNavigateBack = { navController.popBackStack() },\n                    onProcessingFinished = { summary ->')

with open(filepath, "w") as f:
    f.write(text)

