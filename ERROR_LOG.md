# ERROR LOG

Append-only log for installer, execution, tool, and verification errors.
Add new entries with timestamp, location, summary, and details.
Do not rewrite existing entries; append only.

- time: `2026-04-10 00:00:00 +09:00`
  location: `cgv_collector_demo.py, lotte_collector_demo.py, megabox_collector_demo.py`
  summary: `데모 스크립트 공통 실행 실패`
  details: `collectors/__init__.py 가 존재하지 않는 collectors.api 를 임포트해서 세 스크립트 모두 ModuleNotFoundError 로 즉시 종료됨`
  status: `open`

- time: `2026-04-10 00:10:00 +09:00`
  location: `collectors/__init__.py`
  summary: `공통 임포트 오류 해결`
  details: `패키지 루트가 각 하위 수집기 모듈의 실제 심볼만 재수출하도록 수정했고 세 데모 스크립트 재실행이 모두 성공함`
  status: `resolved`

- time: `2026-04-10 12:08:00 +09:00`
  location: `workspace search`
  summary: `rg 실행 권한 오류`
  details: `코드 검색에 rg 를 사용하려 했으나 Access is denied 로 실패했고 같은 탐색은 PowerShell Select-String 으로 우회했다`
  status: `resolved`

- time: `2026-04-10 12:25:03 +09:00`
  location: `PROJECT_HARNESS.md`
  summary: `필수 컨텍스트 파일 누락`
  details: `레포 규칙상 편집 전 읽어야 하는 PROJECT_HARNESS.md 를 확인하려 했지만 파일이 존재하지 않아 README.md, STATE.md, WORKSPACE_CONTEXT.toml, collectors/**, docs/** 를 대체 소스로 사용했다`
  status: `open`

- time: `2026-04-13 09:20:16 +09:00`
  location: `PROJECT_HARNESS.md`
  summary: `필수 컨텍스트 파일 누락 재확인`
  details: `이번 파일트리 정리 작업 전에도 PROJECT_HARNESS.md 를 읽으려 했지만 파일이 없어 README.md, STATE.md, WORKSPACE_CONTEXT.toml 및 파일트리 탐색 결과를 기준으로 진행했다`
  status: `open`

- time: `2026-04-13 09:20:16 +09:00`
  location: `workspace search`
  summary: `rg 실행 권한 오류 재발`
  details: `rg --files 실행이 Access is denied 로 실패해 PowerShell Get-ChildItem 기반 파일 탐색으로 우회했다`
  status: `resolved`

- time: `2026-04-13 16:56:02 +09:00`
  location: `TiDB Cloud connection verification`
  summary: `TiDB 인증 실패`
  details: `로컬 .env 값으로 PyMySQL TLS 연결을 시도했으나 TiDB가 Access denied 를 반환했다. 네트워크 도달성은 확인되었고 username/password 조합 또는 .env 중복/복사 오류를 우선 점검한다.`
  status: `open`

- time: `2026-04-13 17:02:43 +09:00`
  location: `TiDB Cloud connection verification`
  summary: `TiDB 인증 실패 해결`
  details: `.env 의 TiDB username prefix 가 TiDB Cloud Connect 화면 값과 달라 발생한 문제였다. TIDB_USER 및 DATABASE_URL 의 username 을 화면 값으로 맞춘 뒤 PyMySQL TLS 연결이 성공했다.`
  status: `resolved`

- time: `2026-04-13 16:45:22 +09:00`
  location: `PROJECT_HARNESS.md, WORKSPACE_CONTEXT.toml`
  summary: `하네스 컨텍스트가 현재 사용자 작업 방식과 불일치`
  details: `PROJECT_HARNESS.md 는 존재하지 않고 WORKSPACE_CONTEXT.toml 은 하네스 참조와 깨진 텍스트를 포함하고 있어 사용자 맞춤 workspace context 로 교체했다`
  status: `resolved`

- time: `2026-04-13 16:45:22 +09:00`
  location: `workspace search`
  summary: `rg 실행 권한 오류 재발`
  details: `rg --files 실행이 Access is denied 로 실패해 Get-ChildItem 과 직접 파일 읽기로 우회했다`
  status: `resolved`

- time: `2026-04-13 17:42:29 +09:00`
  location: `fresh collector sample probe`
  summary: `Python zoneinfo Asia/Seoul 로드 실패`
  details: `Windows Python 환경에 tzdata 패키지가 없어 ZoneInfo('Asia/Seoul') 초기화가 실패했다. 로컬 시스템 시간이 이미 +09:00 이므로 timezone 의존 없이 datetime.now() 기반으로 재실행한다.`
  status: `resolved`

