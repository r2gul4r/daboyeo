# Database

## Canonical schema contract

- `SCHEMA_CONTRACT.md`: DB 네이밍, provider code, table 역할, stable key, 검색/추천 기준을 고정해둔 기준 문서.
- `migrations/`: 실제 TiDB/MySQL DDL의 기준 위치.

새 환경에서 작업을 이어갈 때는 먼저 `SCHEMA_CONTRACT.md`를 읽고 테이블/컬럼명을 맞춘다.

TiDB/MySQL 관련 스키마, seed, 샘플 데이터의 소유 영역이다.

## 디렉터리

- `migrations/`: 수동 TiDB migration의 canonical 위치. `scripts/db/apply_migrations.py`가 여기의 `*.sql`을 순서대로 실행한다.
- `seeds/`: 로컬 개발용 기준 데이터. 실제 계정, 토큰, 쿠키, 개인 위치 정보는 넣지 않는다.
- `samples/`: 공유 가능한 작은 샘플만 둔다. 대용량 raw JSON은 Cloudflare R2 또는 `.local/`에 둔다.

## 원칙

- TiDB에는 검색/비교에 필요한 최소 공통 컬럼을 둔다.
- 제공사별 원본 응답은 `raw_json` 또는 R2 object key로 보존한다.
- 좌석 상태는 덮어쓰기보다 `seat_snapshots` append를 기본으로 한다.
- 접속 정보, 비밀번호, R2 키는 SQL/문서에 쓰지 않는다.
