import os

file_path = "java/com/example/ui/screens/viewer/NotesViewerScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

start_marker = "// Dedicated Fast Save Button"
end_marker = "// Font-size Dropdown Control"

if start_marker in content and end_marker in content:
    start_idx = content.find(start_marker)
    end_idx = content.find(end_marker)
    
    new_button = """// Dedicated Search Toggle Button
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSearchOpen) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSearchOpen) Color(0xFFBFDBFE) else Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable { onToggleSearch() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchOpen) Color(0xFF2563EB) else Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Search",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSearchOpen) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSearchOpen) Color(0xFF2563EB) else Color(0xFF475569)
                            )
                        }
                    }

                    """
    
    new_content = content[:start_idx] + new_button + content[end_idx:]
    
    with open(file_path, "w") as f:
        f.write(new_content)
    print("✅ Replaced redundant Save button with sleek Search button!")
else:
    print("⚠️ Could not find markers.")