- time: `2026-04-13 17:44:47 +09:00`
  location: `CGV fresh collector sample probe`
  summary: `CGV 최신 샘플 수집 보류`
  details: `.env 에 TIDB 연결 값은 있으나 CGV_API_SECRET 값이 없어 CGV API 클라이언트 초기화가 RuntimeError 로 중단되었다. 롯데시네마와 메가박스 최신 샘플, 기존 CGV 수집기 필드 매핑, 기존 좌석 fixture 를 기준으로 스키마 검토를 계속한다.`
  status: `deferred`

- time: `2026-04-13 17:45:30 +09:00`
  location: `scripts/db/002_showtime_search_metrics.sql`
  summary: `TiDB ALTER TABLE 컬럼 순서 참조 실패`
  details: `한 ALTER TABLE 문 안에서 sold_seat_count 를 추가한 직후 seat_occupancy_rate 의 AFTER 기준으로 다시 참조하자 TiDB가 Unknown column 을 반환했다. 컬럼별 ALTER TABLE 문으로 쪼개 재적용한다.`
  status: `resolved`

## 2026-04-14 10:26:29 +09:00

- time: `2026-04-14 10:26:29 +09:00`
- location: `git rm --cached -- AGENTS.md ERROR_LOG.md STATE.md WORKSPACE_CONTEXT.toml`
- summary: `Index cleanup initially failed because STATE.md had staged content different from both HEAD and the working tree.`
- details: `Retried with git rm --cached -f, which removed only index entries and preserved local files.`
- status: `resolved`

- time: `2026-04-14 13:06:01 +09:00`
- location: `scripts/db/apply_migrations.py`
- summary: `003 마이그레이션 적용 중 SQL 분리 실패`
- details: `003_raw_object_keys.sql 주석의 세미콜론을 단순 split 로직이 SQL 구분자로 처리해 TiDB가 주석 일부를 SQL로 받아 문법 오류를 반환했다. 주석 제거 후 statement를 분리하도록 마이그레이션 러너를 보강한다.`
- status: `open`

- time: `2026-04-14 13:06:37 +09:00`
- location: `scripts/db/apply_migrations.py`
- summary: `SQL 분리 실패 해결`
- details: `마이그레이션 러너가 -- line comment 를 제거한 뒤 statement 를 분리하도록 수정해 주석 내 세미콜론 문제는 해결됐다.`
- status: `resolved`

- time: `2026-04-14 13:06:37 +09:00`
- location: `db/migrations/003_raw_object_keys.sql`
- summary: `TiDB multi-column ALTER 순서 참조 실패`
- details: `movies 테이블에서 raw_object_key 를 추가한 같은 ALTER 문 안에서 raw_object_etag 의 AFTER raw_object_key 를 참조하자 TiDB가 Unknown column 을 반환했다. 002와 같은 방식으로 컬럼별 ALTER TABLE 문으로 쪼갠다.`
- status: `open`

- time: `2026-04-14 13:07:00 +09:00`
- location: `db/migrations/003_raw_object_keys.sql`
- summary: `TiDB multi-column ALTER 순서 참조 실패 해결`
- details: `003 마이그레이션의 raw_object_* 컬럼 추가를 컬럼별 ALTER TABLE 문으로 분리했고 재실행 결과 schema_migrations 가 001,002,003 상태로 확인됐다.`
- status: `resolved`

- time: `2026-04-14 15:12:22 +09:00`
- location: `scripts/cgv_collector_demo.py, scripts/cgv_api_probe.py`
- summary: `CGV 최신 데이터 추출 보류`
- details: `롯데시네마 최신 데이터 추출은 성공했지만 CGV 스크립트는 CGV_API_SECRET 환경변수가 없어 RuntimeError 로 중단됐다. 비밀값은 출력하지 않았고 CGV 수집은 secret 설정 후 재실행해야 한다.`
- status: `deferred`

- time: `2026-04-14 15:21:00 +09:00`
- location: `collectors/cgv/api.py, scripts/cgv_collector_demo.py, scripts/cgv_api_probe.py`
- summary: `CGV 서명 API 호출은 secret 부재로 계속 보류`
- details: `CGV_API_SECRET 를 환경변수 또는 루트 .env 에서 읽도록 보강했고 누락 시 스택트레이스 없이 명확한 오류로 종료하게 했다. 현재 로컬 .env 에 CGV_API_SECRET 값이 없어 실제 CGV API 호출은 아직 실행하지 못했다.`
- status: `deferred`

