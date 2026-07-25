import os
import sys
import zipfile
import tempfile
import requests


def main():
    cookie_string = os.getenv("LANZOU_COOKIE")
    
    if not cookie_string:
        print("Error: LANZOU_COOKIE environment variable is required")
        sys.exit(1)
    
    cookie = {}
    for item in cookie_string.split(';'):
        item = item.strip()
        if '=' in item:
            key, value = item.split('=', 1)
            cookie[key] = value
    
    if 'ylogin' not in cookie or 'phpdisk_info' not in cookie:
        print("Error: Cookie must contain ylogin and phpdisk_info")
        sys.exit(1)
    
    html_file = "bdime_ie.html"
    if not os.path.exists(html_file):
        print(f"Error: {html_file} not found")
        sys.exit(1)
    
    with tempfile.NamedTemporaryFile(suffix=".zip", delete=False) as temp_file:
        zip_path = temp_file.name
    
    with zipfile.ZipFile(zip_path, 'w') as zipf:
        zipf.write(html_file, os.path.basename(html_file))
    
    print(f"Packed {html_file} to {zip_path}")
    
    session = requests.Session()
    session.cookies.update(cookie)
    
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Referer': 'https://pc.woozooo.com/',
        'Origin': 'https://pc.woozooo.com'
    }
    
    try:
        print("Testing login status...")
        resp = session.get('https://pc.woozooo.com/mydisk.php', headers=headers, timeout=30)
        if '登录' in resp.text or '请登录' in resp.text:
            print("Error: Cookie invalid, please refresh your cookie")
            print(f"Response status: {resp.status_code}")
            print(f"Response length: {len(resp.text)}")
            sys.exit(1)
        print("Login status: OK")
        
        print("Getting upload params...")
        resp = session.get('https://pc.woozooo.com/mydisk.php', headers=headers, timeout=30)
        
        import re
        t_match = re.search(r"'t'\s*:\s*'([^']+)'", resp.text)
        k_match = re.search(r"'k'\s*:\s*'([^']+)'", resp.text)
        fid_match = re.search(r"'fid'\s*:\s*(\d+)", resp.text)
        
        if not t_match or not k_match or not fid_match:
            print("Error: Failed to extract upload parameters")
            print("Trying fallback params...")
            t = ''
            k = ''
            fid = '-1'
        else:
            t = t_match.group(1)
            k = k_match.group(1)
            fid = fid_match.group(1)
            print(f"Extracted params: t={t[:20]}..., k={k[:20]}..., fid={fid}")
        
        upload_url = 'https://pc.woozooo.com/fileup.php'
        
        files = {
            'file': ('bdime_ie.zip', open(zip_path, 'rb'), 'application/zip')
        }
        
        data = {
            'task': '1',
            'folder_id': fid,
            't': t,
            'k': k,
            'up_from': '1',
            '_upfile': '1',
            'format': 'json'
        }
        
        print("Uploading file...")
        resp = session.post(upload_url, files=files, data=data, headers=headers, timeout=60)
        
        print(f"Upload response status: {resp.status_code}")
        print(f"Upload response: {resp.text[:500]}")
        
        if resp.status_code == 200:
            try:
                result = resp.json()
                if result.get('zt') == 1:
                    print("Upload successful!")
                else:
                    print(f"Upload failed: {result.get('info', 'Unknown error')}")
                    sys.exit(1)
            except:
                if '成功' in resp.text:
                    print("Upload successful!")
                else:
                    print(f"Upload response: {resp.text}")
                    print("Upload may have failed")
                    sys.exit(1)
        else:
            print(f"Upload failed with status {resp.status_code}")
            sys.exit(1)
            
    finally:
        if os.path.exists(zip_path):
            os.remove(zip_path)


if __name__ == "__main__":
    main()