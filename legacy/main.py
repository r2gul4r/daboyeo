"""
Legacy FastAPI entrypoint kept for reference only.

The active backend for this repository is the Spring Boot app under backend/.
This file was moved out of the repository root so it is not mistaken for the
current runtime entrypoint.
"""

import json
import os
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from pathlib import Path

from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware


def load_env():
    env_path = Path(".env")
    if env_path.exists():
        for line in env_path.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.startswith("#"):
                key, val = line.split("=", 1)
                os.environ[key.strip()] = val.strip().strip('"').strip("'")


load_env()

current_dir = Path(__file__).parent.parent
sys.path.append(str(current_dir))

from collectors import CgvCollector, LotteCinemaCollector, MegaboxCollector
from map.location_service import THEATER_DB_PATH, calculate_distance, get_nearest_theaters

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/api/config")
async def get_config():
    return {"kakao_key": os.environ.get("KAKAO_MAP_API_KEY", "")}


@app.post("/api/theaters/register")
async def register_theaters(theaters: list[dict]):
    if not THEATER_DB_PATH.exists():
        with open(THEATER_DB_PATH, "w", encoding="utf-8") as f:
            json.dump([], f)

    with open(THEATER_DB_PATH, "r", encoding="utf-8") as f:
        existing_theaters = json.load(f)

    def normalized_name(value: str) -> str:
        return "".join(str(value or "").upper().split())

    new_count = 0
    updated_count = 0
    for nt in theaters:
        if "lat" not in nt or "lng" not in nt:
            continue

        try:
            lat = float(nt["lat"])
            lng = float(nt["lng"])
        except (TypeError, ValueError):
            continue

        name = str(nt.get("name", "")).strip()
        if not name:
            continue

        incoming_norm_name = normalized_name(name)

        duplicate = next((
            et for et in existing_theaters
            if normalized_name(et.get("name", "")) == incoming_norm_name
            or calculate_distance(et["lat"], et["lng"], lat, lng) < 0.1
        ), None)

        provider = str(nt.get("provider") or "ETC").upper()
        if provider == "ETC":
            if "CGV" in name.upper():
                provider = "CGV"
            elif "롯데시네마" in name or "LOTTE" in name.upper():
                provider = "LOTTE"
            elif "메가박스" in name or "MEGA" in name.upper():
                provider = "MEGA"

        incoming_code = str(nt.get("code") or "").strip()
        if incoming_code == "9999":
            incoming_code = ""

        if duplicate:
            changed = False

            if duplicate.get("provider") in (None, "", "ETC") and provider != "ETC":
                duplicate["provider"] = provider
                changed = True

            if str(duplicate.get("code", "")).strip() in ("", "9999") and incoming_code:
                duplicate["code"] = incoming_code
                changed = True

            for field in ("source", "external_id", "place_url", "address", "phone"):
                incoming_value = str(nt.get(field) or "").strip()
                if incoming_value and duplicate.get(field) != incoming_value:
                    duplicate[field] = incoming_value
                    changed = True

            if changed:
                updated_count += 1
            continue

        new_theater = {
            "provider": provider,
            "code": incoming_code,
            "name": name,
            "lat": lat,
            "lng": lng,
        }

        for field in ("source", "external_id", "place_url", "address", "phone"):
            incoming_value = str(nt.get(field) or "").strip()
            if incoming_value:
                new_theater[field] = incoming_value

        existing_theaters.append(new_theater)
        new_count += 1

    if new_count > 0 or updated_count > 0:
        with open(THEATER_DB_PATH, "w", encoding="utf-8") as f:
            json.dump(existing_theaters, f, indent=4, ensure_ascii=False)

    return {
        "status": "success",
        "new_registered": new_count,
        "updated_existing": updated_count,
        "total": len(existing_theaters),
    }


@app.get("/api/theaters/all")
async def get_all_theaters():
    if not THEATER_DB_PATH.exists():
        return []
    with open(THEATER_DB_PATH, "r", encoding="utf-8") as f:
        return json.load(f)


@app.get("/api/live/nearby")
async def get_live_nearby_showtimes(
    lat: float = Query(37.5015),
    lng: float = Query(127.0263),
    date: str = Query(default=datetime.now().strftime("%Y%m%d")),
):
    nearby_theaters = get_nearest_theaters(lat, lng)
    if not nearby_theaters:
        return {"results": [], "count": 0, "theaters": [], "theaters_searched": []}

    results = []
    lotte_date = datetime.strptime(date, "%Y%m%d").strftime("%Y-%m-%d")

    mega_col = MegaboxCollector()
    lotte_col = LotteCinemaCollector()

    cgv_col = None
    try:
        cgv_col = CgvCollector()
    except Exception as e:
        print(f"CGV collector init failed (secret might be missing): {e}")

    with ThreadPoolExecutor(max_workers=10) as executor:
        future_to_theater = {}
        for theater in nearby_theaters:
            theater_code = str(theater.get("code") or "").strip()
            if not theater_code or theater_code == "9999":
                continue

            if theater["provider"] == "MEGA":
                future_to_theater[executor.submit(mega_col.build_schedule_records, movie_no="", play_de=date, area_cd="")] = theater
            elif theater["provider"] == "LOTTE":
                selector = f"1|1|{theater_code}"
                future_to_theater[executor.submit(lotte_col.build_schedule_records, play_date=lotte_date, cinema_selector=selector, representation_movie_code="")] = theater
            elif theater["provider"] == "CGV" and cgv_col:
                future_to_theater[executor.submit(cgv_col.build_schedule_records, site_no=theater_code, scn_ymd=date, mov_no="")] = theater

        for future in as_completed(future_to_theater):
            theater = future_to_theater[future]
            try:
                data = future.result()
                if not data:
                    continue
                for item in data:
                    item["theater_name"] = theater["name"]
                    item["provider"] = theater["provider"]
                    item["distance"] = theater["distance"]
                    item["lat"] = theater["lat"]
                    item["lng"] = theater["lng"]
                    if "start_time" in item and ":" not in str(item["start_time"]):
                        t_str = str(item["start_time"])
                        if len(t_str) == 4:
                            item["start_time"] = f"{t_str[:2]}:{t_str[2:]}"
                    results.append(item)
            except Exception as e:
                print(f"Error collecting from {theater['name']}: {e}")

    return {
        "results": results,
        "count": len(results),
        "theaters": nearby_theaters,
        "theaters_searched": [theater["name"] for theater in nearby_theaters],
    }


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)
