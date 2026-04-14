# Backend

Spring Boot 기반 API 서버 영역이다.

초기 역할은 TiDB/MySQL에 저장된 영화, 극장, 상영 시간, 가격, 좌석 요약 데이터를 조회해서 프론트엔드에 제공하는 것이다. 수집기는 기존 `collectors/`와 `scripts/`를 우선 보존하고, 백엔드는 API와 서비스 로직에 집중한다.

## 기본 방향

- Java 17
- Spring Boot 3.5.x
- REST API
- TiDB/MySQL
- JDBC 중심 접근
- Flyway 준비, 기본값은 비활성화
- 비밀값은 환경변수로만 주입

## 로컬 실행 초안

아직 Gradle Wrapper는 넣지 않았다. 팀에서 빌드 도구 확정 후 wrapper를 추가하면 된다.

```powershell
cd backend
gradle bootRun
```

DB가 없더라도 앱이 바로 죽지 않도록 Hikari fail-fast를 꺼두었다. 실제 DB 기능을 켤 때는 `.env.example`의 `DABOYEO_DB_*` 값을 참고한다.

## 패키지 경계

- `api`: HTTP 컨트롤러
- `service`: 유스케이스와 비즈니스 흐름
- `repository`: DB 조회와 저장
- `domain`: 도메인 모델과 값 객체
- `config`: 설정 바인딩과 인프라 설정
