import os, re

root_gradle = "build.gradle.kts"
if os.path.exists(root_gradle):
    with open(root_gradle, "r") as f:
        content = f.read()

    # The new, smart resolution strategy
    smart_strategy = """buildscript {
    configurations.classpath {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-")) {
                useVersion("2.2.10")
            }
        }
    }
}

"""
    # Remove the old nuclear one, inject the smart one
    content = re.sub(r'buildscript \{[\s\S]*?resolutionStrategy\.eachDependency \{[\s\S]*?\}\n\}\n\}\n\n', '', content)
    content = re.sub(r'buildscript \{[\s\S]*?\}', '', content) # Clean up any remaining
    
    with open(root_gradle, "w") as f:
        f.write(smart_strategy + content)
        
    print("✅ ROOT GRADLE: Applied targeted resolution strategy. Only core Kotlin components are pinned to v2.2.10.")
