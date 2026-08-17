import re

yaml_path = ".github/workflows/rebuild.yml"
with open(yaml_path, "r") as f:
    yaml = f.read()

# Add wrapper-validation step before gradle
yaml_new = yaml.replace(
    "- name: Generate Gradle Wrapper (Cloud Execution)",
    "- name: Validate Gradle Wrapper\n        uses: gradle/actions/wrapper-validation@v4\n\n      - name: Generate Gradle Wrapper (Cloud Execution)"
)

# Fix issue where setup-java might be interfering if gradle cache is on without setup-gradle
yaml_new = yaml_new.replace("cache: 'gradle'", "")

with open(yaml_path, "w") as f:
    f.write(yaml_new)
print("Patched rebuild.yml again")
