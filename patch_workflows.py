import re

files = [".github/workflows/rebuild.yml", ".github/workflows/build.yml"]

for yaml_path in files:
    try:
        with open(yaml_path, "r") as f:
            yaml = f.read()

        # Update gradle-version to 8.10.2 in build.yml as well if it's 9.3.1
        yaml_new = yaml.replace("gradle wrapper --gradle-version 9.3.1", "gradle wrapper --gradle-version 8.10.2")

        # Strip setup-gradle@v4 if it exists as it has wrapper validation issues
        yaml_new = re.sub(r'\s+- name: Setup Gradle\s+uses: gradle/actions/setup-gradle@v4', '', yaml_new)

        if yaml_new != yaml:
            with open(yaml_path, "w") as f:
                f.write(yaml_new)
            print(f"Patched {yaml_path}")
    except FileNotFoundError:
        pass
