import os
import glob

workflow_dir = ".github/workflows"

if os.path.exists(workflow_dir):
    yaml_files = glob.glob(f"{workflow_dir}/*.yml") + glob.glob(f"{workflow_dir}/*.yaml")
    
    if not yaml_files:
        print("❌ No YAML workflow files found in .github/workflows/")
    else:
        for filepath in yaml_files:
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()

            # Surgically inject the non-interactive flag to prevent the 2-hour hang
            if "sudo apt-get install" in content and "DEBIAN_FRONTEND=noninteractive" not in content:
                content = content.replace(
                    "sudo apt-get install", 
                    "sudo DEBIAN_FRONTEND=noninteractive apt-get install"
                )
                
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f"✅ Successfully patched: {filepath}")
            else:
                print(f"⏭️ Skipped: {filepath} (Already patched or no apt-get commands)")
                
    print("🚀 Cloud compilation workflow secured against infinite hangs!")
else:
    print(f"❌ Error: '{workflow_dir}' directory not found. Are you in the project root?")
