# movies.html 백엔드 서버 개발 계획

이 문서는 `frontend/movies.html`와 `frontend/src/js/liveMovies.js`를 실제 DB 기반 API로 연결하기 위한 백엔드 구현 계획서다. 목표는 프론트의 가상 데이터 의존을 줄이고, 상영 정보 조회와 필터링의 책임을 Spring Boot 백엔드로 옮기되 현재 화면 구조를 크게 깨지 않는 것이다.

## 1. 이번 작업의 기준

- 현재 프론트는 `loadLiveMovies()`에서 `GET /api/live/nearby?lat={lat}&lng={lng}`를 호출한다.
- 백엔드 기본 포트는 `8080`, 프론트 JS의 기본 API 주소는 `http://localhost:8000/api`라서 실행 계약이 서로 안 맞는다.
- 백엔드는 `collectors/`를 대체하지 않고, 수집 결과가 적재된 TiDB/MySQL 데이터를 조회하는 역할에 집중한다.
- 현재 `movies.html`은 카드 목록과 모달 시간표를 모두 동일한 원본 schedule 리스트에서 파생한다.

## 2. 핵심 수정 방향

기존 계획은 큰 방향은 맞지만, 실제 구현 전에 아래 항목이 더 명확해야 한다.

- 프론트가 기대하는 실제 엔드포인트와 응답 shape를 먼저 고정해야 한다.
- 영화 카드용 집계와 모달용 시간표 상세가 같은 데이터 원본에서 나와야 한다.
- 제공사 코드, 특별관/특별석 표현, 좌석 상태 기준을 백엔드 표준으로 정해야 한다.
- 위치 검색, 시간 필터, 문자열 검색, 좌석 비율 계산을 어느 계층에서 처리할지 분리해야 한다.
- `movies.html` 범위와 추천/AI 범위를 분리해야 한다.

## 3. 고정 계약

### 3.1 API 베이스 경로

`movies.html` 우선 연동을 위해 1차 구현은 아래 계약으로 고정한다.

- base path: `/api`
- 1차 조회 엔드포인트: `GET /api/live/nearby`
- 상세 시간표 엔드포인트: `GET /api/live/movies/{movieKey}/schedules`

추가 버전 관리가 필요해지면 `/api/v1/...` alias를 나중에 더한다. 이번 범위에서는 프론트 수정량을 줄이는 쪽이 우선이다.

### 3.2 포트/프록시 결정

현재 충돌이 있으니 구현 전에 둘 중 하나를 선택해야 한다.

- 권장: 프론트 `API_BASE_URL`을 백엔드 기본 포트 기준인 `http://localhost:8080/api`로 맞춘다.
- 대안: 프론트 개발 서버 또는 프록시를 `8000 -> 8080`으로 붙인다.

이번 계획 기준 기본안은 첫 번째다. 이유는 현재 프론트가 정적 파일 구조라 별도 프록시 레이어를 추가할 이유가 약하기 때문이다.

### 3.3 범위

이번 서버 구축 범위에 포함한다.

- 위치 기반 상영 정보 조회
- 시간대 필터
- 영화사/특별관/특별석/좌석 상태/검색어 필터
- 카드 목록용 집계 데이터
- 모달 시간표 상세 데이터
- CORS와 환경변수 기반 설정

이번 범위에서 제외한다.

- 개인화 추천
- LM Studio 연동
- 로그인/세션 저장
- 관리자 화면
- 수집기 실행 오케스트레이션

## 4. 프론트 기준 백엔드 계약

### 4.1 `GET /api/live/nearby`

#### request query

- `lat`: 필수, 사용자 위도
- `lng`: 필수, 사용자 경도
- `date`: 선택, `YYYY-MM-DD`, 기본값 오늘
- `timeStart`: 선택, `HH:mm`, 기본값 `06:00`
- `timeEnd`: 선택, `HH:mm`, 기본값 `23:59`
- `radiusKm`: 선택, 기본값 `8`
- `providers`: 선택, comma-separated
- `formats`: 선택, comma-separated
- `seatTypes`: 선택, comma-separated
- `seatState`: 선택, `all|spacious|comfortable|closing|group`
- `query`: 선택, 영화명 또는 극장명 검색어
- `limit`: 선택, 기본값 `300`

#### response shape

```json
{
  "search": {
    "lat": 37.4979,
    "lng": 127.0276,
    "date": "2026-04-21",
    "timeStart": "06:00",
    "timeEnd": "23:59",
    "radiusKm": 8,
    "resultCount": 124
  },
  "results": [
    {
      "movie_key": "movie:야당",
      "movie_name": "야당",
      "provider": "CGV",
      "provider_code": "CGV",
      "theater_id": "cgv-gangnam",
      "theater_name": "CGV 강남",
      "screen_id": "screen-1",
      "screen_name": "IMAX관",
      "format_name": "IMAX",
      "seat_type_tags": ["RECLINER"],
      "age_rating": "15",
      "start_time": "19:40",
      "end_time": "22:01",
      "show_date": "2026-04-21",
      "total_seat_count": 120,
      "available_seat_count": 48,
      "remaining_seat_count": 48,
      "seat_ratio": 0.4,
      "seat_state": "comfortable",
      "distance_km": 2.31,
      "booking_url": "https://...",
      "updated_at": "2026-04-21T15:10:00+09:00"
    }
  ]
}
```

