import os

root_gradle = "build.gradle.kts"
pristine_content = """// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    configurations.classpath {
        resolutionStrategy.eachDependency {
            // ONLY force the core Kotlin compiler tools, completely ignoring kotlinx (like coroutines/serialization)
            if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-")) {
                useVersion("2.2.10")
            }
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.secrets) apply false
    alias(libs.plugins.google.services) apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10" apply false
}
"""

with open(root_gradle, "w") as f:
    f.write(pristine_content)

print("✅ ROOT GRADLE: Mutilated file completely overwritten with pristine, syntax-perfect code.")
