from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from .api import LotteCinemaApiClient


@dataclass
class LotteCinemaCollector:
    api: LotteCinemaApiClient | None = None

    def __post_init__(self) -> None:
        if self.api is None:
            self.api = LotteCinemaApiClient()

    def fetch_ticketing_page(self) -> dict[str, Any]:
        return self.api.fetch_ticketing_page()

    def fetch_movies(self) -> list[dict[str, Any]]:
        page = self.fetch_ticketing_page()
        return ((page.get("Movies") or {}).get("Movies") or {}).get("Items") or []

    def fetch_cinemas(self) -> list[dict[str, Any]]:
        page = self.fetch_ticketing_page()
        return ((page.get("Cinemas") or {}).get("Cinemas") or {}).get("Items") or []

    def fetch_play_dates(self) -> list[dict[str, Any]]:
        page = self.fetch_ticketing_page()
        return ((page.get("MoviePlayDates") or {}).get("Items") or {}).get("Items") or []

    def build_movie_records(self) -> list[dict[str, Any]]:
        records: list[dict[str, Any]] = []
        for row in self.fetch_movies():
            records.append(
                {
                    "provider": "LOTTE_CINEMA",
                    "movie_no": row.get("RepresentationMovieCode"),
                    "movie_name": row.get("MovieNameKR"),
                    "movie_name_en": row.get("MovieNameUS"),
                    "age_rating": row.get("ViewGradeNameKR"),
                    "booking_rate": row.get("BookingRate"),
                    "evaluation": row.get("Evaluation"),
                    "release_date": row.get("ReleaseDate"),
                    "runtime_minutes": row.get("PlayTime"),
                    "poster_url": row.get("PosterURL"),
                    "director_name": row.get("DirectorName"),
                    "actor_name": row.get("ActorName"),
                    "genre_name": row.get("MovieGenreNameKR"),
                    "special_screen_codes": row.get("SpecialScreenDivisionCode"),
                    "raw": row,
                }
            )
        return records

    def build_cinema_records(self) -> list[dict[str, Any]]:
        records: list[dict[str, Any]] = []
        for row in self.fetch_cinemas():
            records.append(
                {
                    "provider": "LOTTE_CINEMA",
                    "division_code": row.get("DivisionCode"),
                    "detail_division_code": row.get("DetailDivisionCode"),
                    "detail_division_name": row.get("DetailDivisionNameKR"),
                    "cinema_id": row.get("CinemaID"),
                    "cinema_name": row.get("CinemaNameKR"),
                    "cinema_name_en": row.get("CinemaNameUS"),
                    "latitude": row.get("Latitude"),
                    "longitude": row.get("Longitude"),
                    "address_summary": row.get("CinemaAddrSummary"),
                    "cinema_area_code": row.get("CinemaAreaCode"),
                    "cinema_area_name": row.get("CinemaAreaName"),
                    "smart_order": row.get("SmartOrderYN"),
                    "open_dt": row.get("OpenDtYN"),
                    "stage_greeting": row.get("StageGreetingYN"),
                    "raw": row,
                }
            )
        return records

    def build_play_date_records(self) -> list[dict[str, Any]]:
        records: list[dict[str, Any]] = []
        for row in self.fetch_play_dates():
            records.append(
                {
                    "provider": "LOTTE_CINEMA",
                    "play_date": row.get("PlayDate"),
                    "day_of_week": row.get("DayOfWeek"),
                    "is_play": row.get("PlayYN"),
                    "raw": row,
                }
            )
        return records

    def build_cinema_selector(self, cinema: dict[str, Any]) -> str:
        return (
            f"{cinema.get('DivisionCode')}|"
            f"{cinema.get('DetailDivisionCode')}|"
            f"{cinema.get('CinemaID')}"
        )

    def fetch_play_sequences(
        self,
        play_date: str,
        cinema_selector: str,
        representation_movie_code: str,
    ) -> list[dict[str, Any]]:
        response = self.api.fetch_play_sequences(
            play_date=play_date,
            cinema_id=cinema_selector,
            representation_movie_code=representation_movie_code,
        )
        return ((response.get("PlaySeqs") or {}).get("Items")) or []

    def build_schedule_records(
        self,
        play_date: str,
        cinema_selector: str,
        representation_movie_code: str,
    ) -> list[dict[str, Any]]:
        records: list[dict[str, Any]] = []
        for row in self.fetch_play_sequences(
            play_date=play_date,
            cinema_selector=cinema_selector,
            representation_movie_code=representation_movie_code,
        ):
            records.append(
                {
                    "provider": "LOTTE_CINEMA",
                    "movie_no": row.get("RepresentationMovieCode"),
                    "movie_name": row.get("MovieNameKR"),
                    "movie_name_en": row.get("MovieNameUS"),
                    "age_rating": row.get("ViewGradeNameKR"),
                    "cinema_id": row.get("CinemaID"),
                    "cinema_name": row.get("CinemaNameKR"),
                    "screen_id": row.get("ScreenID"),
                    "screen_name": row.get("ScreenNameKR"),
                    "screen_division_code": row.get("ScreenDivisionCode"),
                    "screen_division_name": row.get("ScreenDivisionNameKR"),
                    "film_code": row.get("FilmCode"),
                    "film_name": row.get("FilmNameKR"),
                    "sound_type_code": row.get("SoundTypeCode"),
                    "sound_type_name": row.get("SoundTypeNameKR"),
                    "translation_division_code": row.get("TranslationDivisionCode"),
                    "translation_division_name": row.get("TranslationDivisionNameKR"),
                    "play_date": row.get("PlayDt"),
                    "play_day": row.get("PlayDayKR"),
                    "play_sequence": row.get("PlaySequence"),
                    "start_time": row.get("StartTime"),
                    "end_time": row.get("EndTime"),
                    "total_seat_count": row.get("TotalSeatCount"),
                    "remaining_seat_count": self._calculate_remaining_seats(row),
                    "booked_seat_count": row.get("BookingSeatCount"),
                    "booking_available": row.get("IsBookingYN"),
                    "sequence_group_name": row.get("SequenceNoGroupNameKR"),
                    "screen_floor": row.get("ScreenFloor"),
                    "poster_url": row.get("PosterURL"),
                    "booking_key": {
                        "cinema_id": row.get("CinemaID"),
                        "screen_id": row.get("ScreenID"),
                        "play_date": row.get("PlayDt"),
                        "play_sequence": row.get("PlaySequence"),
                        "screen_division_code": row.get("ScreenDivisionCode"),
                    },
                    "raw": row,
                }
            )
        return records

    def fetch_seat_map(
        self,
        cinema_id: int,
        screen_id: int,
        play_date: str,
        play_sequence: int,
        screen_division_code: int,
    ) -> dict[str, Any]:
        return self.api.fetch_seats(
            cinema_id=cinema_id,
            screen_id=screen_id,
            play_date=play_date,
            play_sequence=play_sequence,
            screen_division_code=screen_division_code,
        )

    def build_seat_records(
        self,
        cinema_id: int,
        screen_id: int,
        play_date: str,
        play_sequence: int,
        screen_division_code: int,
    ) -> list[dict[str, Any]]:
        payload = self.fetch_seat_map(
            cinema_id=cinema_id,
            screen_id=screen_id,
            play_date=play_date,
            play_sequence=play_sequence,
            screen_division_code=screen_division_code,
        )
        records: list[dict[str, Any]] = []
        for row in ((payload.get("Seats") or {}).get("Items")) or []:
            seat_row = str(row.get("ShowSeatRow") or row.get("SeatRow") or "")
            seat_column = row.get("ShowSeatColumn") or row.get("SeatColumn")
            records.append(
                {
                    "provider": "LOTTE_CINEMA",
                    "cinema_id": cinema_id,
                    "screen_id": screen_id,
                    "play_date": play_date,
                    "play_sequence": play_sequence,
                    "screen_division_code": screen_division_code,
                    "seat_no": row.get("SeatNo"),
                    "seat_label": f"{seat_row}{seat_column}" if seat_row else str(row.get("SeatNo") or ""),
                    "seat_row": seat_row,
                    "seat_column": seat_column,
                    "screen_floor": row.get("ScreenFloor"),
                    "seat_floor": row.get("SeatFloor"),
                    "seat_status_code": row.get("SeatStatusCode"),
                    "customer_division_code": row.get("CustomerDivisionCode"),
                    "physical_block_code": row.get("PhysicalBlockCode"),
                    "logical_block_code": row.get("LogicalBlockCode"),
                    "seat_block_set": row.get("SeatBlockSet"),
                    "sales_disable_ticket_code": row.get("SalesDisableTicketCode"),
                    "x": row.get("SeatXCoordinate"),
                    "y": row.get("SeatYCoordinate"),
                    "width": row.get("SeatXLength"),
                    "height": row.get("SeatYLength"),
                    "sweet_spot": row.get("SweetSpotYN"),
                    "raw": row,
                }
            )
        return records

    def summarize_seat_map(
        self,
        cinema_id: int,
        screen_id: int,
        play_date: str,
        play_sequence: int,
        screen_division_code: int,
    ) -> dict[str, Any]:
        payload = self.fetch_seat_map(
            cinema_id=cinema_id,
            screen_id=screen_id,
            play_date=play_date,
            play_sequence=play_sequence,
            screen_division_code=screen_division_code,
        )
        return {
            "provider": "LOTTE_CINEMA",
            "cinema_id": cinema_id,
            "screen_id": screen_id,
            "play_date": play_date,
            "play_sequence": play_sequence,
            "screen_division_code": screen_division_code,
            "customer_divisions": ((payload.get("CustomerDivision") or {}).get("Items")) or [],
            "screen_seat_info": ((payload.get("ScreenSeatInfo") or {}).get("Items")) or [],
            "entrances": ((payload.get("Enterences") or {}).get("Items")) or [],
            "seat_count": len(((payload.get("Seats") or {}).get("Items")) or []),
            "booking_seat_count": len(((payload.get("BookingSeats") or {}).get("Items")) or []),
            "fee_items": ((payload.get("Fees") or {}).get("Items")) or [],
            "play_seq_details": ((payload.get("PlaySeqsDetails") or {}).get("Items")) or [],
            "additional_messages": ((payload.get("AdditionalMessages") or {}).get("Items")) or [],
            "seat_info_img": payload.get("SeatInfoImg"),
        }

    def collect_bundle(
        self,
        play_date: str,
        representation_movie_code: str,
        cinema_selector: str,
        seat_cinema_id: int | None = None,
        seat_screen_id: int | None = None,
        seat_play_sequence: int | None = None,
        seat_screen_division_code: int | None = None,
    ) -> dict[str, Any]:
        movies = self.build_movie_records()
        cinemas = self.build_cinema_records()
        play_dates = self.build_play_date_records()
        schedules = self.build_schedule_records(
            play_date=play_date,
            cinema_selector=cinema_selector,
            representation_movie_code=representation_movie_code,
        )

        seat_summary: dict[str, Any] | None = None
        seat_records: list[dict[str, Any]] = []
        if (
            seat_cinema_id is not None
            and seat_screen_id is not None
            and seat_play_sequence is not None
            and seat_screen_division_code is not None
        ):
            seat_summary = self.summarize_seat_map(
                cinema_id=seat_cinema_id,
                screen_id=seat_screen_id,
                play_date=play_date,
                play_sequence=seat_play_sequence,
                screen_division_code=seat_screen_division_code,
            )
            seat_records = self.build_seat_records(
                cinema_id=seat_cinema_id,
                screen_id=seat_screen_id,
                play_date=play_date,
                play_sequence=seat_play_sequence,
                screen_division_code=seat_screen_division_code,
            )

        return {
            "play_date": play_date,
            "movie_count": len(movies),
            "cinema_count": len(cinemas),
            "play_date_count": len(play_dates),
            "schedule_count": len(schedules),
            "seat_count": len(seat_records),
            "movies": movies,
            "cinemas": cinemas,
            "play_dates": play_dates,
            "schedules": schedules,
            "seat_records": seat_records,
            "seat_summary": seat_summary,
        }

    @staticmethod
    def _calculate_remaining_seats(row: dict[str, Any]) -> int | None:
        total = row.get("TotalSeatCount")
        booked = row.get("BookingSeatCount")
        if isinstance(total, int) and isinstance(booked, int):
            return total - booked
        return None
