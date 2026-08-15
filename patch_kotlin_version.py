import os
import re

# We will check both common locations for dependency management
build_gradle = "build.gradle.kts"
toml_file = "gradle/libs.versions.toml"

def patch_file(path, search_regex, replacement):
    if not os.path.exists(path):
        return False
    with open(path, "r") as f:
        content = f.read()
    
    if re.search(search_regex, content):
        content = re.sub(search_regex, replacement, content)
        with open(path, "w") as f:
            f.write(content)
        return True
    return False

# Attempt 1: Patch libs.versions.toml (Modern Android Studio setups)
patched_toml = patch_file(
    toml_file,
    r'kotlinx-serialization\s*=\s*"[^"]+"',
    'kotlinx-serialization = "1.6.3"' # Safe, stable version compatible with most 1.9.x / 2.0.x compilers
)

# Attempt 2: Patch root build.gradle.kts (Legacy setups)
patched_gradle = patch_file(
    build_gradle,
    r'id\("org.jetbrains.kotlin.plugin.serialization"\)\s*version\s*"[^"]+"',
    'id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24"' # Ensure this matches your kotlin("android") version
)

if patched_toml or patched_gradle:
    print("✅ SURGICAL LINT REPAIR: Kotlin Serialization version synced to prevent compiler mismatch.")
else:
    print("⚠️ Could not locate the serialization plugin definition. You might need to manually align it with your Kotlin version in build.gradle.kts.")
