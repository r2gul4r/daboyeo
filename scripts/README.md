# Scripts

로컬 실행, 수집 검증, DB 확인, R2 확인용 스크립트 영역이다.

## 디렉터리

- `db/`: TiDB migration 적용과 스키마 검사 실행 스크립트.
- `ingest/`: 수집 결과를 TiDB/R2 파이프라인으로 넘기는 진입점.
- `storage/`: Cloudflare R2 연결과 object 확인 스크립트.
- `verify/`: 적재 후 row count, 중복 정책, snapshot append 검증 스크립트.

## 원칙

- 실제 SQL migration 파일은 `db/migrations/`에 둔다.
- 스크립트는 `.env`를 읽되 비밀번호나 secret을 출력하지 않는다.
- 외부 쓰기 작업은 명령 이름과 출력에서 명확히 드러나야 한다.
