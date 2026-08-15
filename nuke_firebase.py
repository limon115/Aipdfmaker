import os
import glob

# Find all gradle files in the project
files = glob.glob('**/*.gradle', recursive=True) + glob.glob('**/*.gradle.kts', recursive=True)

found_cockroach = False

for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    with open(file_path, 'w', encoding='utf-8') as f:
        for line in lines:
            # If the line contains the plugin name but isn't already commented out
            if ('google-services' in line or 'google.gms' in line) and not line.strip().startswith('//'):
                f.write('// ' + line)
                found_cockroach = True
                print(f"💥 NUKED a hidden Firebase line in: {file_path}")
                print(f"   -> {line.strip()}")
            else:
                f.write(line)

if not found_cockroach:
    print("⚠️ No new lines found. It might be applied indirectly.")
else:
    print("🟢 Firebase eradicated completely from all build files.")