- time: `2026-04-14 15:29:00 +09:00`
- location: `.local/api-responses/fresh-cgv-20260414-152645`
- summary: `CGV 서명 API 최신 샘플 수집 성공`
- details: `사용자가 로컬 .env 에 CGV_API_SECRET 을 설정한 뒤 CGV 영화, 속성, 지역, 극장, 날짜, 상영, 좌석 샘플 수집이 성공했다. 비밀값은 출력하지 않았고 raw/normalized JSON 은 ignored .local 경로에 저장했다.`
- status: `resolved`

- time: `2026-04-14 15:29:00 +09:00`
- location: `PowerShell ConvertFrom-Json over CGV UTF-8 JSON`
- summary: `PowerShell 기본 인코딩으로 CGV JSON 파싱 실패`
- details: `Python 이 UTF-8 JSON 을 정상 저장했지만 Windows PowerShell Get-Content 기본 인코딩으로 읽자 한글 바이트가 깨져 ConvertFrom-Json 이 실패했다. Get-Content -Encoding UTF8 로 재실행해 정상 집계했다.`
- status: `resolved`

- time: `2026-04-15 17:35:00 +09:00`
- location: `backend verification`
- summary: `Java/Gradle PATH 부재로 백엔드 테스트 실행 보류`
- details: `로컬 Gemma 추천 API 구현 후 java -version 과 gradle test 를 실행했지만 둘 다 PATH 에서 명령을 찾지 못해 테스트를 실행하지 못했다. JDK 21 과 Gradle 경로 설정 후 gradle test 를 재실행해야 한다.`
- status: `deferred`

- time: `2026-04-16 10:43:22 +09:00`
- location: `backend gradle test after JDK/Gradle install`
- summary: `백엔드 컴파일 실패`
- details: `JDK 21, Gradle 8.14.4 설치 후 backend 에서 gradle test 를 재실행했으나 RecommendationService.java:103 의 lambda 캡처 변수가 effectively final 이 아니어서 compileJava 단계에서 실패했다. 설치 검증은 통과했고, 코드 수정 후 gradle test 재실행이 필요하다.`
- status: `open`

- time: `2026-04-16 14:50:44 +09:00`
- location: `repository root gradle verification`
- summary: `Gradle 테스트를 레포 루트에서 실행해 빌드 루트를 찾지 못함`
- details: `gradle test --tests kr.daboyeo.backend.service.recommendation.PreferenceProfileBuilderTests 를 C:\lsh\git\daboyeo 에서 실행해 settings.gradle/build.gradle 을 찾지 못했다. 같은 명령을 C:\lsh\git\daboyeo\backend 에서 재실행해 성공했다.`
- status: `resolved`

- time: `2026-04-16 17:23:52 +09:00`
- location: `backend Gradle verification`
- summary: `동일 Gradle build 디렉터리를 대상으로 테스트를 병렬 실행해 test-results 삭제 충돌 발생`
- details: `PreferenceProfileBuilderTests 와 recommendation 패키지 테스트를 동시에 실행하면서 C:\lsh\git\daboyeo\backend\build\test-results\test\binary\output.bin 삭제가 실패했다. 코드 실패가 아니라 검증 명령 병렬화 문제이므로 테스트를 직렬로 재실행한다.`
- status: `resolved`

- time: `2026-04-17 09:51:51 +09:00`
- location: `LM Studio direct chat completion verification`
- summary: `LM Studio가 OpenAI json_object response_format 값을 거부함`
- details: `POST http://127.0.0.1:1234/v1/chat/completions 에 response_format.type=json_object 를 보내자 'response_format.type' must be 'json_schema' or 'text' 오류가 반환됐다. 백엔드 구현은 response_format.type=text 와 기존 JSON-only 프롬프트/파서 fallback 조합으로 고정했고, 직접 호출 및 Gradle 테스트를 재실행해 통과했다.`
- status: `resolved`

