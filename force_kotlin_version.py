import re, os

print("🔍 Scanning for exact Kotlin version...")

# 1. Dynamically grab the exact Kotlin version you are using
kotlin_version = "2.2.10" # Fallback
toml_path = "gradle/libs.versions.toml"
if os.path.exists(toml_path):
    with open(toml_path, "r") as f:
        match = re.search(r'kotlin\s*=\s*"([^"]+)"', f.read())
        if match:
            kotlin_version = match.group(1)

print(f"📌 Forcing Serialization Plugin to strictly lock to v{kotlin_version}...")

# 2. Aggressively patch the root build.gradle.kts
root_gradle = "build.gradle.kts"
if os.path.exists(root_gradle):
    with open(root_gradle, "r") as f:
        root_content = f.read()
    
    # Nuke any existing serialization declarations (aliases, ids, etc.)
    root_content = re.sub(r'(?m)^\s*(alias|id|kotlin).*serialization.*$\n?', '', root_content)
    
    # Force inject the exact version string directly inside plugins {
    root_content = root_content.replace(
        'plugins {', 
        f'plugins {{\n    id("org.jetbrains.kotlin.plugin.serialization") version "{kotlin_version}" apply false'
    )
    
    with open(root_gradle, "w") as f:
        f.write(root_content)
    print("✅ ROOT GRADLE: Serialization version explicitly hardcoded.")

# 3. Aggressively patch the app build.gradle.kts
app_gradle = "app/build.gradle.kts"
if os.path.exists(app_gradle):
    with open(app_gradle, "r") as f:
        app_content = f.read()
    
    # Nuke existing declarations
    app_content = re.sub(r'(?m)^\s*(alias|kotlin|id).*serialization.*$\n?', '', app_content)
    
    # Force inject the plugin ID
    app_content = app_content.replace(
        'plugins {', 
        'plugins {\n    id("org.jetbrains.kotlin.plugin.serialization")'
    )
    
    with open(app_gradle, "w") as f:
        f.write(app_content)
    print("✅ APP GRADLE: Plugin explicitly applied without alias resolution.")

print("🎉 SURGICAL LINT REPAIR COMPLETE: Gradle can no longer escape the version lock!")
