import re

with open('app/src/main/java/com/example/ui/screens/settings/SettingsViewModel.kt', 'r') as f:
    content = f.read()

methods = """
    fun updateAi3Provider(provider: AiProvider) {
        viewModelScope.launch { dataStore.updateAi3Provider(provider) }
    }
    fun updateAi3Model(model: String) {
        viewModelScope.launch { dataStore.updateAi3Model(model) }
    }
    fun updateAi3ApiKey(apiKey: String) {
        viewModelScope.launch { dataStore.updateAi3ApiKey(apiKey) }
    }
"""

content = re.sub(r'fun updateThemeMode', methods + '\n    fun updateThemeMode', content)

with open('app/src/main/java/com/example/ui/screens/settings/SettingsViewModel.kt', 'w') as f:
    f.write(content)