- time: `2026-04-17 14:47:29 +09:00`
- location: `local recommendation E2E verification`
- summary: `세션 API가 DB 준비와 JDBC URL 보간 문제로 503 반환`
- details: `초기 /api/recommendation/sessions 호출은 추천 저장 테이블이 없어 503을 반환했다. 기존 V004 anonymous recommendation migration만 적용해 테이블을 생성했다. 이후 Java/Spring 실행용 JDBC URL을 PowerShell에서 만들 때 $database?serverTimezone 형태가 잘못 보간되어 DB명이 깨졌고 Java 연결 테스트에서 Unknown database 오류가 확인됐다. ${database}?serverTimezone 형태로 보간을 수정해 java -jar 백엔드를 재시작한 뒤 세션 생성과 추천 no_candidates 응답이 200으로 통과했다.`
- status: `resolved`

- time: `2026-04-17 15:17:55 +09:00`
- location: `scripts/ingest/collect_all_to_tidb.py lotte ingest`
- summary: `롯데 실제 수집 데이터의 24시 이후 종료 시간이 DB datetime 입력을 막음`
- details: `롯데 실제 ingest 실행 중 원본 종료 시간이 24:22 형태로 들어와 showtimes.ends_at 입력에서 Incorrect datetime value 오류가 발생했다. fake seed 대신 실제 수집 데이터를 쓰기 위해 수집 스크립트의 시간 정규화가 필요하다.`
- status: `open`

- time: `2026-04-17 15:44:23 +09:00`
- location: `scripts/ingest/collect_all_to_tidb.py lotte ingest`
- summary: `롯데 24시 이후 종료 시간 정규화 해결`
- details: `DB datetime 변환에서 24:22, 2515 같은 시간을 다음날 00:22:00, 01:15:00으로 정규화하도록 수정했다. 롯데 실제 ingest를 재실행해 실제 상영 20건이 upsert됐다.`
- status: `resolved`

- time: `2026-04-17 15:31:03 +09:00`
- location: `backend recommendation API to LM Studio`
- summary: `LM Studio 호출이 무기한 대기해 추천 API가 응답하지 않음`
- details: `실제 수집 후보가 있는 fast 추천 요청에서 Spring RestClient가 LM Studio /chat/completions 응답을 기다리며 180초 이상 반환하지 않았다. Java thread dump에서 LocalModelRecommendationClient.callLmStudio 내부 HTTP 요청 대기가 확인됐고, 추천 API가 fallback으로도 빠지지 못했다.`
- status: `open`

- time: `2026-04-17 15:44:23 +09:00`
- location: `backend recommendation API to LM Studio`
- summary: `LM Studio 호출 대기 및 JSON 잘림 해결`
- details: `LM Studio 호출에 connect/read timeout을 추가하고 JSON schema 문자열 maxLength 및 max_tokens 260을 적용했다. E2B fast와 E4B precise 모두 실제 수집 후보로 status=ok, recommendation_count=3 응답을 확인했다.`
- status: `resolved`

- time: `2026-04-20 09:54:16 +09:00`
- location: `backend recommendation API to LM Studio`
- summary: `최적화 중 토큰 예산 과축소로 AI 응답 JSON 잘림`
- details: `fast 200, precise 240 max_tokens 설정에서 두 모델 모두 3번째 추천 JSON이 잘려 API가 fallback으로 응답했다. 후보/프롬프트 축소는 유지하고 fast 280, precise 320으로 토큰 예산을 올려 fast와 precise 모두 status=ok를 확인했다.`
- status: `resolved`

- time: `2026-04-20 10:05:16 +09:00`
- location: `scripts/ingest/collect_all_to_tidb.py, showtimes.movie_id`
- summary: `메가박스 수집 데이터에서 showtimes.movie_title 과 movies.title_ko 불일치 발견`
- details: `대량 수집 후 검증 쿼리에서 showtimes.movie_title <> movies.title_ko 인 행이 139개 확인됐다. 특히 하나의 movie_id가 여러 메가박스 상영 제목에 연결되어 추천 응답의 movieId와 피드백 대상 정합성이 흔들릴 수 있다.`
- status: `open`

- time: `2026-04-20 10:24:00 +09:00`
- location: `backend recommendation focused tests`
- summary: `영화 다양성 테스트 기대값 불일치`
- details: `제목 우선 중복 제거 정책으로 고정한 뒤 기존 테스트가 서로 다른 제목이 같은 movie_id 를 가진 경우까지 중복으로 기대해 실패했다. 메가박스 과거 매핑 오류를 고려해 같은 제목의 다른 시간표를 중복으로 보는 테스트로 수정했고 추천 패키지 테스트를 재실행해 통과했다.`
- status: `resolved`

