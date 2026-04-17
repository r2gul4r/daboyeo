import json
import sys
from pathlib import Path

# 프로젝트 루트를 경로에 추가
root_dir = Path(__file__).parent.parent
sys.path.append(str(root_dir))

from collectors import CgvCollector, LotteCinemaCollector, MegaboxCollector

THEATER_DB_PATH = root_dir / "frontend/src/map/theaters.json"

def normalize_name(name):
    if not name: return ""
    name = "".join(name.upper().split())
    # 영화사 명칭 및 불필요한 수식어 제거 하여 매칭률 향상
    for word in ["롯데시네마", "메가박스", "CGV", "씨네드쉐프", "더부티크", "부티크", "(부티크)", "점"]:
        name = name.replace(word, "")
    return name

def update_codes():
    if not THEATER_DB_PATH.exists():
        print("theaters.json 파일을 찾을 수 없습니다.")
        return

    with open(THEATER_DB_PATH, "r", encoding="utf-8") as f:
        theaters = json.load(f)

    print(f"현재 로드된 극장 수: {len(theaters)}")

    # 1. CGV 실시간 목록 가져오기
    print("CGV 지점 정보 수집 중...")
    try:
        cgv = CgvCollector()
        cgv_sites = cgv.fetch_regions_and_sites().get("sites", [])
        cgv_map = {normalize_name(s.get("siteNm", "")): s.get("siteNo") for s in cgv_sites}
        print(f"CGV 수집 완료: {len(cgv_map)}개 지점")
    except Exception as e:
        print(f"CGV 수집 실패: {e}")
        cgv_map = {}

    # 2. 롯데시네마 실시간 목록 가져오기
    print("롯데시네마 지점 정보 수집 중...")
    try:
        lotte = LotteCinemaCollector()
        lotte_sites = lotte.fetch_cinemas()
        lotte_map = {normalize_name(s.get("CinemaNameKR", "")): s.get("CinemaID") for s in lotte_sites}
        print(f"롯데시네마 수집 완료: {len(lotte_map)}개 지점")
    except Exception as e:
        print(f"롯데시네마 수집 실패: {e}")
        lotte_map = {}

    # 3. 메가박스 실시간 목록 가져오기
    print("메가박스 지점 정보 수집 중...")
    try:
        from datetime import datetime
        today = datetime.now().strftime("%Y%m%d")
        mega = MegaboxCollector()
        # fetch_areas를 통해 전 지점 정보를 가져옴
        mega_sites = mega.fetch_areas(today)
        # brchNm: 지점명, brchNo: 지점코드
        mega_map = {normalize_name(s.get("brchNm", "")): s.get("brchNo") for s in mega_sites if s.get("brchNm")}
        print(f"메가박스 수집 완료: {len(mega_map)}개 지점")
    except Exception as e:
        print(f"메가박스 수집 실패: {e}")
        mega_map = {}

    # 코드 업데이트 진행
    update_count = 0
    skipped_cgv = False
    for t in theaters:
        name = normalize_name(t.get("name", ""))
        provider = t.get("provider", "").upper()
        current_code = str(t.get("code", ""))

        if current_code != "9999" and current_code != "":
             continue

        real_code = None
        if provider == "CGV":
            if not cgv_map:
                skipped_cgv = True
                continue
            real_code = cgv_map.get(name)
        elif provider == "LOTTE":
            real_code = lotte_map.get(name)
        elif provider == "MEGA":
            real_code = mega_map.get(name)
        
        if real_code:
            t["code"] = str(real_code)
            update_count += 1

    if skipped_cgv:
        print("공지: CGV_API_SECRET이 없어 CGV 지점 정보는 업데이트하지 못했습니다.")
    
    print(f"업데이트 완료: {update_count}개 지점 코드 갱신됨")

    with open(THEATER_DB_PATH, "w", encoding="utf-8") as f:
        json.dump(theaters, f, indent=4, ensure_ascii=False)
    
    print("theaters.json 저장 완료.")

if __name__ == "__main__":
    update_codes()
