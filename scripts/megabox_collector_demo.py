from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from collectors.megabox import MegaboxCollector


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    parser = argparse.ArgumentParser(description="Megabox API collector demo")
    parser.add_argument("--play-de", default="20260408", help="Play date in YYYYMMDD")
    parser.add_argument("--movie-no", default="26018000", help="Megabox movie number")
    parser.add_argument("--area-cd", default="10", help="Megabox area code")
    parser.add_argument(
        "--seat-play-schdl-no",
        default="2604081431004",
        help="Seat map play schedule number",
    )
    parser.add_argument("--seat-brch-no", default="1431", help="Seat map branch number")
    parser.add_argument(
        "--mode",
        choices=["bundle", "movies", "areas", "schedules", "seats", "seat-summary"],
        default="bundle",
        help="Output mode",
    )
    parser.add_argument(
        "--output",
        help="Optional path to save JSON output",
    )
    args = parser.parse_args()

    collector = MegaboxCollector()
    if args.mode == "bundle":
        output = collector.collect_bundle(
            play_de=args.play_de,
            movie_no=args.movie_no,
            area_cd=args.area_cd,
            seat_play_schdl_no=args.seat_play_schdl_no,
            seat_brch_no=args.seat_brch_no,
        )
    elif args.mode == "movies":
        output = collector.build_movie_records(args.play_de)
    elif args.mode == "areas":
        output = collector.build_area_records(args.play_de)
    elif args.mode == "schedules":
        output = collector.build_schedule_records(
            movie_no=args.movie_no,
            play_de=args.play_de,
            area_cd=args.area_cd,
        )
    elif args.mode == "seats":
        output = collector.build_seat_records(
            play_schdl_no=args.seat_play_schdl_no,
            brch_no=args.seat_brch_no,
        )
    else:
        output = collector.summarize_seat_map(
            play_schdl_no=args.seat_play_schdl_no,
            brch_no=args.seat_brch_no,
        )

    text = json.dumps(output, ensure_ascii=False, indent=2)
    if args.output:
        Path(args.output).write_text(text, encoding="utf-8")
    print(text)


if __name__ == "__main__":
    main()
