# Backend Flyway Migrations

Spring Boot가 classpath에서 읽는 Flyway 마이그레이션 위치다.

현재는 기존 `scripts/db/*.sql` 이력과 충돌하지 않도록 실행 SQL을 넣지 않았다. DB 스키마를 Flyway로 완전히 전환할 때 이 폴더에 아래 형식으로 추가한다.

```text
V1__init_schema.sql
V2__add_showtime_search_metrics.sql
```

기본 설정은 `DABOYEO_FLYWAY_ENABLED=false`라서 서버 실행만으로 DB가 바뀌지 않는다.