- time: `2026-04-20 10:55:00 +09:00`
- location: `scripts/ingest/collect_all_to_tidb.py, showtimes.movie_id`
- summary: `메가박스 movie_id 매핑 오류 해결`
- details: `메가박스 상영 row의 movieNo 를 우선 사용해 영화 row를 upsert하고, showtimes에는 있으나 movies에는 없는 external_movie_id 를 raw showtime 기반으로 backfill한 뒤 showtimes.external_movie_id 와 movies.external_movie_id 기준으로 기존 연결을 보정하도록 수정했다. 검증 결과 오늘 메가박스 title mismatch 0, multi-title movie_id 그룹 0, 전체 external link mismatch 0을 확인했다.`
- status: `resolved`

- time: `2026-04-20 12:45:00 +09:00`
- location: `frontend benchmark via Chrome CDP`
- summary: `프론트 벤치마크 초기 CDP 타임아웃`
- details: `Chrome /json/list 의 첫 target 이 page가 아니라 extension background_page 여서 벤치 DOM 완료 신호를 읽지 못하고 두 차례 타임아웃됐다. 이후 type=page target만 선택하도록 벤치 드라이버를 수정해 fast/precise 5회 측정을 완료했다.`
- status: `resolved`

- time: `2026-04-20 14:10:33 +09:00`
- location: `frontend benchmark via Chrome CDP`
- summary: `벤치 드라이버 WebSocket 경로와 소켓 타임아웃 문제`
- details: `수동 CDP WebSocket 연결에서 query가 없는 target path 뒤에 불필요한 ?를 붙여 Chrome이 target id를 찾지 못했고, 수정 후에는 기본 socket timeout 30초가 추론 중 먼저 만료됐다. 요청 측정값으로 쓰지 않고 request path 처리와 socket timeout을 수정한 뒤 fast/precise 5회 벤치를 완료했다.`
- status: `resolved`

- time: `2026-04-20 14:50:59 +09:00`
- location: `backend restart for compact JSON frontend benchmark`
- summary: `백엔드 재시작 시 Spring DB 환경변수 매핑 누락으로 세션 API 503`
- details: `.env`에는 TIDB_HOST/TIDB_PORT/TIDB_USER/TIDB_PASSWORD/TIDB_DATABASE가 있었지만 Spring은 DABOYEO_DB_URL/DABOYEO_DB_USERNAME/DABOYEO_DB_PASSWORD를 사용한다. 첫 재시작은 이 매핑 없이 기본 datasource로 떠서 /api/recommendation/sessions가 503을 반환했다. 값은 출력하지 않고 TiDB env에서 Spring datasource env를 구성해 재시작한 뒤 health와 session create/delete를 확인하고 벤치를 재실행했다.`
- status: `resolved`

- time: `2026-04-20 16:12:39 +09:00`
- location: `backend recommendation focused tests`
- summary: `analysisPoint를 precise 전용으로 바꾸면서 fast 기대값 테스트가 실패`
- details: `기존 품질 테스트 3개가 fast/E2B 응답에도 analysisPoint=#애니메이션취향 이 있다고 기대해 실패했다. 새 계약에 맞춰 fast는 blank analysisPoint, precise/E4B는 selected-poster genre analysisPoint를 기대하도록 테스트를 수정했고 focused/package/full Gradle 테스트를 재실행해 통과했다.`
- status: `resolved`

- time: `2026-04-21 20:16:00 +09:00`
  location: `STATE.md`
  summary: `작업 재분류 중 STATE.md 인코딩 손상`
  details: `Stitch-led AI 페이지 리뉴얼 태스크로 보드를 갱신하는 중 STATE.md 가 혼합 인코딩으로 깨져 읽기 어려운 상태가 됐다. 현재 보드를 UTF-8로 재구성해 계속 진행한다.`
  status: `resolved`

- time: `2026-04-21 20:18:00 +09:00`
  location: `mcp__stitch__.create_design_system, mcp__stitch__.generate_screen_from_text`
  summary: `Stitch 디자인 시스템/화면 생성 호출 실패`
  details: `create_design_system 은 invalid argument 를 반환했고, 이어진 generate_screen_from_text 는 service unavailable 을 반환했다. Stitch를 디자인 근거 시도로는 사용했지만 실제 화면 구현은 동일한 브리프를 기반으로 로컬 코드에서 우회했다.`
  status: `deferred`
