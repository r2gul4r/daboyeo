# Scripts

로컬 실행, 수집 검증, DB 확인용 스크립트 영역이다.

PowerShell 기반 작업 흐름을 우선한다. 기존 Python probe/demo 스크립트와 `scripts/db/*.sql`은 현재 수집기와 TiDB 검증의 기준 자료이므로 유지한다.

## 책임

- 수집기 단독 실행
- 외부 API shape 확인
- TiDB 수동 SQL 검증
- 자동화 전 단계의 로컬 재현
