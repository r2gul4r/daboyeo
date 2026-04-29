import sys
from pathlib import Path
PROJECT_ROOT = Path(__file__).resolve().parents[2]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))
from collectors.common.tidb import connect_tidb, load_tidb_config

def main():
    config = load_tidb_config()
    with connect_tidb(config) as conn:
        with conn.cursor() as cursor:
            cursor.execute("SELECT show_date, count(*) FROM movie_schedules GROUP BY show_date ORDER BY show_date DESC")
            rows = cursor.fetchall()
            if not rows:
                print("No data in movie_schedules table.")
            for row in rows:
                print(f"{row[0]}: {row[1]} schedules")
if __name__ == "__main__":
    main()
