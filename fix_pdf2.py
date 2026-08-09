import sys

with open('app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'r') as f:
    content = f.read()

target = "printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().build())"
replacement = "printManager.print(jobName, printAdapter, android.print.PrintAttributes.Builder().setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4).setMinMargins(android.print.PrintAttributes.Margins.NO_MARGINS).build())"

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/domain/services/export/ExportEngine.kt', 'w') as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Target not found")
