import re
import os

print("🔍 Aligning Gradle Plugin Versions...")

# 1. Update libs.versions.toml to explicitly track the serialization plugin
toml_path = "gradle/libs.versions.toml"
if os.path.exists(toml_path):
    with open(toml_path, "r") as f:
        toml = f.read()
    
    if "kotlin-serialization" not in toml:
        # Inject the serialization plugin into the [plugins] block, referencing the core kotlin version
        toml = toml.replace("[plugins]", "[plugins]\nkotlin-serialization = { id = \"org.jetbrains.kotlin.plugin.serialization\", version.ref = \"kotlin\" }")
        with open(toml_path, "w") as f:
            f.write(toml)
        print("✅ TOML: Added 'kotlin-serialization' alias synced to core Kotlin version.")

# 2. Update root build.gradle.kts to use the alias instead of a hardcoded string
root_gradle = "build.gradle.kts"
if os.path.exists(root_gradle):
    with open(root_gradle, "r") as f:
        root = f.read()
    
    # Strip out the hardcoded ID and replace with the strict alias
    root = re.sub(
        r'id\("org\.jetbrains\.kotlin\.plugin\.serialization"\).*', 
        'alias(libs.plugins.kotlin.serialization) apply false', 
        root
    )
    with open(root_gradle, "w") as f:
        f.write(root)
    print("✅ ROOT GRADLE: Replaced hardcoded serialization ID with strict TOML alias.")

# 3. Update app build.gradle.kts to use the alias
app_gradle = "app/build.gradle.kts"
if os.path.exists(app_gradle):
    with open(app_gradle, "r") as f:
        app = f.read()
    
    app = app.replace('kotlin("plugin.serialization")', 'alias(libs.plugins.kotlin.serialization)')
    with open(app_gradle, "w") as f:
        f.write(app)
    print("✅ APP GRADLE: Switched to synchronized TOML alias.")

print("🎉 SURGICAL LINT REPAIR COMPLETE: Gradle will now force perfect synchronization!")
