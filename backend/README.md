# Backend

Spring Boot 기반 API 서버 영역이다.

백엔드는 수집기를 대체하지 않는다. CGV, 롯데시네마, 메가박스 원본 데이터 수집은 `collectors/`와 `scripts/`가 맡고, 백엔드는 저장된 상영 데이터와 추천 상태를 조회, 정리, 응답하는 API와 서비스 로직에 집중한다.

## 기술 스택

- Java 21
- Spring Boot 3.5.13
- Gradle Wrapper
- Spring Web REST API
- Spring JDBC
- Flyway
- MySQL Connector/J
- JUnit 5 / Spring Boot Test

## 방향성

- 프론트엔드에 영화, 극장, 상영 시간, 좌석 요약, 예매 링크, 추천 결과를 제공한다.
- 검색과 비교의 기준은 `showtimes` 중심의 최소 공통 필드로 맞춘다.
- 제공사별 원본 데이터 특성은 수집기와 DB `raw_json` 또는 object key로 보존한다.
- 추천은 익명 세션 기반으로 처리하고 민감한 개인정보는 저장하지 않는다.
- 비밀값, DB 접속 정보, 모델 설정은 환경변수로 주입한다.

## 로컬 실행

```powershell
cd backend
.\gradlew.bat test
.\gradlew.bat bootRun
```

기본 포트는 `8080`이고, 배포 환경에서는 `PORT` 환경변수를 우선 사용한다.

## 주요 환경변수

- `DABOYEO_BACKEND_PORT`
- `PORT`
- `DABOYEO_DB_URL`
- `DABOYEO_DB_USERNAME`
- `DABOYEO_DB_PASSWORD`
- `DABOYEO_FLYWAY_ENABLED`
- `DABOYEO_FLYWAY_BASELINE_ON_MIGRATE`
- `DABOYEO_FLYWAY_BASELINE_VERSION`
- `DABOYEO_FRONTEND_ORIGINS`

## 패키지 경계

- `api`: HTTP 컨트롤러와 예외 응답
- `service`: 유스케이스와 응답 조합
- `repository`: DB 조회와 저장
- `domain`: 도메인 모델
- `config`: 설정 바인딩과 인프라 설정
- `sync`: 수집기 연동과 동기화 흐름