#### 응답 정책

- 프론트 최소 수정이 목표라서 `results`는 카드 집계 전의 flat schedule 리스트로 유지한다.
- 현재 JS가 읽는 필드명인 `movie_name`, `provider`, `format_name`, `theater_name`, `age_rating`, `total_seat_count`, `available_seat_count`, `start_time`는 반드시 포함한다.
- 호환성을 위해 `remaining_seat_count`도 함께 내려준다.
- `provider`는 프론트 필터와 맞춰 `CGV`, `LOTTE`, `MEGA` 중 하나로 정규화한다.
- 원본 코드 보존을 위해 `provider_code`는 별도 필드로 둔다.

### 4.2 `GET /api/live/movies/{movieKey}/schedules`

이 엔드포인트는 2차 구현 대상이다. 현재 모달은 `allRawSchedules`에서 파생 가능하므로 1차 출시 필수는 아니다. 다만 아래 조건 중 하나가 생기면 도입한다.

- 근처 상영 데이터 양이 커져서 목록 API payload가 지나치게 커질 때
- 모달에서 더 넓은 시간 범위 또는 더 많은 극장 데이터를 따로 조회해야 할 때
- 영화 카드와 모달 데이터 캐싱 전략을 분리해야 할 때

#### 초기 응답 방향

- path variable: `movieKey`
- query: `lat`, `lng`, `date`, `timeStart`, `timeEnd`, `radiusKm`
- response: 극장 단위 그룹과 provider 분류를 포함한 시간표 리스트

## 5. 정규화 규칙

### 5.1 제공사 코드

프론트와 백엔드가 혼동 없이 맞춰야 한다.

| 원본 값 예시 | 백엔드 표준 `provider` | 보조 필드 |
| :--- | :--- | :--- |
| `CGV` | `CGV` | `provider_code=CGV` |
| `LOTTE_CINEMA` | `LOTTE` | `provider_code=LOTTE_CINEMA` |
| `MEGABOX` | `MEGA` | `provider_code=MEGABOX` |

현재 `movies.html` 모달 탭 텍스트는 `MEGABOX`, 필터 칩 값은 `MEGA`라서 불일치가 있다. 이번 계획에서는 백엔드 표준을 `MEGA`로 두고, 프론트 화면 라벨만 `메가박스`로 표시하는 쪽이 안전하다.

### 5.2 특별관/특별석

문자열 contains만으로 끝내지 말고 백엔드에서 태그화를 한다.

- `format_tags`: `IMAX`, `DOLBY`, `4DX`, `SCREENX`
- `seat_type_tags`: `RECLINER`, `PRIVATE`, `CHEF`

원본 `screen_name`, `format_name`, `special_info` 등에서 정규화하되, 판별 규칙은 설정이나 enum으로 분리한다.

### 5.3 좌석 상태

백엔드 기준값을 고정한다.

- `spacious`: `available / total >= 0.5`
- `comfortable`: `available / total >= 0.3`
- `closing`: `available / total < 0.1 && available > 0`
- `group`: `available >= 20`
- `sold_out`: `available = 0`

응답에는 비율 계산 결과인 `seat_ratio`와 분류 결과인 `seat_state`를 함께 포함한다.

## 6. 서버 내부 구조 계획

### 6.1 패키지 책임

- `api`: 요청 검증, query 파라미터 바인딩, 응답 DTO
- `service`: 검색 조건 조합, 좌석 상태 계산, provider/format 정규화
- `repository`: `showtimes` 중심 조회 SQL, 집계 쿼리, 상세 조회
- `domain`: `LiveMovieSearchCriteria`, `LiveSchedule`, `SeatState`, `ProviderType`
- `config`: CORS, 설정 바인딩, clock/timezone, 예외 응답

### 6.2 1차 구현 클래스 제안

- `LiveMovieController`
- `LiveMovieService`
- `LiveMovieRepository`
- `LiveMovieSearchCriteria`
- `LiveMovieRowMapper`
- `ProviderNormalizer`
- `SeatStateCalculator`
- `LiveMovieResponse`

## 7. DB 조회 전략

### 7.1 조회 기준

백엔드 README 방향대로 `showtimes`를 중심에 둔다.

- `showtimes`
- `movies`
- `theaters`
- `screens`

필요 시 좌석 스냅샷 또는 가격 테이블이 붙더라도 1차 구현의 메인 엔트리는 `showtimes`다.

