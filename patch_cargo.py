import os, re
path = "app/src/main/rust/tectonic-jni/Cargo.toml"
if os.path.exists(path):
    with open(path, "r") as f:
        c = f.read()
    
    # Safely disable default features to avoid OpenSSL panics
    c = re.sub(r'tectonic\s*=\s*"([^"]+)"', r'tectonic = { version = "\1", default-features = false }', c)
    c = re.sub(r'tectonic\s*=\s*\{\s*version\s*=\s*"([^"]+)"\s*\}', r'tectonic = { version = "\1", default-features = false }', c)
    
    with open(path, "w") as f:
        f.write(c)
    print("✅ Cargo.toml patched: default-features = false")
else:
    print("❌ Cargo.toml not found")
