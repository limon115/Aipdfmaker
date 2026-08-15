import os, re

print("🔍 Scanning Gradle files to sync Kotlin compiler plugins...")

kotlin_version = None

# Step 1: Find the Kotlin version in TOML and fix the plugin ref
toml_path = "gradle/libs.versions.toml"
if os.path.exists(toml_path):
    with open(toml_path, "r") as f:
        content = f.read()
    
    match = re.search(r'kotlin\s*=\s*"([^"]+)"', content)
    if match:
        kotlin_version = match.group(1)
        print(f"📌 Found Kotlin version {kotlin_version} in TOML.")
        
        # Force the plugin to reference the core Kotlin version
        content = re.sub(
            r'id\s*=\s*"org\.jetbrains\.kotlin\.plugin\.serialization"[^}]+',
            'id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" ',
            content
        )
        with open(toml_path, "w") as f:
            f.write(content)
        print("✅ Synced serialization plugin in libs.versions.toml")

# Step 2: Traverse build.gradle.kts to fix inline versions
for root_dir, _, files in os.walk("."):
    if "build.gradle.kts" in files:
        path = os.path.join(root_dir, "build.gradle.kts")
        with open(path, "r") as f:
            content = f.read()
        
        # If we didn't find the version in TOML, look for it here
        if not kotlin_version:
            match = re.search(r'kotlin\("android"\)\s*version\s*"([^"]+)"', content)
            if match:
                kotlin_version = match.group(1)
                print(f"📌 Found Kotlin version {kotlin_version} in {path}.")

        if kotlin_version:
            # Force replace the serialization version to perfectly match
            content = re.sub(
                r'kotlin\("plugin\.serialization"\)\s*version\s*"[^"]+"',
                f'kotlin("plugin.serialization") version "{kotlin_version}"',
                content
            )
            content = re.sub(
                r'id\("org\.jetbrains\.kotlin\.plugin\.serialization"\)\s*version\s*"[^"]+"',
                f'id("org.jetbrains.kotlin.plugin.serialization") version "{kotlin_version}"',
                content
            )
            with open(path, "w") as f:
                f.write(content)
            print(f"✅ Synced serialization plugin to v{kotlin_version} in {path}")

print("🎉 Plugin sync complete!")
