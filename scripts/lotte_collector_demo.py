from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from collectors.lotte import LotteCinemaCollector


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")

    parser = argparse.ArgumentParser(description="Lotte Cinema API collector demo")
    parser.add_argument(
        "--play-date",
        default="2026-04-08",
        help="Play date in YYYY-MM-DD",
    )
    parser.add_argument(
        "--movie-code",
        default="23816",
        help="Lotte representation movie code",
    )
    parser.add_argument(
        "--cinema-selector",
        default="1|0001|1013",
        help="DivisionCode|DetailDivisionCode|CinemaID",
    )
    parser.add_argument("--seat-cinema-id", type=int, default=1013)
    parser.add_argument("--seat-screen-id", type=int, default=101302)
    parser.add_argument("--seat-play-sequence", type=int, default=1)
    parser.add_argument("--seat-screen-division-code", type=int, default=100)
    parser.add_argument(
        "--mode",
        choices=["bundle", "movies", "cinemas", "play-dates", "schedules", "seats", "seat-summary"],
        default="bundle",
    )
    parser.add_argument("--output", help="Optional path to save JSON output")
    args = parser.parse_args()

    collector = LotteCinemaCollector()

    if args.mode == "bundle":
        output = collector.collect_bundle(
            play_date=args.play_date,
            representation_movie_code=args.movie_code,
            cinema_selector=args.cinema_selector,
            seat_cinema_id=args.seat_cinema_id,
            seat_screen_id=args.seat_screen_id,
            seat_play_sequence=args.seat_play_sequence,
            seat_screen_division_code=args.seat_screen_division_code,
        )
    elif args.mode == "movies":
        output = collector.build_movie_records()
    elif args.mode == "cinemas":
        output = collector.build_cinema_records()
    elif args.mode == "play-dates":
        output = collector.build_play_date_records()
    elif args.mode == "schedules":
        output = collector.build_schedule_records(
            play_date=args.play_date,
            cinema_selector=args.cinema_selector,
            representation_movie_code=args.movie_code,
        )
    elif args.mode == "seats":
        output = collector.build_seat_records(
            cinema_id=args.seat_cinema_id,
            screen_id=args.seat_screen_id,
            play_date=args.play_date,
            play_sequence=args.seat_play_sequence,
            screen_division_code=args.seat_screen_division_code,
        )
    else:
        output = collector.summarize_seat_map(
            cinema_id=args.seat_cinema_id,
            screen_id=args.seat_screen_id,
            play_date=args.play_date,
            play_sequence=args.seat_play_sequence,
            screen_division_code=args.seat_screen_division_code,
        )

    text = json.dumps(output, ensure_ascii=False, indent=2)
    if args.output:
        Path(args.output).write_text(text, encoding="utf-8")
    print(text)


if __name__ == "__main__":
    main()
