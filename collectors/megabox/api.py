from __future__ import annotations

import json
import time
from typing import Any

import requests

MEGABOX_BASE_URL = "https://www.megabox.co.kr"
MASTER_LIST_URL = (
    "https://www.megabox.co.kr/on/oh/ohb/PlayTime/selectPlayTimeMasterList.do"
)
SCHEDULE_URL = "https://www.megabox.co.kr/on/oh/ohc/Brch/schedulePage.do"
SEAT_URL = "https://www.megabox.co.kr/on/oh/ohz/PcntSeatChoi/selectSeatList.do"

DEFAULT_HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/135.0.0.0 Safari/537.36"
    ),
    "Content-Type": "application/json",
    "X-Requested-With": "XMLHttpRequest",
}


class MegaboxApiClient:
    def __init__(self, headers: dict[str, str] | None = None) -> None:
        self.headers = {**DEFAULT_HEADERS, **(headers or {})}
        self.session = requests.Session()
        self.session.headers.update(self.headers)
        # Megabox sometimes needs initial cookies to not block API calls
        try:
            self.session.get(MEGABOX_BASE_URL, timeout=10)
        except Exception:
            pass

    def _refresh_session(self) -> None:
        try:
            self.session.get(MEGABOX_BASE_URL, timeout=10)
        except Exception:
            pass

    def _post_json(self, url: str, payload: dict[str, Any]) -> dict[str, Any]:
        last_error: Exception | None = None
        for attempt in range(5):
            try:
                response = self.session.post(url, json=payload, timeout=30)
                text = response.text.strip()
                if not text or text.startswith("<") or text.startswith("Workload"):
                    raise RuntimeError(f"Megabox API returned invalid response for {url}: {text[:100]}")
                return response.json()
            except Exception as e:
                last_error = e
                if attempt == 4:
                    raise
                self._refresh_session()
                time.sleep(1.5 + attempt)
        if last_error is not None:
            raise last_error
        return {}

    def fetch_master(self, play_de: str) -> dict[str, Any]:
        return self._post_json(MASTER_LIST_URL, {"playDe": play_de})

    def fetch_schedule(
        self,
        movie_no: str,
        play_de: str,
        area_cd: str,
        first_at: str = "Y",
        master_type: str = "movie",
    ) -> dict[str, Any]:
        payload = {
            "masterType": master_type,
            "movieNo": movie_no,
            "playDe": play_de,
            "areaCd": area_cd,
            "firstAt": first_at,
        }
        return self._post_json(SCHEDULE_URL, payload)

    def fetch_seats(self, play_schdl_no: str, brch_no: str) -> dict[str, Any]:
        payload = {"playSchdlNo": play_schdl_no, "brchNo": brch_no}
        return self._post_json(SEAT_URL, payload)