### 7.2 필수 조회 조건

- 상영일자
- 시작 시각 범위
- 극장 좌표 반경
- 제공사 필터
- 영화명/극장명 검색
- 좌석 잔여 조건
- 특별관/특별석 태그 조건

### 7.3 SQL 방향

- 위치 반경은 MySQL/TiDB에서 계산 가능한 거리식으로 처리한다.
- 시간 비교는 문자열이 아니라 `TIME` 또는 분 단위 정규화 값으로 처리한다.
- `LIKE '%keyword%'`는 초기 구현에서 허용하되, 데이터가 커지면 전문 검색 또는 접두 검색으로 분리한다.
- 결과는 기본적으로 `date`, `start_time`, `distance_km`, `movie_name` 순으로 정렬한다.

### 7.4 인덱스 후보

- `showtimes(show_date, start_time)`
- `showtimes(movie_id, show_date, start_time)`
- `showtimes(screen_id, show_date, start_time)`
- `theaters(latitude, longitude)`는 직접 인덱스 효율이 낮을 수 있으니 bounding box 전처리도 검토한다.
- 필요하면 정규화된 provider/format tag 컬럼 또는 매핑 테이블을 추가한다.

## 8. 구현 단계

### Phase 0. 계약 먼저 고정

- 프론트의 API base URL을 `8080` 기준으로 맞출지 결정
- `provider` 표준값을 `CGV|LOTTE|MEGA`로 고정
- `movie_key` 생성 규칙 결정
- `date`, `timeStart`, `timeEnd` 기본값 처리 위치를 백엔드 기준으로 고정

### Phase 1. 조회 API 최소 기능

- `GET /api/live/nearby` 구현
- 필수 파라미터 검증
- date/time/radius 기본값 처리
- DB 없는 환경에서도 명확한 오류 응답 또는 빈 결과 응답 설계

### Phase 2. 정규화와 필터 완성

- provider/format/seat type 정규화
- 좌석 상태 계산
- 검색어, 영화사, 특별관, 특별석, 좌석 상태 필터 적용
- `results[]` shape 프론트 호환성 검증

### Phase 3. 카드/모달 최적화

- 카드에서 필요한 집계 데이터가 충분한지 검증
- payload가 과하면 상세 시간표 API 추가
- `booking_url`, `updated_at`, `distance_km` 노출

### Phase 4. 운영 준비

- CORS origin 설정
- 쿼리 성능 확인
- 예외 응답 표준화
- collector 데이터 적재 주기와 API freshness 기준 정리

## 9. 검증 계획

### 9.1 API 검증

- `lat/lng` 누락 시 400 응답
- 잘못된 날짜/시간 형식 시 400 응답
- 정상 요청 시 `results` 배열과 `search.resultCount` 일치
- `provider`가 `CGV|LOTTE|MEGA` 중 하나로만 내려오는지 확인
- `seat_state`와 `seat_ratio` 계산값 검증

### 9.2 프론트 계약 검증

- `loadLiveMovies()`가 추가 매핑 없이 동작하는지 확인
- 카드 렌더링에 필요한 필드가 모두 존재하는지 확인
- 모달 시간표 렌더링이 provider 필터와 시간 필터를 함께 처리하는지 확인
- 검색 결과 0건일 때 프론트가 정상 메시지를 보여주는지 확인

### 9.3 저장소 검증 명령

백엔드 코드 구현 단계에서 아래를 기본 검증으로 둔다.

```powershell
cd backend
gradle test
gradle bootJar
```

문서 단계인 현재 작업에서는 프론트 파일과 계획 문서 간 계약 검토까지로 마감한다.

## 10. 주요 리스크와 대응

- 포트 불일치 리스크: 프론트 기본 API 주소를 조정하거나 프록시를 명시적으로 둔다.
- provider 불일치 리스크: `LOTTE_CINEMA`, `LOTTE`, `MEGABOX`, `MEGA`를 혼용하지 말고 백엔드 표준값을 고정한다.
- 태그 문자열 의존 리스크: 특별관/특별석 판별을 enum 또는 룰 테이블로 분리한다.
- 응답 과대 리스크: 1차는 flat schedule로 가되, payload 증가 시 상세 API를 분리한다.
- 거리 계산 비용 리스크: 반경 검색 전에 bounding box를 적용하고 필요한 인덱스를 검토한다.

## 11. 이번 계획의 완료 기준

아래 조건을 만족하면 `movies.html`용 백엔드 계획이 구현 착수 가능한 상태로 본다.

- 어떤 엔드포인트를 먼저 만들지 명확하다.
- 프론트가 기대하는 필드와 백엔드 표준 필드가 충돌 없이 정의됐다.
- 조회 기준, 정규화 규칙, 좌석 상태 계산식이 문서에 고정됐다.
- 구현 단계와 검증 기준이 문서만 읽어도 바로 따라갈 수 있다.
