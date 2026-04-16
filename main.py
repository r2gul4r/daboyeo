import json
import math
import os
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import datetime
from pathlib import Path

from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware
import requests

# --- .env 로딩 및 경로 설정 ---
def load_env():
    env_path = Path(".env")
    if env_path.exists():
        for line in env_path.read_text(encoding="utf-8").splitlines():
            if "=" in line and not line.startswith("#"):
                key, val = line.split("=", 1)
                os.environ[key.strip()] = val.strip().strip('"').strip("'")

load_env()

# collectors 모듈 임포트를 위해 경로 추가
current_dir = Path(__file__).parent
sys.path.append(str(current_dir))

# 로컬 서비스 및 콜렉터 임포트
from collectors import CgvCollector, LotteCinemaCollector, MegaboxCollector
from map.location_service import THEATER_DB_PATH, get_nearest_theaters, calculate_distance

app = FastAPI()

# --- CORS 설정 ---
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/api/config")
async def get_config():
    return {
        "kakao_key": os.environ.get("KAKAO_MAP_API_KEY", "")
    }

# ---------------------------
# 엔드포인트
# ---------------------------

@app.post("/api/theaters/register")
async def register_theaters(theaters: list[dict]):
    """
    프론트엔드에서 발견한 주변 극장 정보를 DB에 자동 등록
    """
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

        # 중복 체크 (이름 또는 매우 가까운 거리 기준)
        duplicate = next((
            et for et in existing_theaters
            if normalized_name(et.get("name", "")) == incoming_norm_name
            or calculate_distance(et['lat'], et['lng'], lat, lng) < 0.1
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
            "lng": lng
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
        "total": len(existing_theaters)
    }

@app.get("/api/theaters/all")
async def get_all_theaters():
    """
    등록된 모든 극장 정보 반환 (지도 클러스터링용)
    """
    if not THEATER_DB_PATH.exists():
        return []
    with open(THEATER_DB_PATH, "r", encoding="utf-8") as f:
        return json.load(f)

@app.get("/api/live/nearby")
async def get_live_nearby_showtimes(
    lat: float = Query(37.5015), 
    lng: float = Query(127.0263),
    date: str = Query(default=datetime.now().strftime("%Y%m%d"))
):
    """
    사용자 위치 기반 주변 극장 실시간 수집 호출 (New Structured Collectors)
    """
    nearby_theaters = get_nearest_theaters(lat, lng)
    if not nearby_theaters:
        return {"results": [], "count": 0, "theaters": [], "theaters_searched": []}

    results = []
    lotte_date = datetime.strptime(date, "%Y%m%d").strftime("%Y-%m-%d")

    # Initialize Collectors
    mega_col = MegaboxCollector()
    lotte_col = LotteCinemaCollector()
    
    # CGV Collector (API_SECRET 체크 포함)
    cgv_col = None
    try:
        cgv_col = CgvCollector()
    except Exception as e:
        print(f"CGV Collector init failed (Secret might be missing): {e}")

    with ThreadPoolExecutor(max_workers=10) as executor:
        future_to_theater = {}
        for t in nearby_theaters:
            theater_code = str(t.get("code") or "").strip()
            if not theater_code or theater_code == "9999":
                continue

            if t['provider'] == "MEGA":
                future_to_theater[executor.submit(mega_col.build_schedule_records, movie_no="", play_de=date, area_cd="")] = t
                # Note: area_cd="" in Megabox shows all for that date if first_at="Y" usually, 
                # but to be sure we should use fetch_schedule with branch_no.
                # For now, let's keep the logic simple or use their branch-specific method.
            elif t['provider'] == "LOTTE":
                # Lotte needs Division|Detail|ID selector
                selector = f"1|1|{theater_code}" # Default guess
                future_to_theater[executor.submit(lotte_col.build_schedule_records, play_date=lotte_date, cinema_selector=selector, representation_movie_code="")] = t
            elif t['provider'] == "CGV" and cgv_col:
                future_to_theater[executor.submit(cgv_col.build_schedule_records, site_no=theater_code, scn_ymd=date, mov_no="")] = t

        for future in as_completed(future_to_theater):
            theater = future_to_theater[future]
            try:
                data = future.result()
                if not data: continue
                # 필드명 매핑 및 메타데이터 추가
                for item in data:
                    item['theater_name'] = theater['name']
                    item['provider'] = theater['provider']
                    item['distance'] = theater['distance']
                    item['lat'] = theater['lat']
                    item['lng'] = theater['lng']
                    # 상영시간(start_time)이 1430 형태일 수 있으므로 14:30으로 보정 (필요시)
                    if 'start_time' in item and ':' not in str(item['start_time']):
                        t_str = str(item['start_time'])
                        if len(t_str) == 4:
                            item['start_time'] = f"{t_str[:2]}:{t_str[2:]}"
                    
                    results.append(item)
            except Exception as e:
                print(f"Error collecting from {theater['name']}: {e}")

    return {
        "results": results,
        "count": len(results),
        "theaters": nearby_theaters,
        "theaters_searched": [t['name'] for t in nearby_theaters]
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
