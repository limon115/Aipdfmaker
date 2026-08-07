import sys

with open('/app/applet/app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'val settings by viewModel.settings.collectAsStateWithLifecycle()',
    'val settingsNullable by viewModel.settings.collectAsStateWithLifecycle()'
)

content = content.replace(
    '    ) { innerPadding ->\n        Column(',
    '    ) { innerPadding ->\n        if (settingsNullable == null) {\n            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {\n                CircularProgressIndicator()\n            }\n        } else {\n            val settings = settingsNullable!!\n            Column('
)

content = content.replace(
    '                    )\n                }\n            )\n        }\n    }\n}\n\n@OptIn',
    '                    )\n                }\n            )\n        }\n        }\n    }\n}\n\n@OptIn'
)

with open('/app/applet/app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'w') as f:
    f.write(content)
