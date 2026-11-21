import os

filepath = "app/src/main/java/com/example/domain/services/pdf/TermuxXeLaTeXBridge.kt"

if os.path.exists(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Fix 1: Update the literal string keys to match the actual Termux API
    content = content.replace('"com.termux.service.EXTRA_PLUGIN_RESULT_BUNDLE"', '"result_bundle"')
    content = content.replace('"com.termux.service.EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE"', '"exitCode"')
    content = content.replace('"com.termux.service.EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT"', '"stdout"')
    content = content.replace('"com.termux.service.EXTRA_PLUGIN_RESULT_BUNDLE_STDERR"', '"stderr"')
    content = content.replace('"com.termux.service.EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG"', '"err"')

    # Fix 2: Implement Android 8.0+ background execution compliance
    old_service_call = "context.startService(intent)"
    
    new_service_call = """if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }"""
                
    content = content.replace(old_service_call, new_service_call)

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
        
    print(f"✅ Successfully patched: {filepath}")
    print("🚀 Termux execution intents and bundle keys are locked in!")
else:
    print(f"❌ Error: '{filepath}' not found. Are you in the project root?")
