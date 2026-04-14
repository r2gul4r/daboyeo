# Workspace Tree

현재 목표 구조다.

```text
daboyeo/
  backend/
    build.gradle
    settings.gradle
    src/main/java/kr/daboyeo/backend/
      DaboyeoApplication.java
      api/
      config/
      domain/
      repository/
      service/
    src/main/resources/
      application.yml
      db/migration/
    src/test/java/kr/daboyeo/backend/
  frontend/
    index.html
    public/assets/
    src/css/
    src/js/api/
    src/js/pages/
  db/
    migrations/
    seeds/
    samples/
  collectors/
    cgv/
    lotte/
    megabox/
    common/
  scripts/
    db/
  docs/
  .local/              # 로컬 전용, Git 제외
    codex-backups/
    playwright-cli/
  README.md
```

## 책임 경계

- `frontend/`: 사용자 화면. 바닐라 HTML/CSS/JS. Bootstrap은 보류.
- `backend/`: Spring Boot API 서버. TiDB/MySQL 접근은 환경변수 기반.
- `db/`: DB 정책, seed, 샘플 데이터 책임 경계.
- `collectors/`: 극장별 원본 데이터 수집.
- `scripts/`: 로컬 검증과 수동 실행.
- `docs/`: 협업 규칙과 작업 문서.
- `.local/`: 개인 작업 흔적, 임시 파일, 캡처, 덤프, 로그. 커밋 금지.

## 루트 공개 파일 정책

루트에서 기본으로 GitHub에 올리는 문서는 `README.md`만 둔다.

아래 파일은 로컬 작업 제어용이라 Git에 올리지 않는다.

```text
AGENTS.md
ERROR_LOG.md
MULTI_AGENT_LOG.md
STATE.md
SEED.yaml
WORKSPACE_CONTEXT.toml
```

## 로컬 전용 작업공간

`.local/` 아래는 Git에 올리지 않는다. 공유해야 하는 소스, 문서, 예시는 이 안에 넣지 않는다.

```text
.local/
  codex-backups/
  playwright-cli/
  tmp/
  logs/
  captures/
  api-responses/
  db-dumps/
  screenshots/
  scratch/
```

용도:

- `tmp/`: 일회성 임시 파일
- `codex-backups/`: Codex 작업 백업
- `playwright-cli/`: Playwright CLI 캡처와 임시 산출물
- `logs/`: 로컬 실행 로그
- `captures/`: 수집기 응답 캡처
- `api-responses/`: 외부 API 응답 샘플
- `db-dumps/`: 로컬 DB 덤프
- `screenshots/`: 화면 확인 이미지
- `scratch/`: 실험 코드와 메모

## 보류

- Gradle Wrapper 추가
- Bootstrap 도입 여부
- Flyway baseline 확정
- 실제 TiDB 연결 검증
- 프론트엔드 빌드 도구 도입
