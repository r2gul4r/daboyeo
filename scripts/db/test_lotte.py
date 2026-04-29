import sys
from pathlib import Path
PROJECT_ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(PROJECT_ROOT))

from collectors.lotte.collector import LotteCinemaCollector
import datetime
import json

c = LotteCinemaCollector()
res = c.collect_bundle(
    play_date=datetime.date.today().strftime('%Y%m%d'),
    cinema_selector="1|1|1013",
    representation_movie_code="21252"
)
print(json.dumps(res, ensure_ascii=False)[:500])
