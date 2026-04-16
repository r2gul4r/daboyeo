import json
import requests
from pathlib import Path

# 스크립트 위치가 map 폴더 내부이므로 상위로 이동 후 frontend 경로 탐색
THEATER_DB_PATH = Path(__file__).parent.parent / "frontend/src/map/theaters.json"

def normalize_name(name):
    """이름 정규화: 공백, 브랜드명, '점' 등 제거"""
    name = str(name).replace(" ", "").upper()
    for brand in ["CGV", "롯데시네마", "메가박스", "LOTTECINEMA", "MEGABOX"]:
        name = name.replace(brand, "")
    if name.endswith("점"): name = name[:-1]
    return name

def get_static_masters():
    """네트워크 차단 시를 대비한 3사 주요 지점 핵심 코드 데이터베이스"""
    return {
        "CGV": {
            "강남": "0056", "강변": "0001", "계양": "0016", "광주터미널": "0090", "구로": "0010",
            "구리": "0134", "대구": "0015", "대학로": "0063", "동대문": "0096", "명동": "0009",
            "목동": "0019", "미아": "0055", "불광": "0030", "상암": "0014", "서면": "0005",
            "송파": "0088", "수원": "0011", "압구정": "0008", "야탑": "0027", "영등포": "0059",
            "오리": "0003", "왕십리": "0074", "용산아이파크몰": "0013", "의정부": "0118",
            "인천": "0002", "일산": "0007", "죽전": "0054", "천안": "0024", "청주": "0026",
            "판교": "0181", "포항": "0046", "피카디리1958": "0103", "하계": "0120", "홍대": "0135"
        },
        "LOTTE": {
            "월드타워": "1004", "건대입구": "1010", "청량리": "1009", "영등포": "1012", "노원": "1005",
            "홍대입구": "1014", "서울대입구": "1016", "신림": "1015", "김포공항": "1002", "수원": "1003",
            "인천터미널": "1017", "부천": "1001", "강남": "1008", "가산디지털": "1013", "신도림": "1020",
            "용산": "1023", "평촌": "1006", "안산": "1007", "광명": "1021", "천안": "1018", "대전": "1019"
        },
        "MEGA": {
            "코엑스": "1351", "강남": "1351", "신촌": "1202", "동대문": "1101", "성수": "1003",
            "목동": "1581", "상암월드컵경기장": "1211", "센트럴": "1372", "하남스타필드": "1311", 
            "고양스타필드": "1051", "백석": "1041", "분당": "1353", "영통": "1651", "송도": "4061",
            "대구신세계": "7011", "해운대": "4801", "대전현대아울렛": "3051", "천안": "3301"
        }
    }

def run_matching():
    if not THEATER_DB_PATH.exists():
        print(f"파일을 찾을 수 없습니다: {THEATER_DB_PATH}")
        return

    with open(THEATER_DB_PATH, "r", encoding="utf-8") as f:
        theaters = json.load(f)

    print("정밀 코드 매칭을 시작합니다...")
    masters = get_static_masters()

    match_count = 0
    already_count = 0
    
    for t in theaters:
        provider = t.get('provider')
        if not provider or provider not in masters: continue
        
        # 코드가 없거나 9999인 것만 업데이트
        if t.get('code') and t['code'] != "9999" and t['code'] != "":
            already_count += 1
            continue

        norm_name = normalize_name(t['name'])
        brand_master = masters[provider]

        # 정밀 대조
        if norm_name in brand_master:
            t['code'] = brand_master[norm_name]
            match_count += 1
        else:
            # 부분 일치 검색 (예: "강남"이 포함된 경우)
            for m_name, m_code in brand_master.items():
                if m_name in norm_name or norm_name in m_name:
                    t['code'] = m_code
                    match_count += 1
                    break

    with open(THEATER_DB_PATH, "w", encoding="utf-8") as f:
        json.dump(theaters, f, indent=4, ensure_ascii=False)

    print("\n" + "="*50)
    print(f"상태 요약:")
    print(f"- 기존 활성 극장: {already_count}개")
    print(f"- 새로 복구된 극장: {match_count}개")
    print(f"이제 {THEATER_DB_PATH}에 실제 상용 코드가 반영되었습니다.")
    print("="*50)

if __name__ == "__main__":
    run_matching()
