import sys

with open('/app/applet/app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

permissions_to_add = """    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
"""
content = content.replace('    <uses-permission android:name="android.permission.INTERNET" />', permissions_to_add + '    <uses-permission android:name="android.permission.INTERNET" />')

service_to_add = """
        <service
            android:name="androidx.work.impl.foreground.SystemForegroundService"
            android:foregroundServiceType="dataSync"
            tools:node="merge" />
"""
content = content.replace('    </application>', service_to_add + '    </application>')

with open('/app/applet/app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)
print("Updated AndroidManifest.xml")
