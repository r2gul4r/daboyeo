from __future__ import annotations

import json
from typing import Any

from scrapling import DynamicFetcher


TIMETABLE_URL = "https://www.megabox.co.kr/booking/timetable"


def text_or_empty(node: Any, selector: str) -> str:
    found = node.css(selector)
    if not found:
        return ""
    first = found[0]
    return first.text.strip() if first and first.text else ""


def main() -> None:
    page = DynamicFetcher.fetch(TIMETABLE_URL, headless=True)

    movie_buttons = page.css("button.on[data-movie-no]")
    movie_button = movie_buttons[0] if movie_buttons else None
    movie_name = movie_button.attrib.get("data-movie-nm", "").strip() if movie_button else ""
    movie_no = movie_button.attrib.get("data-movie-no", "").strip() if movie_button else ""

    active_areas = page.css(".theater-list-box .tab-block li.on a[data-area-cd]")
    active_area = active_areas[0] if active_areas else None
    area_name = active_area.text.strip() if active_area and active_area.text else ""
    area_code = active_area.attrib.get("data-area-cd", "").strip() if active_area else ""

    records: list[dict[str, Any]] = []

    for theater_list in page.css(".theater-list"):
        theater_links = theater_list.css(".theater-area-click a")
        theater_link = theater_links[0] if theater_links else None
        theater_name = theater_link.text.strip() if theater_link and theater_link.text else ""
        theater_detail_url = ""
        if theater_link and theater_link.attrib.get("href"):
            theater_detail_url = f"https://www.megabox.co.kr{theater_link.attrib['href']}"

        for theater_type_box in theater_list.css(".theater-type-box"):
            screen_name = text_or_empty(theater_type_box, ".theater-name")
            total_seats = text_or_empty(theater_type_box, ".theater-type .chair")
            format_name = text_or_empty(theater_type_box, ".theater-type-area")

            for time_cell in theater_type_box.css("td[play-schdl-no]"):
                start_time = text_or_empty(time_cell, ".time")
                remaining_seats = text_or_empty(time_cell, ".chair")

                play_time_paragraphs = [p.text.strip() for p in time_cell.css(".play-time p") if p.text]
                running_time = play_time_paragraphs[0] if play_time_paragraphs else ""
                play_round = play_time_paragraphs[1] if len(play_time_paragraphs) > 1 else ""

                play_date = time_cell.attrib.get("play-de", "").strip()
                play_seq = time_cell.attrib.get("play-seq", "").strip()
                play_schedule_no = time_cell.attrib.get("play-schdl-no", "").strip()
                branch_no = time_cell.attrib.get("brch-no", "").strip()
                theater_no = time_cell.attrib.get("theab-no", "").strip()

                booking_url = (
                    "https://www.megabox.co.kr/on/oh/ohz/PcntSeatChoi/selectPcntSeatChoi.do"
                    f"?playSchdlNo={play_schedule_no}"
                )

                records.append(
                    {
                        "movie_name": movie_name,
                        "movie_no": movie_no,
                        "area_name": area_name,
                        "area_code": area_code,
                        "theater_name": theater_name,
                        "theater_detail_url": theater_detail_url,
                        "screen_name": screen_name,
                        "format_name": format_name,
                        "total_seats_text": total_seats,
                        "remaining_seats_text": remaining_seats,
                        "start_time": start_time,
                        "running_time_text": running_time,
                        "play_round_text": play_round,
                        "play_date": play_date,
                        "play_seq": play_seq,
                        "play_schedule_no": play_schedule_no,
                        "branch_no": branch_no,
                        "theater_no": theater_no,
                        "booking_url_candidate": booking_url,
                    }
                )

    output = {
        "source": "megabox",
        "page_url": TIMETABLE_URL,
        "movie_name": movie_name,
        "movie_no": movie_no,
        "area_name": area_name,
        "area_code": area_code,
        "record_count": len(records),
        "records": records[:20],
    }
    print(json.dumps(output, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
