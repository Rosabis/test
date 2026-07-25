import os
import sys
import zipfile
import tempfile
from lanzou.api import LanZouCloud


def main():
    ylogin = os.getenv("LANZOU_YLOGIN")
    phpdisk_info = os.getenv("LANZOU_PHPDISK_INFO")
    target_folder_id = os.getenv("LANZOU_FOLDER_ID", "-1")
    
    if not ylogin or not phpdisk_info:
        print("Error: LANZOU_YLOGIN and LANZOU_PHPDISK_INFO environment variables are required")
        sys.exit(1)
    
    cookie = {
        'ylogin': ylogin,
        'phpdisk_info': phpdisk_info
    }
    
    html_file = "bdime_ie.html"
    if not os.path.exists(html_file):
        print(f"Error: {html_file} not found")
        sys.exit(1)
    
    with tempfile.NamedTemporaryFile(suffix=".zip", delete=False) as temp_file:
        zip_path = temp_file.name
    
    with zipfile.ZipFile(zip_path, 'w') as zipf:
        zipf.write(html_file, os.path.basename(html_file))
    
    print(f"Packed {html_file} to {zip_path}")
    
    lanzou = LanZouCloud()
    lanzou.ignore_limits()
    
    try:
        result = lanzou.login_by_cookie(cookie)
        if result != LanZouCloud.SUCCESS:
            print("Error: Login failed")
            sys.exit(1)
        print("Login successful")
        
        print(f"Uploading to folder ID: {target_folder_id}")
        result = lanzou.upload_file(zip_path, int(target_folder_id))
        
        if result == LanZouCloud.SUCCESS:
            print("Upload successful")
        else:
            print(f"Upload failed: {result}")
            sys.exit(1)
    finally:
        if os.path.exists(zip_path):
            os.remove(zip_path)


if __name__ == "__main__":
    main()