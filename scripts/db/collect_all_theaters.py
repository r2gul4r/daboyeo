import sys
from pathlib import Path

# Add project root to sys.path
PROJECT_ROOT = Path(__file__).resolve().parents[2]
sys.path.append(str(PROJECT_ROOT))

from collectors.cgv.collector import CgvCollector
from collectors.lotte.collector import LotteCinemaCollector
from collectors.megabox.collector import MegaboxCollector
from collectors.common.tidb import connect_tidb
import json

def upsert_theaters(conn, records):
    with conn.cursor() as cursor:
        sql = """
        INSERT INTO theaters (
            provider_code, external_theater_id, name, region_code, region_name, 
            address, latitude, longitude, raw_json, last_collected_at
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())
        ON DUPLICATE KEY UPDATE
            name = VALUES(name),
            region_code = VALUES(region_code),
            region_name = VALUES(region_name),
            address = VALUES(address),
            latitude = VALUES(latitude),
            longitude = VALUES(longitude),
            raw_json = VALUES(raw_json),
            last_collected_at = VALUES(last_collected_at)
        """
        data = []
        for r in records:
            data.append((
                r['provider_code'],
                r['external_theater_id'],
                r['name'],
                r.get('region_code'),
                r.get('region_name'),
                r.get('address'),
                r.get('latitude'),
                r.get('longitude'),
                json.dumps(r.get('raw', {}), ensure_ascii=False)
            ))
        cursor.executemany(sql, data)
    conn.commit()

def collect_cgv():
    print("Collecting CGV theaters...")
    collector = CgvCollector()
    sites = collector.build_site_records()
    results = []
    for s in sites:
        results.append({
            'provider_code': 'CGV',
            'external_theater_id': s['site_no'],
            'name': s['site_name'],
            'region_code': s['region_code'],
            'region_name': s['region_name'],
            'address': s['address'],
            'latitude': s['latitude'],
            'longitude': s['longitude'],
            'raw': s['raw']
        })
    print(f"Found {len(results)} CGV theaters.")
    return results

def collect_lotte():
    print("Collecting Lotte Cinema theaters...")
    collector = LotteCinemaCollector()
    cinemas = collector.build_cinema_records()
    results = []
    for c in cinemas:
        results.append({
            'provider_code': 'LOTTE_CINEMA',
            'external_theater_id': str(c['cinema_id']),
            'name': c['cinema_name'],
            'region_code': c['division_code'],
            'region_name': c['detail_division_name'],
            'address': c['address_summary'],
            'latitude': c['latitude'],
            'longitude': c['longitude'],
            'raw': c['raw']
        })
    print(f"Found {len(results)} Lotte Cinema theaters.")
    return results

def collect_megabox():
    print("Collecting Megabox theaters...")
    from datetime import datetime
    today = datetime.now().strftime("%Y%m%d")
    collector = MegaboxCollector()
    areas = collector.build_area_records(today)
    results = []
    for a in areas:
        results.append({
            'provider_code': 'MEGABOX',
            'external_theater_id': str(a['branch_no']),
            'name': a['branch_name'],
            'region_code': a['area_code'],
            'region_name': a['area_name'],
            'address': None, # Megabox master doesn't have address, need to fetch detail if needed
            'latitude': None,
            'longitude': None,
            'raw': a['raw']
        })
    print(f"Found {len(results)} Megabox theaters.")
    return results

def main():
    conn = connect_tidb()
    try:
        all_theaters = []
        
        try:
            all_theaters.extend(collect_cgv())
        except Exception as e:
            print(f"FAILED to collect CGV: {e}")

        try:
            all_theaters.extend(collect_lotte())
        except Exception as e:
            print(f"FAILED to collect Lotte Cinema: {e}")

        try:
            all_theaters.extend(collect_megabox())
        except Exception as e:
            print(f"FAILED to collect Megabox: {e}")
        
        if not all_theaters:
            print("No theaters were collected from any provider.")
            return

        print(f"Total theaters found: {len(all_theaters)}")
        print("Upserting to TiDB...")
        upsert_theaters(conn, all_theaters)
        print("Success!")
    finally:
        conn.close()

if __name__ == "__main__":
    main()
