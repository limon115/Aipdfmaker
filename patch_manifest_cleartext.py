import os

filepath = "app/src/main/AndroidManifest.xml"

if os.path.exists(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    if 'android:usesCleartextTraffic="true"' not in content:
        old_tag = '<application android:name=".DocMorphApplication"'
        new_tag = '<application android:name=".DocMorphApplication"\n        android:usesCleartextTraffic="true"'
        
        content = content.replace(old_tag, new_tag)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print("✅ Successfully injected CleartextTraffic permission into Manifest.")
    else:
        print("⏭️ Manifest already patched.")
else:
    print("❌ Manifest not found.")
