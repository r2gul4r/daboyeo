import sys
from pathlib import Path
from datetime import datetime, timedelta
import json
import time

# Add project root to sys.path
PROJECT_ROOT = Path(__file__).resolve().parents[2]
sys.path.append(str(PROJECT_ROOT))

from collectors.cgv.collector import CgvCollector
from collectors.lotte.collector import LotteCinemaCollector
from collectors.megabox.collector import MegaboxCollector
from collectors.common.tidb import connect_tidb

def sanitize_date(date_str):
    if not date_str:
        return None
    # Keep only digits and hyphens, and limit to 10 chars
    sanitized = "".join(c for c in str(date_str) if c.isdigit() or c == "-")
    if len(sanitized) > 10:
        sanitized = sanitized[:10]
    if len(sanitized) < 8: # Not a valid date
        return None
    return sanitized

def upsert_movies(conn, records):
    if not records: return
    with conn.cursor() as cursor:
        sql = """
        INSERT INTO movies (
            provider_code, external_movie_id, representative_movie_id, title_ko, title_en,
            age_rating, runtime_minutes, release_date, booking_rate, box_office_rank,
            poster_url, raw_json, last_collected_at
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())
        ON DUPLICATE KEY UPDATE
            representative_movie_id = VALUES(representative_movie_id),
            title_ko = VALUES(title_ko),
            title_en = VALUES(title_en),
            age_rating = VALUES(age_rating),
            runtime_minutes = VALUES(runtime_minutes),
            release_date = VALUES(release_date),
            booking_rate = VALUES(booking_rate),
            box_office_rank = VALUES(box_office_rank),
            poster_url = VALUES(poster_url),
            raw_json = VALUES(raw_json),
            last_collected_at = VALUES(last_collected_at)
        """
        data = []
        for r in records:
            rel_date = sanitize_date(r.get('release_date'))
            data.append((
                r['provider'], r['movie_no'], r.get('representative_movie_id'),
                r['movie_name'], r.get('movie_name_en'), r.get('age_rating'),
                r.get('runtime_minutes'), rel_date, r.get('booking_rate'),
                r.get('box_office_rank'), r.get('poster_url'), 
                json.dumps(r.get('raw', {}), ensure_ascii=False)
            ))
        cursor.executemany(sql, data)
    conn.commit()

def sanitize_datetime(date_str, time_str):
    if not date_str or not time_str:
        return None
    try:
        hour, minute = map(int, time_str.split(':'))
        dt = datetime.strptime(date_str, "%Y-%m-%d")
        if hour >= 24:
            dt += timedelta(days=hour // 24)
            hour %= 24
        return dt.replace(hour=hour, minute=minute).strftime("%Y-%m-%d %H:%M:%00")
    except:
        return f"{date_str} {time_str}:00" # Fallback

def upsert_showtimes(conn, records):
    if not records: return
    with conn.cursor() as cursor:
        sql = """
        INSERT INTO showtimes (
            provider_code, external_showtime_key, movie_title, theater_name,
            external_movie_id, external_theater_id, external_screen_id,
            screen_name, screen_type, show_date, starts_at, ends_at,
            total_seat_count, remaining_seat_count, booking_url, raw_json, last_collected_at
        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, NOW())
        ON DUPLICATE KEY UPDATE
            remaining_seat_count = VALUES(remaining_seat_count),
            raw_json = VALUES(raw_json),
            last_collected_at = VALUES(last_collected_at)
        """
        data = []
        for r in records:
            # Normalize play_date to YYYY-MM-DD if it is YYYYMMDD
            p_date = str(r['play_date'])
            if len(p_date) == 8 and "-" not in p_date:
                p_date = f"{p_date[:4]}-{p_date[4:6]}-{p_date[6:]}"
            
            starts_at = sanitize_datetime(p_date, r.get('start_time'))
            ends_at = sanitize_datetime(p_date, r.get('end_time'))

            data.append((
                r['provider'], str(r.get('play_schedule_no') or r.get('booking_key', {}).get('play_sequence') or r.get('play_sequence')),
                r['movie_name'], r['cinema_name'],
                str(r['movie_no']), str(r['cinema_id']), str(r.get('screen_id') or ""),
                r.get('screen_name'), r.get('screen_type'), p_date,
                starts_at, ends_at,
                r.get('total_seat_count'), r.get('remaining_seat_count'),
                r.get('booking_url'), json.dumps(r.get('raw', {}), ensure_ascii=False)
            ))
        cursor.executemany(sql, data)
    conn.commit()

def collect_megabox(conn, days=1):
    print("--- Starting Megabox Collection ---")
    collector = MegaboxCollector()
    for i in range(days):
        date_str = (datetime.now() + timedelta(days=i)).strftime("%Y%m%d")
        print(f"Date: {date_str}")
        
        # 1. Update movies
        movies = collector.build_movie_records(date_str)
        upsert_movies(conn, movies)
        print(f"Upserted {len(movies)} movies.")
        
        # 2. Get areas
        areas = collector.fetch_areas(date_str)
        area_codes = sorted(list(set([a['areaCd'] for a in areas if a.get('areaCd')])))
        # Prioritize Seoul (Area 10)
        if "10" in area_codes:
            area_codes.remove("10")
            area_codes.insert(0, "10")
        
        for area_cd in area_codes:
            for movie in movies:
                try:
                    schedules = collector.build_schedule_records(movie['movie_no'], date_str, area_cd)
                    if schedules:
                        # Map internal fields to our schema-friendly names if needed
                        for s in schedules:
                            s['cinema_id'] = s['branch_no']
                            s['cinema_name'] = s['branch_name']
                        upsert_showtimes(conn, schedules)
                        print(f"  Added {len(schedules)} schedules for movie {movie['movie_name']} in area {area_cd}")
                    time.sleep(0.1)
                except Exception as e:
                    print(f"  Error for movie {movie['movie_name']} in area {area_cd}: {e}")

def collect_lotte(conn, days=1):
    print("--- Starting Lotte Cinema Collection ---")
    collector = LotteCinemaCollector()
    
    # Lotte's master page gives movies and cinemas
    master = collector.fetch_ticketing_page()
    movies = collector.build_movie_records()
    upsert_movies(conn, movies)
    print(f"Upserted {len(movies)} movies.")
    
    cinemas = collector.build_cinema_records()
    # Filter for some major cinemas or use all
    target_cinemas = cinemas # Let's try all
    
    for i in range(days):
        date_str = (datetime.now() + timedelta(days=i)).strftime("%Y-%m-%d")
        print(f"Date: {date_str}")
        
        for movie in movies:
            movie_code = movie['movie_no']
            for cinema in target_cinemas:
                selector = collector.build_cinema_selector(cinema)
                try:
                    schedules = collector.build_schedule_records(date_str, selector, movie_code)
                    if schedules:
                        upsert_showtimes(conn, schedules)
                        print(f"  Added {len(schedules)} schedules for movie {movie['movie_name']} at {cinema['cinema_name']}")
                    time.sleep(0.1)
                except Exception as e:
                    print(f"  Error for movie {movie['movie_name']} at {cinema['cinema_name']}: {e}")

def main():
    conn = connect_tidb()
    try:
        # For demo/initial load, just do 1 day to see if it works
        collect_megabox(conn, days=1)
        collect_lotte(conn, days=1)
        print("All collection tasks completed!")
    finally:
        conn.close()

if __name__ == "__main__":
    main()
