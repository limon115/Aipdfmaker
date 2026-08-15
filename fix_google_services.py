import json, os, re

pkg = "com.example.app"
try:
    with open("app/build.gradle", "r") as f:
        m = re.search(r'applicationId\s+["\']([^"\']+)["\']', f.read())
        if m: pkg = m.group(1)
except Exception: 
    pass

gs = "app/google-services.json"
dummy_data = {
  "project_info": {"project_number": "1234567890", "project_id": "limon-rebuild"},
  "client": [{
      "client_info": {
          "mobilesdk_app_id": "1:1234567890:android:abc123def",
          "android_client_info": {"package_name": pkg}
      },
      "api_key": [{"current_key": "DUMMY_KEY_FOR_CLOUD_BUILD"}]
  }],
  "configuration_version": "1"
}

if os.path.exists(gs):
    try:
        with open(gs, "r") as f: data = json.load(f)
        client = data.get("client", [{}])[0]
        if "api_key" not in client:
            client["api_key"] = [{"current_key": "DUMMY_KEY_FOR_CLOUD_BUILD"}]
        elif not client["api_key"][0].get("current_key"):
            client["api_key"][0]["current_key"] = "DUMMY_KEY_FOR_CLOUD_BUILD"
    except Exception: 
        data = dummy_data
else:
    data = dummy_data

with open(gs, "w") as f: json.dump(data, f, indent=2)
print(f"🔥 Successfully patched {gs} for package: {pkg}")
