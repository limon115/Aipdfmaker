import os, re

workflow_path = ".github/workflows/build.yml"
if os.path.exists(workflow_path):
    with open(workflow_path, "r") as f:
        content = f.read()

    # Strip out the entire Setup Tectonic TeX Bundle step
    content = re.sub(
        r"\s*- name: Setup Tectonic TeX Bundle[\s\S]*?(?=\s*- name: Build Static Dependencies)", 
        "\n", 
        content
    )
    
    with open(workflow_path, "w") as f:
        f.write(content)
    print("✅ SURGICAL LINT REPAIR: Removed 2.7GB tarball extraction from the cloud pipeline.")
