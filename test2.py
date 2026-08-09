import urllib.request
import urllib.parse
import json

url = "https://html.duckduckgo.com/html/?q=" + urllib.parse.quote("Android WebView createPrintDocumentAdapter silently save pdf ParcelFileDescriptor LayoutResultCallback")
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req) as response:
        print(response.read().decode('utf-8')[:2000])
except Exception as e:
    print(e)
