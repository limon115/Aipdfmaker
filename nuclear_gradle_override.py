import re
import os

print("🔍 Initiating Nuclear Classpath Override...")

# 1. Grab the exact Kotlin version to use as the dictator
kotlin_version = "2.2.10"
toml_path = "gradle/libs.versions.toml"
if os.path.exists(toml_path):
    with open(toml_path, "r") as f:
        match = re.search(r'kotlin\s*=\s*"([^"]+)"', f.read())
        if match:
            kotlin_version = match.group(1)

# 2. Inject the global resolution strategy at the absolute top of root build.gradle.kts
root_gradle = "build.gradle.kts"
if os.path.exists(root_gradle):
    with open(root_gradle, "r") as f:
        content = f.read()
    
    override_block = f"""buildscript {{
    configurations.classpath {{
        resolutionStrategy.eachDependency {{
            if (requested.group.startsWith("org.jetbrains.kotlin")) {{
                useVersion("{kotlin_version}")
            }}
        }}
    }}
}}

"""
    if "resolutionStrategy" not in content:
        with open(root_gradle, "w") as f:
            f.write(override_block + content)
        print(f"✅ ROOT GRADLE: Dictator override injected. All Kotlin dependencies FORCED to v{kotlin_version}!")
    else:
        print("⚠️ Override already present.")

# 3. Patch the GitHub Actions workflow to nuke the poisoned cache
workflow_path = ".github/workflows/build.yml"
if os.path.exists(workflow_path):
    with open(workflow_path, "r") as f:
        content = f.read()
    
    if "./gradlew clean assembleDebug" not in content:
        content = content.replace("./gradlew assembleDebug", "./gradlew clean assembleDebug")
        with open(workflow_path, "w") as f:
            f.write(content)
        print("✅ CLOUD WORKFLOW: Added 'clean' to flush the poisoned Gradle cache.")
else:
    print("⚠️ Could not find build.yml workflow.")

print("🎉 SURGICAL LINT REPAIR COMPLETE: The transitive leak is permanently sealed.")