## 2026-04-21 17:17:07 +09:00
- time: `2026-04-21 17:17:07 +09:00`
- location: `mcp__stitch__.edit_screens`
- summary: `Results-screen Stitch regeneration timed out during edit_screens call`
- details: `A focused dark-layout redesign prompt was sent to Stitch for screen a11a57f615384ba28f95ce67ac495b55 in project 7742688576431333902. The tool timed out after 120 seconds. Per tool instructions, the generation may still have completed server-side, so the next step is to verify with get_screen instead of retrying immediately.`
- status: `open`

## 2026-04-21 17:17:07 +09:00
- time: `2026-04-21 17:17:07 +09:00`
- location: `mcp__stitch__.generate_variants`
- summary: `Results-screen variant generation rejected the variantOptions argument`
- details: `Tried to generate one dark-theme variant from screen a11a57f615384ba28f95ce67ac495b55 in project 7742688576431333902, but Stitch returned 'Request contains an invalid argument.' The next fallback is to generate a fresh results screen from text in the same project instead of variant mode.`
- status: `open`

## 2026-04-21 17:17:07 +09:00
- time: `2026-04-21 17:17:07 +09:00`
- location: `mcp__stitch__.generate_screen_from_text`
- summary: `Results-screen redesign recovered via short prompt fallback`
- details: `After one timed-out long prompt and one rejected variant call, a shorter direct generation prompt succeeded in project 7742688576431333902 and created screen 8c8ebac647434c24a68f40e105393323 titled AI 추천 결과 (Dark Cinematic Stage). This resolves the immediate need for a dark replacement results screen.`
- status: `resolved`

- time: `2026-04-22 13:10:11 +09:00`
  location: `mcp__stitch__.create_design_system`
  summary: `1440x1026 PPT 새 세션 생성 중 Stitch 디자인 시스템 생성 실패`
  details: `새 PPT 프로젝트 13482283388031437931 에서 스타일을 먼저 고정하려고 create_design_system 을 호출했지만 'Request contains an invalid argument.' 를 반환했다. 작업은 각 slide generation prompt 에 스타일 규칙을 직접 포함하는 방식으로 우회했다.`
  status: `resolved`

- time: `2026-04-22 13:10:11 +09:00`
  location: `mcp__stitch__.generate_screen_from_text`
  summary: `Stitch가 1440x1026 exact frame size를 보장하지 않음`
  details: `사용자 요구는 1440x1026 발표 프레임이었지만 생성된 screen 메타데이터는 2560x2052, 2880x2052, 2752x2240 등 더 큰 기본 desktop canvas 로 반환됐다. 내용과 톤은 생성했지만 exact pixel 규격은 현재 Stitch 생성 경로에서 강제되지 않았다.`
  status: `open`

- time: `2026-04-22 13:10:11 +09:00`
  location: `mcp__stitch__.list_screens`
  summary: `생성 후 list_screens 결과가 비어 있음`
  details: `프로젝트 13482283388031437931 에 여러 screen 생성 호출이 성공했지만 list_screens 는 빈 객체를 반환했다. 이번 턴의 산출물 확인은 각 generate_screen_from_text 성공 응답의 screen id 와 screenshot metadata 를 근거로 했다.`
  status: `open`

- time: `2026-04-22 14:20:00 +09:00`
  location: `mcp__stitch__.generate_screen_from_text`
  summary: `UI/UX 슬라이드 생성 응답이 120초 타임아웃으로 끊김`
  details: `프로젝트 10979052864160268633 에서 '7. 주요 화면 (UI/UX)' 슬라이드를 생성하던 중 Stitch 호출이 120초 후 타임아웃됐다. 도구 안내상 서버 쪽 생성이 계속됐을 가능성은 있지만, 같은 환경에서 list_screens 도 빈 객체를 반환해 즉시 복구 확인을 하지 못했다.`
  status: `open`

- time: `2026-04-22 14:38:20 +09:00`
  location: `tool discovery for Figma resizing`
  summary: `현 세션에 exact 1440x1026 프레임 리사이즈용 Figma write tool 이 노출되지 않음`
  details: `사용자가 rebuilt deck 의 정확한 1440x1026 사이즈를 요구해 도구 탐색을 다시 수행했지만, 현재 세션에서는 Stitch generate/edit 와 Figma read/generate 계열만 확인됐고 기존 슬라이드 프레임 크기를 직접 수정하는 write/resize 액션은 찾지 못했다. 이 때문에 현재 자동 경로로는 exact pixel size enforcement 가 막혀 있다.`
  status: `open`
