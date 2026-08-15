import os

workflow = ".github/workflows/build.yml"
if os.path.exists(workflow):
    with open(workflow, "r") as f:
        content = f.read()
    
    # Inject the aggressive cache-busting flags into the Gradle command
    old_cmd = "./gradlew clean assembleDebug --no-daemon"
    new_cmd = "./gradlew clean assembleDebug --no-daemon --refresh-dependencies --no-build-cache"
    
    if old_cmd in content:
        content = content.replace(old_cmd, new_cmd)
        with open(workflow, "w") as f:
            f.write(content)
        print("✅ CLOUD WORKFLOW: Cache-busting flags injected. The cloud's poisoned memory will be wiped!")
    elif new_cmd in content:
        print("⚠️ Cache-busting flags are already present.")
    else:
        print("⚠️ Could not find the exact gradlew command. Please check build.yml.")
else:
    print("❌ Could not find build.yml")
