import os, re
path = "app/src/main/rust/tectonic-jni/Cargo.toml"
if os.path.exists(path):
    with open(path, "r") as f:
        lines = f.readlines()
    
    with open(path, "w") as f:
        for line in lines:
            # Safely target ONLY the tectonic main dependency
            if line.strip().startswith("tectonic ="):
                if "version" in line:
                    line = re.sub(r'version\s*=\s*"[^"]+"', 'version = "*"', line)
                else:
                    line = re.sub(r'"[^"]+"', '"*"', line)
            f.write(line)
    print("✅ SURGICAL LINT REPAIR: Unlocked Tectonic to latest version ('*').")
else:
    print("❌ Cargo.toml not found")
