import os

file_path = ".github/workflows/build.yml"
if os.path.exists(file_path):
    with open(file_path, "r") as f:
        content = f.read()

    # 1. Defuse Landmine 1: Upgrade to JDK 21
    content = content.replace("java-version: '17'", "java-version: '21'")
    content = content.replace("name: Set up JDK 17", "name: Set up JDK 21")

    # 2. Defuse Landmine 2: Inject Dummy google-services.json
    dummy_step = """- name: Stub dummy google-services.json for CI
        run: |
          echo '{
            "project_info": { "project_number": "123456789", "project_id": "ci-dummy-project" },
            "client": [{
              "client_info": { "mobilesdk_app_id": "1:123456789:android:abcdef", "android_client_info": { "package_name": "com.dummy.app" } }
            }]
          }' > app/google-services.json

      - name: Assemble Debug APK with Gradle"""
    
    if "Stub dummy google-services.json" not in content:
        content = content.replace("- name: Assemble Debug APK with Gradle", dummy_step)

    with open(file_path, "w") as f:
        f.write(content)
    print("✅ PREEMPTIVE STRIKE SUCCESS: Upgraded to JDK 21 and stubbed google-services.json!")
else:
    print("❌ Could not find build.yml")
