with open("app/src/main/java/com/example/ui/screens/processing/ProcessingViewModel.kt", "r") as f:
    text = f.read()

target = """        viewModelScope.launch(Dispatchers.IO) {
            // Step 1: OCR Processing (simulated wait since text is already extracted in NewProjectViewModel)
            updateStepState(0, StepState.IN_PROGRESS, 25)"""
replacement = """        viewModelScope.launch(Dispatchers.IO) {
            com.example.utils.AppLogger.i("ProcessingVM", "Starting processing for project $projectId")
            // Step 1: OCR Processing (simulated wait since text is already extracted in NewProjectViewModel)
            updateStepState(0, StepState.IN_PROGRESS, 25)"""
if target in text:
    text = text.replace(target, replacement)

target2 = """            } catch (e: Exception) {
                e.printStackTrace()
                // Stop spinning and show error on the UI
                updateStepState(1, StepState.FAILED, 75)
            }"""
replacement2 = """            } catch (e: Exception) {
                com.example.utils.AppLogger.e("ProcessingVM", "Failed to generate blueprint", e)
                e.printStackTrace()
                // Stop spinning and show error on the UI
                updateStepState(1, StepState.FAILED, 75)
            }"""
if target2 in text:
    text = text.replace(target2, replacement2)

with open("app/src/main/java/com/example/ui/screens/processing/ProcessingViewModel.kt", "w") as f:
    f.write(text)

