import urllib.request
url = "https://raw.githubusercontent.com/simple-icons/simple-icons/master/icons/adobeacrobatreader.svg"
try:
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    html = urllib.request.urlopen(req).read().decode('utf-8')
    print(html)
except Exception as e:
    print(e)
