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
