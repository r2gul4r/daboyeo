from __future__ import annotations

import json
import urllib.parse
import urllib.request
from typing import Any


LOTTE_TICKETING_URL = "https://www.lottecinema.co.kr/LCWS/Ticketing/TicketingData.aspx"
USER_AGENT = "Mozilla/5.0"


def post_ticketing(payload: dict[str, Any]) -> dict[str, Any]:
    body = urllib.parse.urlencode(
        {"paramList": json.dumps(payload, ensure_ascii=False)}
    ).encode("utf-8")
    request = urllib.request.Request(
        LOTTE_TICKETING_URL,
        data=body,
        headers={"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"},
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def main() -> None:
    base_payload = {
        "channelType": "HO",
        "osType": "W",
        "osVersion": USER_AGENT,
    }

    ticketing_page = post_ticketing(
        {
            **base_payload,
            "MethodName": "GetTicketingPageTOBE",
            "memberOnNo": "0",
        }
    )

    play_dates = ticketing_page["MoviePlayDates"]["Items"]["Items"]
    movies = ticketing_page["Movies"]["Movies"]["Items"]
    cinemas = ticketing_page["Cinemas"]["Cinemas"]["Items"]

    selected_movie = movies[0]
    selected_cinema = cinemas[0]
    selected_play_date = play_dates[0]["PlayDate"]
    cinema_id = (
        f"{selected_cinema['DivisionCode']}|"
        f"{selected_cinema['DetailDivisionCode']}|"
        f"{selected_cinema['CinemaID']}"
    )

    play_sequence_response = post_ticketing(
        {
            **base_payload,
            "MethodName": "GetPlaySequence",
            "playDate": selected_play_date,
            "cinemaID": cinema_id,
            "representationMovieCode": selected_movie["RepresentationMovieCode"],
        }
    )
    play_sequences = play_sequence_response["PlaySeqs"]["Items"]

    first_play = play_sequences[0] if play_sequences else None
    seat_response: dict[str, Any] = {}
    seat_preview: dict[str, Any] = {}

    if first_play:
        seat_response = post_ticketing(
            {
                **base_payload,
                "MethodName": "GetSeats",
                "cinemaId": int(first_play["CinemaID"]),
                "screenId": int(first_play["ScreenID"]),
                "playDate": first_play["PlayDt"],
                "playSequence": int(first_play["PlaySequence"]),
                "screenDivisionCode": int(first_play["ScreenDivisionCode"]),
            }
        )

        seat_items = seat_response.get("Seats", {}).get("Items", [])
        screen_items = seat_response.get("ScreenSeatInfo", {}).get("Items", [])
        fee_items = seat_response.get("Fees", {}).get("Items", [])
        seat_preview = {
            "seat_count": len(seat_items),
            "screen_seat_info": screen_items[0] if screen_items else {},
            "first_seat": seat_items[0] if seat_items else {},
            "first_fee": fee_items[0] if fee_items else {},
        }

    output = {
        "source": "lottecinema",
        "ticketing_url": LOTTE_TICKETING_URL,
        "play_date_count": len(play_dates),
        "movie_count": len(movies),
        "cinema_count": len(cinemas),
        "selected_play_date": selected_play_date,
        "selected_movie": {
            "representation_movie_code": selected_movie.get("RepresentationMovieCode"),
            "movie_name_kr": selected_movie.get("MovieNameKR"),
            "view_grade_name_kr": selected_movie.get("ViewGradeNameKR"),
            "release_date": selected_movie.get("ReleaseDate"),
            "poster_url": selected_movie.get("PosterURL"),
        },
        "selected_cinema": {
            "division_code": selected_cinema.get("DivisionCode"),
            "detail_division_code": selected_cinema.get("DetailDivisionCode"),
            "cinema_id": selected_cinema.get("CinemaID"),
            "cinema_name_kr": selected_cinema.get("CinemaNameKR"),
        },
        "play_sequence_count": len(play_sequences),
        "first_play_sequence": first_play,
        "seat_response_keys": list(seat_response.keys()),
        "seat_preview": seat_preview,
    }
    print(json.dumps(output, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
