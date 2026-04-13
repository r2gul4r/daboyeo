from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from .api import CgvApiClient


@dataclass
class CgvCollector:
    api: CgvApiClient | None = None
    co_cd: str = "A420"

    def __post_init__(self) -> None:
        if self.api is None:
            self.api = CgvApiClient()

    def fetch_movies(self, mov_nm: str = "", div: str = "", attr_cd: str = "") -> list[dict[str, Any]]:
        payload = self.api.fetch_movies(co_cd=self.co_cd, mov_nm=mov_nm, div=div, attr_cd=attr_cd)
        return payload.get("data") or []

    def fetch_attributes(self) -> list[dict[str, Any]]:
        payload = self.api.fetch_attributes(co_cd=self.co_cd)
        return payload.get("data") or []

    def fetch_regions_and_sites(self) -> dict[str, list[dict[str, Any]]]:
        payload = self.api.fetch_regions_and_sites(co_cd=self.co_cd)
        data = payload.get("data") or {}
        return {
            "regions": data.get("regionInfo") or [],
            "sites": data.get("siteInfo") or [],
            "recommended_sites": data.get("rcmSiteInfo") or [],
        }

    def fetch_dates_by_movie(self, site_no: str, mov_no: str, div: str = "", attr_cd: str = "") -> list[dict[str, Any]]:
        payload = self.api.fetch_dates_by_movie(
            co_cd=self.co_cd,
            site_no=site_no,
            mov_no=mov_no,
            div=div,
            attr_cd=attr_cd,
        )
        return payload.get("data") or []

    def fetch_schedules_by_movie(
        self,
        site_no: str,
        scn_ymd: str,
        mov_no: str,
        scns_no: str = "",
        scn_sseq: str = "",
        prod_no: str = "",
        rtctl_scop_cd: str = "08",
        sals_tzn_cd: str = "",
        tcscns_grad_cd: str = "",
        sascns_grad_cd: str = "",
        cust_no: str = "",
    ) -> list[dict[str, Any]]:
        payload = self.api.fetch_schedules_by_movie(
            co_cd=self.co_cd,
            site_no=site_no,
            scn_ymd=scn_ymd,
            mov_no=mov_no,
            scns_no=scns_no,
            scn_sseq=scn_sseq,
            prod_no=prod_no,
            rtctl_scop_cd=rtctl_scop_cd,
            sals_tzn_cd=sals_tzn_cd,
            tcscns_grad_cd=tcscns_grad_cd,
            sascns_grad_cd=sascns_grad_cd,
            cust_no=cust_no,
        )
        return payload.get("data") or []

    def fetch_seat_map(
        self,
        site_no: str,
        scn_ymd: str,
        scns_no: str,
        scn_sseq: str,
        seat_area_no: str = "",
        cusgd_cd: str = "",
        cust_no: str = "",
    ) -> dict[str, Any]:
        payload = self.api.fetch_seats(
            co_cd=self.co_cd,
            site_no=site_no,
            scn_ymd=scn_ymd,
            scns_no=scns_no,
            scn_sseq=scn_sseq,
            seat_area_no=seat_area_no,
            cusgd_cd=cusgd_cd,
            cust_no=cust_no,
        )
        return payload.get("data") or {}

    def build_movie_records(self) -> list[dict[str, Any]]:
        records: list[dict[str, Any]] = []
        for row in self.fetch_movies():
            records.append(
                {
                    "provider": "CGV",
                    "co_cd": row.get("coCd") or self.co_cd,
                    "movie_no": row.get("movNo"),
                    "movie_name": row.get("movNm"),
                    "poster_filename": row.get("i320Fnm"),
                    "runtime_minutes": row.get("scnBssTm"),
                    "age_rating_code": row.get("cratgClsCd"),
                    "booking_rate": row.get("atktRate"),
                    "mobile_url": row.get("mblUrl"),
                    "raw": row,
                }
            )
        return records

    def build_attribute_records(self) -> list[dict[str, Any]]:
        records: list[dict[str, Any]] = []
        for row in self.fetch_attributes():
            records.append(
                {
                    "provider": "CGV",
                    "attribute_code": row.get("comCd") or row.get("attrCd"),
                    "attribute_name": row.get("comCdNm") or row.get("attrNm"),
                    "raw": row,
                }
            )
        return records

    def build_region_records(self) -> list[dict[str, Any]]:
        records: list[dict[str, Any]] = []
        for row in self.fetch_regions_and_sites()["regions"]:
            records.append(
                {
                    "provider": "CGV",
                    "region_code": row.get("regionCd") or row.get("regnCd"),
                    "region_name": row.get("regionNm") or row.get("regnNm"),
                    "region_name_en": row.get("regionEnm") or row.get("regnEnm"),
                    "raw": row,
                }
            )
        return records

    def build_site_records(self) -> list[dict[str, Any]]:
        records: list[dict[str, Any]] = []
        for row in self.fetch_regions_and_sites()["sites"]:
            records.append(
                {
                    "provider": "CGV",
                    "co_cd": row.get("coCd") or self.co_cd,
                    "site_no": row.get("siteNo"),
                    "site_name": row.get("siteNm"),
                    "region_code": row.get("regionCd") or row.get("regnCd"),
                    "region_name": row.get("regionNm") or row.get("regnNm"),
                    "latitude": row.get("latiVal"),
                    "longitude": row.get("lntiVal"),
                    "address": row.get("roadNmAddr") or row.get("addr"),
                    "special_site_yn": row.get("speclSiteYn"),
                    "raw": row,
                }
            )
        return records

    def build_date_records(self, site_no: str, mov_no: str) -> list[dict[str, Any]]:
        records: list[dict[str, Any]] = []
        for row in self.fetch_dates_by_movie(site_no=site_no, mov_no=mov_no):
            records.append(
                {
                    "provider": "CGV",
                    "site_no": site_no,
                    "movie_no": mov_no,
                    "screening_date": row.get("scnYmd"),
                    "holiday": row.get("hldyYn"),
                    "raw": row,
                }
            )
        return records

    def build_schedule_records(self, site_no: str, scn_ymd: str, mov_no: str) -> list[dict[str, Any]]:
        records: list[dict[str, Any]] = []
        for row in self.fetch_schedules_by_movie(site_no=site_no, scn_ymd=scn_ymd, mov_no=mov_no):
            records.append(
                {
                    "provider": "CGV",
                    "co_cd": row.get("coCd") or self.co_cd,
                    "site_no": row.get("siteNo"),
                    "site_name": row.get("siteNm"),
                    "movie_no": row.get("movNo"),
                    "movie_name": row.get("movNm") or row.get("prodNm"),
                    "movie_name_en": row.get("movEnm") or row.get("engProdNm"),
                    "screen_no": row.get("scnsNo"),
                    "screen_name": row.get("scnsNm") or row.get("expoScnsNm"),
                    "screening_date": row.get("scnYmd"),
                    "screen_sequence": row.get("scnSseq"),
                    "product_no": row.get("prodNo"),
                    "format_code": row.get("movkndCd"),
                    "format_name": row.get("movkndDsplNm"),
                    "age_rating_code": row.get("cratgClsCd"),
                    "age_rating_name": row.get("cratgClsNm"),
                    "sales_zone_code": row.get("salsTznCd"),
                    "sales_zone_name": row.get("salsTznNm"),
                    "start_time": row.get("scnsrtTm"),
                    "end_time": row.get("scnendTm"),
                    "sale_end_time": row.get("salEndTm"),
                    "screen_grade_code": row.get("scnsGradCd"),
                    "screen_grade_name": row.get("sascnsGradNm"),
                    "total_seat_count": row.get("stcnt"),
                    "available_seat_count": row.get("frSeatCnt"),
                    "temp_locked_seat_count": row.get("frtmpSeatCnt"),
                    "caption_subtitle_code": row.get("sbtdivCd"),
                    "caption_subtitle_name": row.get("sbtdivNm"),
                    "poster_filename": row.get("physcFnm"),
                    "poster_path": row.get("physcFilePathnm"),
                    "booking_key": {
                        "site_no": row.get("siteNo"),
                        "scn_ymd": row.get("scnYmd"),
                        "scns_no": row.get("scnsNo"),
                        "scn_sseq": row.get("scnSseq"),
                    },
                    "raw": row,
                }
            )
        return records

    def build_seat_records(
        self,
        site_no: str,
        scn_ymd: str,
        scns_no: str,
        scn_sseq: str,
    ) -> list[dict[str, Any]]:
        payload = self.fetch_seat_map(
            site_no=site_no,
            scn_ymd=scn_ymd,
            scns_no=scns_no,
            scn_sseq=scn_sseq,
        )
        records: list[dict[str, Any]] = []
        for item in payload.get("items") or []:
            for seat in item.get("seats") or []:
                seat_row = str(seat.get("seatRowNm") or "")
                seat_no = str(seat.get("seatNo") or "")
                records.append(
                    {
                        "provider": "CGV",
                        "site_no": site_no,
                        "screening_date": scn_ymd,
                        "screen_no": scns_no,
                        "screen_sequence": scn_sseq,
                        "seat_loc_no": seat.get("seatLocNo"),
                        "seat_label": f"{seat_row}{seat_no}".strip(),
                        "seat_row": seat_row,
                        "seat_number": seat_no,
                        "seat_kind_code": seat.get("stkndCd"),
                        "seat_kind_name": seat.get("stkndNm"),
                        "seat_zone_name": seat.get("szoneNm"),
                        "seat_zone_kind_code": seat.get("szoneKindCd"),
                        "seat_status_code": seat.get("seatStusCd"),
                        "seat_status_name": seat.get("seatStusNm"),
                        "seat_sale_yn": seat.get("seatSaleYn"),
                        "left_passage_yn": seat.get("leftPwayYn"),
                        "right_passage_yn": seat.get("rghtPwayYn"),
                        "x1": seat.get("xcoordStartVal"),
                        "y1": seat.get("ycoordStartVal"),
                        "x2": seat.get("xcoordEndVal"),
                        "y2": seat.get("ycoordEndVal"),
                        "terminal_no": seat.get("trmnNo"),
                        "booking_ticket_no": seat.get("movAtktNo"),
                        "raw": seat,
                    }
                )
        return records

    def summarize_seat_map(
        self,
        site_no: str,
        scn_ymd: str,
        scns_no: str,
        scn_sseq: str,
    ) -> dict[str, Any]:
        payload = self.fetch_seat_map(
            site_no=site_no,
            scn_ymd=scn_ymd,
            scns_no=scns_no,
            scn_sseq=scn_sseq,
        )
        items = payload.get("items") or []
        seat_areas: list[dict[str, Any]] = []
        seat_boards: list[dict[str, Any]] = []
        for item in items:
            if item.get("sbord"):
                seat_boards.append(item.get("sbord"))
            seat_areas.extend(item.get("seatArea") or [])

        return {
            "provider": "CGV",
            "site_no": payload.get("siteNo"),
            "site_name": payload.get("siteNm"),
            "screening_date": payload.get("scnYmd"),
            "screen_no": payload.get("scnsNo"),
            "screen_name": payload.get("scnsNm"),
            "seat_board_count": len(seat_boards),
            "seat_area_count": len(seat_areas),
            "seat_count": len(self.build_seat_records(site_no, scn_ymd, scns_no, scn_sseq)),
            "seat_boards": seat_boards,
            "seat_areas": seat_areas,
        }

    def collect_bundle(
        self,
        site_no: str,
        mov_no: str,
        scn_ymd: str | None = None,
        scns_no: str | None = None,
        scn_sseq: str | None = None,
    ) -> dict[str, Any]:
        movies = self.build_movie_records()
        attributes = self.build_attribute_records()
        regions = self.build_region_records()
        sites = self.build_site_records()
        dates = self.build_date_records(site_no=site_no, mov_no=mov_no)

        effective_date = scn_ymd or (dates[0]["screening_date"] if dates else "")
        schedules = self.build_schedule_records(site_no=site_no, scn_ymd=effective_date, mov_no=mov_no) if effective_date else []

        effective_scns_no = scns_no
        effective_sseq = scn_sseq
        if schedules and (not effective_scns_no or not effective_sseq):
            effective_scns_no = effective_scns_no or str(schedules[0]["screen_no"] or "")
            effective_sseq = effective_sseq or str(schedules[0]["screen_sequence"] or "")

        seat_records: list[dict[str, Any]] = []
        seat_summary: dict[str, Any] | None = None
        if effective_date and effective_scns_no and effective_sseq:
            seat_records = self.build_seat_records(
                site_no=site_no,
                scn_ymd=effective_date,
                scns_no=effective_scns_no,
                scn_sseq=effective_sseq,
            )
            seat_summary = self.summarize_seat_map(
                site_no=site_no,
                scn_ymd=effective_date,
                scns_no=effective_scns_no,
                scn_sseq=effective_sseq,
            )

        return {
            "movie_count": len(movies),
            "attribute_count": len(attributes),
            "region_count": len(regions),
            "site_count": len(sites),
            "date_count": len(dates),
            "schedule_count": len(schedules),
            "seat_count": len(seat_records),
            "movies": movies,
            "attributes": attributes,
            "regions": regions,
            "sites": sites,
            "dates": dates,
            "schedules": schedules,
            "seat_records": seat_records,
            "seat_summary": seat_summary,
        }
