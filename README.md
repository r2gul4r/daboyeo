# daboyeo backend portfolio

Backend portfolio project for movie showtime ingest, live search APIs, seat snapshot persistence, and recommendation experiments.

## What this project shows

- Spring Boot backend API design
- Shared normalization pipeline for CGV, LOTTE CINEMA, and MEGABOX data
- Location-based live movie search APIs
- Seat snapshot collection and persistence structure
- Local LLM recommendation integration experiments

## Repo layout

- `backend/`: Spring Boot backend
- `collectors/`: Python collectors
- `db/sql/`: schema and migration source SQL
- `scripts/`: helper scripts for ingest and inspection

## Current stage

This repository is strong enough for portfolio review, but it is not positioned as a production-ready public service.

- Build artifact exists: `backend/build/libs/daboyeo-backend-0.1.0-SNAPSHOT.jar`
- Previous test results exist under `backend/build/test-results/test/`
- Gradle wrapper is not committed yet
- Local `gradle` was not available in the current environment during cleanup

## Quick start

### 1. Minimal demo path

Goal: verify the backend without requiring external DB data, collectors, or LM Studio.

```powershell
cd backend
java -jar build/libs/daboyeo-backend-0.1.0-SNAPSHOT.jar
```

Then try:

- `GET http://localhost:8080/api/health`
- `GET http://localhost:8080/api/live/nearby?lat=37.4979&lng=127.0276`
- `GET http://localhost:8080/api/live/movies/CGV:demo_dune/schedules?lat=37.4979&lng=127.0276`

If database lookup fails, the live API can return deterministic sample data in demo fallback mode.

## Portfolio demo sequence

Use this exact order when showing the project:

1. `GET /api/health`
2. `GET /api/live/nearby?lat=37.4979&lng=127.0276`
3. `GET /api/live/movies/CGV:demo_dune/schedules?lat=37.4979&lng=127.0276`
4. Show `README.md`, `.env.example`, and the backend service classes together

This sequence works even when DB access is unavailable because the live endpoints can switch to sample data fallback.

### Example health response

```json
{
  "status": "ok",
  "service": "daboyeo-backend",
  "time": "2026-04-28T12:00:00+09:00"
}
```

### Example live nearby response shape

```json
{
  "search": {
    "lat": 37.4979,
    "lng": 127.0276,
    "date": "2026-04-28",
    "timeStart": "06:00",
    "timeEnd": "23:59",
    "radiusKm": 8,
    "resultCount": 3,
    "databaseAvailable": false,
    "warning": "demo sample data returned because database lookup failed."
  },
  "results": [
    {
      "movie_key": "CGV:demo_dune",
      "movie_name": "Dune Part Two",
      "provider": "CGV",
      "theater_name": "CGV Gangnam",
      "seat_state": "comfortable"
    }
  ]
}
```

### 2. Development path

The repository currently does not include `gradlew` or `gradlew.bat`.

- If Gradle is installed locally, use Gradle 8.9+ with Java 21
- If Gradle is not installed, use the prebuilt jar path above for a limited demo

Example:

```powershell
cd backend
gradle test
gradle bootRun
```

## Configuration

Environment variables are documented in the root `.env.example`.

### Minimal backend boot

- No variables are required if you only want `/api/health`
- `DABOYEO_DEMO_LIVE_FALLBACK_ENABLED=true` is recommended for demo-safe live API responses without DB data

### Data-backed APIs

- `DABOYEO_DB_URL`
- `DABOYEO_DB_USERNAME`
- `DABOYEO_DB_PASSWORD`

### Optional features

- Collector login and storage variables for Python collectors
- `DABOYEO_FLYWAY_ENABLED=true` to apply backend migrations through Spring Boot
- LM Studio variables for recommendation endpoints
- Sync variables for scheduled collector jobs

## Recommended portfolio demo scope

### Stable to show

- `/api/health`
- live movie search API shape with demo fallback
- grouped schedules API shape with demo fallback
- schema and ingest pipeline design
- request validation and API error shape

### Good advanced talking points

- scheduled sync jobs
- seat snapshot persistence
- local LLM reranking

## Known gaps

- Gradle wrapper is missing, so setup is less reproducible than it should be
- Fresh test execution was blocked here because `gradle` was not installed in PATH
- Some existing Korean strings in source and resources still need an encoding cleanup pass
- Full demo quality still depends on DB contents, collector setup, and local model availability
- Live API demo fallback returns sample data, not real-time theater data
- The example responses in this README are shape-oriented and omit some fields for brevity

## Next finish priorities

1. Add a working Gradle wrapper
2. Clean up encoding-sensitive user-facing strings
3. Define a sample-data-backed demo path for live movie endpoints
4. Rerun test and build verification in a reproducible environment
