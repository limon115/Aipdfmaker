import os
import re

# 1. Update libs.versions.toml
toml_path = "gradle/libs.versions.toml"
with open(toml_path, "r") as f:
    toml = f.read()

if "timber = " not in toml:
    toml = toml.replace('[versions]\n', '[versions]\ntimber = "5.0.1"\n')
    toml = toml.replace('[libraries]\n', '[libraries]\ntimber = { group = "com.jakewharton.timber", name = "timber", version.ref = "timber" }\n')
    with open(toml_path, "w") as f:
        f.write(toml)

# 2. Update build.gradle.kts
gradle_path = "app/build.gradle.kts"
with open(gradle_path, "r") as f:
    gradle = f.read()

if "implementation(libs.timber)" not in gradle:
    gradle = gradle.replace('dependencies {\n', 'dependencies {\n  implementation(libs.timber)\n')
    with open(gradle_path, "w") as f:
        f.write(gradle)

# 3. Update DocMorphApplication.kt
app_path = "app/src/main/java/com/example/DocMorphApplication.kt"
with open(app_path, "r") as f:
    app_kt = f.read()

if "Timber.plant" not in app_kt:
    app_kt = app_kt.replace('import android.app.Application', 'import android.app.Application\nimport timber.log.Timber')
    app_kt = app_kt.replace('super.onCreate()', 'super.onCreate()\n        Timber.plant(Timber.DebugTree())')
    with open(app_path, "w") as f:
        f.write(app_kt)

# 4. Update ExportEngine.kt
ee_path = "app/src/main/java/com/example/domain/services/export/ExportEngine.kt"
with open(ee_path, "r") as f:
    ee = f.read()

if "import timber.log.Timber" not in ee:
    ee = ee.replace('import java.io.File', 'import java.io.File\nimport timber.log.Timber')

ee = ee.replace(
    'com.example.utils.AppLogger.i("ExportEngine", "Exporting project $projectName as ${if (isPdf) "PDF" else "HTML"}")',
    'Timber.i("Exporting project %s as %s", projectName, if (isPdf) "PDF" else "HTML")\n            com.example.utils.AppLogger.i("ExportEngine", "Exporting project $projectName as ${if (isPdf) "PDF" else "HTML"}")'
)

ee = ee.replace(
    'var baseDir = File(documentsDir, "aipdfs/$safeName")',
    'var baseDir = File(documentsDir, "aipdfs/$safeName")\n            Timber.d("Target base directory: %s", baseDir.absolutePath)'
)

ee = ee.replace(
    'val htmlString = htmlConverter.convert(document)',
    'Timber.d("Starting HTML conversion for document: %s", document.title)\n                val htmlString = htmlConverter.convert(document)\n                Timber.d("HTML conversion complete. Length: %d", htmlString.length)'
)

ee = ee.replace(
    'override fun onPageFinished(view: WebView, url: String) {',
    'override fun onPageFinished(view: WebView, url: String) {\n                                Timber.d("WebView onPageFinished triggered for URL: %s", url)'
)

ee = ee.replace(
    'printManager.print(jobName, printAdapter, builder.build())',
    'Timber.d("Initiating PrintManager.print for job: %s", jobName)\n        printManager.print(jobName, printAdapter, builder.build())'
)

ee = ee.replace(
    'com.example.utils.AppLogger.e("ExportEngine", "Render Error: ${e.localizedMessage}", e)',
    'Timber.e(e, "Render Error during export")\n                com.example.utils.AppLogger.e("ExportEngine", "Render Error: ${e.localizedMessage}", e)'
)

ee = ee.replace(
    'com.example.utils.AppLogger.e("ExportEngine", "Failed: ${e.localizedMessage}", e)',
    'Timber.e(e, "Failed to export project")\n            com.example.utils.AppLogger.e("ExportEngine", "Failed: ${e.localizedMessage}", e)'
)

with open(ee_path, "w") as f:
    f.write(ee)
