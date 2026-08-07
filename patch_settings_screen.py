import sys

with open('/app/applet/app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '    val context = androidx.compose.ui.platform.LocalContext.current\n',
    '    val context = androidx.compose.ui.platform.LocalContext.current\n    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current\n'
)

content = content.replace(
    '                        onError = { android.widget.Toast.makeText(context, "Connection Failed: $it", android.widget.Toast.LENGTH_LONG).show() }',
    '                        onError = {\n                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(it))\n                            android.widget.Toast.makeText(context, "Connection Failed: copied full error to clipboard", android.widget.Toast.LENGTH_LONG).show()\n                        }'
)

with open('/app/applet/app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt', 'w') as f:
    f.write(content)
