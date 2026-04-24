# Backend

Spring Boot 기반 API 서버 영역이다.

백엔드는 수집기를 대체하지 않는다. CGV, 롯데시네마, 메가박스 원본 데이터 수집은 기존 `collectors/`와 `scripts/`를 우선 보존하고, 백엔드는 TiDB/MySQL에 저장된 상영 데이터와 추천 상태를 조회, 정리, 응답하는 API와 서비스 로직에 집중한다.

## 기술 스택

- Java 21
- Spring Boot 3.5.13
- Gradle
- Spring Web REST API
- Spring JDBC / `JdbcTemplate`
- TiDB Cloud 또는 MySQL 호환 DB
- MySQL Connector/J
- Flyway MySQL migration 준비, 기본값은 비활성화
- JUnit 5 / Spring Boot Test
- LM Studio OpenAI-compatible API

아직 Gradle Wrapper는 넣지 않았다. 로컬에서는 설치된 `gradle` 명령을 사용한다.

## 백엔드 방향성

- 프론트엔드에 영화, 극장, 상영 시간, 좌석 요약, 예매 링크, 추천 결과를 제공한다.
- 검색과 비교의 기준은 `showtimes` 중심의 최소 공통 필드로 맞춘다.
- 제공사별 원본 데이터 특성은 수집기와 DB의 `raw_json` 또는 R2 object key로 보존한다.
- 사용자 추천은 익명 세션 기반으로 처리하고 이름, 이메일, 전화번호, 인증 토큰, 쿠키를 저장하지 않는다.
- AI 추천은 로컬 LM Studio 모델을 보조 판단자로 사용하되, 최종 응답은 DB에 존재하는 상영 후보만 반환한다.
- 비밀값, DB 접속 정보, 모델 설정은 환경변수로 주입한다.

## 로컬 실행

```powershell
cd backend
gradle bootRun
```

기본 포트는 `8080`이다.

DB가 없더라도 앱이 바로 죽지 않도록 Hikari fail-fast를 꺼두었다. 실제 DB 기능을 사용할 때는 루트 `.env.example`의 `DABOYEO_DB_*` 값을 참고한다. 백엔드는 시작할 때 워크스페이스 루트 `.env`를 자동으로 읽고, 같은 키가 OS 환경변수에도 있으면 OS 환경변수가 우선한다.

## 주요 환경변수

- `DABOYEO_BACKEND_PORT`: 백엔드 포트, 기본값 `8080`
- `DABOYEO_DB_URL`: JDBC URL
- `DABOYEO_DB_USERNAME`: DB 사용자
- `DABOYEO_DB_PASSWORD`: DB 비밀번호
- `DABOYEO_FLYWAY_ENABLED`: Flyway 실행 여부, 기본값 `false`
- `DABOYEO_FRONTEND_ORIGINS`: 허용할 프론트엔드 origin 목록
- `DABOYEO_LM_STUDIO_BASE_URL`: LM Studio API base URL, 기본값 `http://127.0.0.1:1234/v1`
- `DABOYEO_RECOMMEND_FAST_MODEL`: 빠른 추천 모델, 기본값 `gemma-4-e2b-it`
- `DABOYEO_RECOMMEND_PRECISE_MODEL`: 정밀 추천 모델, 기본값 `gemma-4-e4b-it`

## 수집 번들 적재

`movies.html` 백엔드는 `showtimes` 공통 테이블을 읽기 때문에, 수집기 번들을 아래 태스크로 적재하면 3사 데이터가 같은 조회 경로로 합쳐진다.

```powershell
cd backend
gradle ingestCollectorBundle -Pprovider=CGV -PbundlePath=../backend/build/tmp/cgv-bundle.json
gradle ingestCollectorBundle -Pprovider=LOTTE_CINEMA -PbundlePath=../backend/build/tmp/lotte-bundle.json
gradle ingestCollectorBundle -Pprovider=MEGABOX -PbundlePath=../backend/build/tmp/megabox-bundle.json
```

- 기본 동작은 dry-run 이고, 실제 upsert는 `-Pwrite=true`를 붙였을 때만 수행한다.
- 호환용 태스크로 `ingestCgvBundle`, `ingestLotteBundle`, `ingestMegaboxBundle`도 같이 제공한다.
- 루트 PowerShell 진입점은 `scripts/ingest_collector_to_tidb.ps1`이고, 기존 `scripts/ingest_cgv_to_tidb.ps1`는 CGV 래퍼로 유지한다.

## 백엔드 스케줄러 구조

백엔드 내부 자동화는 두 갈래로 나뉜다.

- 일일 상영표 동기화
  - 대상 테이블: `movies`, `theaters`, `screens`, `showtimes`
  - 기본 크론: 매일 `03:00`
  - 대상 날짜: 기본 `오늘 + 내일`
- 30분 좌석 스냅샷
  - 대상 테이블: `seat_snapshots`, `seat_snapshot_items`
  - 기본 크론: `30분`마다
  - 대상 범위: 현재 시각부터 기본 `6시간` 이내 상영

주요 클래스는 아래처럼 나뉜다.

- `sync/ShowtimeSyncScheduler`, `sync/ShowtimeSyncService`
- `sync/SeatSnapshotScheduler`, `sync/SeatSnapshotSyncService`
- `sync/PythonCollectorBridge`
- `ingest/CollectorBundlePersistenceService`
- `sync/SeatSnapshotRepository`, `sync/SeatSnapshotPersistenceService`

스케줄러는 기본적으로 꺼져 있고, 명시적으로 켰을 때만 동작한다.

```env
DABOYEO_SYNC_ENABLED=true
DABOYEO_SHOWTIME_SYNC_ENABLED=true
DABOYEO_SEAT_SYNC_ENABLED=true
```

상영표 수집 타깃은 provider별로 명시적으로 넣어야 한다. 지금 구현은 무작정 전국 전체를 순회하지 않고, 설정된 타깃만 안전하게 돈다.

- `CGV`: `siteNo`, `movieNo`
- `LOTTE_CINEMA`: `cinemaSelector`, `representationMovieCode`
- `MEGABOX`: `movieNo`, `areaCode`

좌석 스냅샷은 별도 타깃을 직접 받지 않고, DB에 저장돼 있는 가까운 상영의 `booking_key_json`을 읽어서 provider 좌석 API를 다시 호출한다.

## 추천 API 방향

추천 API는 빠른 모드와 정밀 모드를 나눈다.

- 빠른 모드: `gemma-4-e2b-it`를 사용하고 응답 설명을 단순하게 유지한다.
- 정밀 모드: `gemma-4-e4b-it`를 사용해 더 신중한 후보 선택과 분석 태그를 생성한다.
- LM Studio 호출이 실패하거나 응답이 깨지면 서버의 deterministic fallback으로 DB 후보 기반 추천을 유지한다.
- public response shape는 호환성을 우선한다. 필드 제거보다 빈 값 또는 호환 필드 추가를 선호한다.

## 검증 명령

```powershell
cd backend
gradle test
gradle bootJar
```

추천 로직만 빠르게 볼 때는 아래처럼 범위를 좁힌다.

```powershell
cd backend
gradle test --tests kr.daboyeo.backend.service.recommendation.* --tests kr.daboyeo.backend.repository.recommendation.*
```

## 패키지 경계

- `api`: HTTP 컨트롤러와 예외 응답
- `service`: 유스케이스, 추천 흐름, fallback 로직
- `repository`: DB 조회와 저장
- `domain`: 도메인 모델과 값 객체
- `config`: 설정 바인딩, CORS, 인프라 설정
