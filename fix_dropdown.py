import re

files = [
    "app/src/main/java/com/example/ui/screens/settings/AiSettingsScreen.kt",
    "app/src/main/java/com/example/ui/screens/settings/ApiLabScreen.kt"
]

for file in files:
    with open(file, "r") as f:
        content = f.read()

    # In AiSettingsScreen.kt
    content = content.replace(
        "ExposedDropdownMenuBox(\n                expanded = modelDropdownExpanded,\n                onExpandedChange = { modelDropdownExpanded = !modelDropdownExpanded }\n            )",
        "ExposedDropdownMenuBox(\n                expanded = modelDropdownExpanded,\n                onExpandedChange = { modelDropdownExpanded = !modelDropdownExpanded },\n                modifier = Modifier.fillMaxWidth()\n            )"
    )

    # In ApiLabScreen.kt
    content = content.replace(
        "ExposedDropdownMenuBox(\n                expanded = expanded,\n                onExpandedChange = { expanded = !expanded }\n            )",
        "ExposedDropdownMenuBox(\n                expanded = expanded,\n                onExpandedChange = { expanded = !expanded },\n                modifier = Modifier.fillMaxWidth()\n            )"
    )

    with open(file, "w") as f:
        f.write(content)
