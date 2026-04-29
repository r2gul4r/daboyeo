import sys
from pathlib import Path
PROJECT_ROOT = Path(__file__).resolve().parents[2]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

import json

def get_cgv():
    print("=== CGV ===")
    from collectors.cgv.api import CgvApiClient
    api = CgvApiClient()
    res = api.fetch_regions_and_sites()
    sites = res.get("data", {}).get("siteList", [])
    print("Theaters (sample):", [t for t in sites if '강남' in t.get('siteNm', '')])
    movies = api.fetch_movies()
    print("Movies (sample):", movies.get("data", {}).get("movieList", [])[:2])

def get_lotte():
    print("=== LOTTE ===")
    from collectors.lotte.api import LotteCinemaApi
    api = LotteCinemaApi()
    res = api.fetch_ticketing_data()
    cinemas = res.get('Cinemas', {}).get('Items', [])
    print("Theaters (sample):", [c for c in cinemas if '강남' in c.get('CinemaName', '') or '월드타워' in c.get('CinemaName', '')])
    movies = res.get('Movies', {}).get('Items', [])
    print("Movies (sample):", movies[:2])

def get_megabox():
    print("=== MEGABOX ===")
    from collectors.megabox.api import MegaboxApi
    api = MegaboxApi()
    try:
        theaters = api.fetch_theaters()
        print("Theaters (sample):", [t for t in theaters if '코엑스' in t.get('brchNm', '') or '강남' in t.get('brchNm', '')])
    except Exception as e:
        print("Megabox theaters error:", e)
    try:
        movies = api.fetch_movies()
        print("Movies (sample):", movies[:2])
    except Exception as e:
        print("Megabox movies error:", e)

if __name__ == "__main__":
    # get_cgv()
    get_lotte()
    get_megabox()
