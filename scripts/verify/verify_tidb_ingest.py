from __future__ import annotations

import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from collectors.common.tidb import connect_tidb, load_tidb_config


TABLES = ["movies", "theaters", "screens", "showtimes", "showtime_prices", "seat_snapshots", "seat_snapshot_items"]


def main() -> int:
    config = load_tidb_config()
    print(f"verifying {config.safe_summary()}")

    with connect_tidb(config) as conn:
        with conn.cursor() as cursor:
            for table in TABLES:
                cursor.execute(f"SELECT COUNT(*) FROM `{table}`")
                count = cursor.fetchone()[0]
                print(f"{table}={count}")

            cursor.execute(
                """
                SELECT provider_code, COUNT(*), COUNT(DISTINCT external_showtime_key)
                FROM showtimes
                GROUP BY provider_code
                ORDER BY provider_code
                """
            )
            for provider_code, total_count, distinct_count in cursor.fetchall():
                print(
                    f"showtimes_by_provider[{provider_code}]="
                    f"total:{total_count},distinct_keys:{distinct_count}"
                )

            cursor.execute(
                """
                SELECT COUNT(*)
                FROM (
                  SELECT provider_code, external_showtime_key
                  FROM showtimes
                  GROUP BY provider_code, external_showtime_key
                  HAVING COUNT(*) > 1
                ) duplicates
                """
            )
            duplicate_groups = cursor.fetchone()[0]
            print(f"duplicate_showtime_key_groups={duplicate_groups}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
