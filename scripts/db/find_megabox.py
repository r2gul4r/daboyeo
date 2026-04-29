import sys
import datetime
from pathlib import Path
PROJECT_ROOT = Path(__file__).resolve().parents[2]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from collectors.megabox.api import MegaboxApiClient

def main():
    api = MegaboxApiClient()
    today = datetime.datetime.now().strftime("%Y%m%d")
    res = api.fetch_master(today)
    movies = res.get('movieList', [])
    print("Movies:")
    for m in movies[:5]:
        print(f"Title: {m.get('movieNm')}, MovieNo: {m.get('movieNo')}")
    
    brchs = res.get('brchList', [])
    print("First 10 theaters:")
    for b in brchs[:10]:
        print(f"Name: {b.get('brchNm')}, AreaCd: {b.get('areaCd')}")

if __name__ == "__main__":
    main()
