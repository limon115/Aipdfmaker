import os, re

print("🔧 Aligning KSP and Gradle configurations...")

# 1. Fix gradle/libs.versions.toml
toml_path = "gradle/libs.versions.toml"
if os.path.exists(toml_path):
    with open(toml_path, "r") as f:
        toml = f.read()

    # Fix KSP version to match Kotlin 2.2.10
    toml = re.sub(r'googleDevtoolsKsp\s*=\s*"[^"]+"', 'googleDevtoolsKsp = "2.2.10-2.0.2"', toml)
    
    # Ensure kotlin-serialization plugin alias exists in [plugins]
    if "kotlin-serialization" not in toml:
        toml = toml.replace(
            "[plugins]",
            '[plugins]\nkotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }'
        )

    with open(toml_path, "w") as f:
        f.write(toml)
    print("✅ TOML: Pinned googleDevtoolsKsp to '2.2.10-2.0.2' and mapped serialization alias.")

# 2. Reset root build.gradle.kts to a clean, standard configuration
root_gradle = "build.gradle.kts"
pristine_root = """// Top-level build file where you can add configuration options common to all sub-projects/modules.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.secrets) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
"""
with open(root_gradle, "w") as f:
    f.write(pristine_root)
print("✅ ROOT GRADLE: Cleaned up plugins block with synchronized aliases.")

# 3. Ensure app/build.gradle.kts uses the alias
app_gradle = "app/build.gradle.kts"
if os.path.exists(app_gradle):
    with open(app_gradle, "r") as f:
        app = f.read()

    # Replace hardcoded ID or old syntax with alias
    app = re.sub(r'(?m)^\s*(id\("org\.jetbrains\.kotlin\.plugin\.serialization"\)|kotlin\("plugin\.serialization"\)).*$\n?', '', app)
    if "alias(libs.plugins.kotlin.serialization)" not in app:
        app = app.replace("plugins {", "plugins {\n    alias(libs.plugins.kotlin.serialization)")

    with open(app_gradle, "w") as f:
        f.write(app)
    print("✅ APP GRADLE: Configured to use alias(libs.plugins.kotlin.serialization).")

print("🎉 SURGICAL REPAIR COMPLETE!")
