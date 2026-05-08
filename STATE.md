## Current Task
- task: implement LOTTE theater-first nearby collection using GetPlaySequence with an empty movie code
- phase: implement
- reason: read-only verification proved the existing LOTTE GetPlaySequence call returns full theater schedules when representationMovieCode is blank, so we can replace movie-first probing with a theater-first collector path

## Orchestration Profile
- score_total: 8
- score_breakdown: collector coverage 3, nearby runtime impact 2, multi-file contract change 2, verification sensitivity 1
- hard_triggers: none
- selected_rules: verification_required
- selected_skills: none
- selection_reason: the endpoint uncertainty is resolved, but the implementation still spans the LOTTE collector path plus nearby orchestration, so one local lane with tight verification is safer than splitting writes
- execution_topology: single-session
- orchestration_value: low
- evaluation_need: light
- agent_budget: 0
- spawn_decision: no-spawn
- efficiency_basis: the rewrite touches one tightly coupled collector flow where bridge, collector, and nearby service must stay aligned under one write owner

## Writer Slot
- writer_slot: main
- write_sets: STATE.md, collectors/lotte/collector.py, backend/src/main/java/kr/daboyeo/backend/sync/bridge/PythonCollectorBridge.java, backend/src/main/java/kr/daboyeo/backend/sync/nearby/NearbyShowtimeRefreshService.java

- contract_freeze: keep the frontend and public nearby response schema unchanged, switch only the LOTTE nearby collector path from movie-first discovery to one theater-first schedule fetch per cinema, and preserve the existing ingest contract
- non_goals: homepage UI redesign, CGV candidate sourcing changes, touching MEGABOX flow, or solving the unrelated full test-suite compile drift
- task_acceptance:
  - LOTTE nearby refresh no longer depends on representationMovieCode probing
  - one theater-first fetch per LOTTE cinema can produce the same ingestible bundle shape as before
  - Hongdae rerun shows broader LOTTE movie coverage than the current movie-first path
- hard_checks:
  - confirm the new theater-first path still produces a valid bundle for persistence
  - confirm Hongdae local nearby rerun emits broader LOTTE results after restart

## Reviewer
- reviewer: main
- llm_review_rubric: confirm the theater-first rewrite is limited to the LOTTE nearby collector path and preserves the existing ingest contract
- evidence_required: diff plus runtime verification

## Last Update
- 2026-05-08 KST: reclassified from read-only endpoint investigation to implementation after verifying that GetPlaySequence returns full theater schedules when representationMovieCode is blank for Hongdae and Hapjeong
- 2026-05-08 KST: reclassified from LOTTE diagnostics to a read-only theater-first endpoint investigation after the new logs showed Hongdae theaters probing 41 movies but still matching only one or two titles
- 2026-05-08 KST: reclassified from LOTTE limit tuning to nearby diagnostics after the 100/8 rerun left Hongdae LOTTE breadth unchanged at two titles and eleven cards
- 2026-05-08 KST: reclassified from theater-first parser design back to bounded implementation after the user chose the 100/8 LOTTE discovery tuning path for immediate validation
- 2026-05-08 KST: reclassified from LOTTE discovery-limit tuning to a read-only parser-design pass after runtime verification showed Hongdae LOTTE breadth improved to two titles but still lagged the real theater lineup
- 2026-05-08 KST: reclassified from MEGABOX retry hardening to LOTTE discovery widening after confirming nearby LOTTE collection only scans the first 20 movie candidates and can miss real-site Hongdae showtimes
- 2026-05-08 KST: reclassified from nearby breadth repair to MEGABOX collector hardening after log inspection showed repeated `Workload is so high` responses from `schedulePage.do`, while CGV had zero nearby candidates and is intentionally out of scope
- 2026-05-08 KST: reclassified from frontend API-base repair to nearby search contract repair after confirming `LiveMovieService` still collapses nearby rows to one closest theater per provider, which starves the direct-search page on thin Render datasets
- 2026-05-08 KST: reclassified from Render Flyway startup analysis to frontend deploy routing repair after confirming `client.js` and `liveMovies.js` still default to `http://localhost:8080`, which breaks Render-hosted browser requests
- 2026-05-08 KST: reclassified from event deployment hardening to Render startup repair after a new stack trace showed `flywayInitializer` failing with `Unable to restore connection to its original state` while wiring the `LiveMovie` datasource path
- 2026-05-08 KST: hardened the event path for deployment by adding cached live-crawl fallback in `MovieEventService`, exposing `DABOYEO_EVENT_FALLBACK_CACHE_MINUTES`, and re-verifying `compileJava`, `bootJar`, `/api/health`, `/api/events`, homepage preview cards, and event-page cards against the local runtime
- 2026-05-08 KST: fixed the `/api/events` 503 path by making `MovieEventService` fall back to live crawler results when the repository query fails, then verified that the homepage shows 4 preview cards and `src/pages/events.html` renders 6 event cards against the local backend
- 2026-05-08 KST: verification showed the backend boots and `/api/health` returns 200, but `/api/events` currently returns `503 DATA_UNAVAILABLE` with `BadSqlGrammarException`, so the homepage event preview falls back to its empty state and `src/pages/events.html` shows its error state even though the page wiring itself loads correctly
- 2026-05-08 KST: reclassified the event frontend port into a homepage-preview integration after the user asked to continue by adding the branch-style event teaser section to `index.html`
- 2026-05-08 KST: reclassified the frontend merge from route-only alignment to an event-page port after confirming `origin/feature/ksg-event` stores the event UI as an isolated static page plus scripts that can be adapted into the current `frontend/src/pages` structure
- 2026-05-08 KST: reclassified the frontend merge to route-only work after the user excluded the search box and map flow, which made `src/pages` navigation alignment the safe adoption target
- 2026-05-08 KST: reclassified from documentation-only branch adoption to a bounded event-feature port after confirming `lsh` DB wording is partly reusable and `feature/ksg-event` requires new Flyway versions to coexist with the current `kmh` schema chain
- 2026-05-08 KST: reclassified the task to selective branch adoption after a `ksg` merge attempt showed large backend deletions, then narrowed the write set to low-risk documentation files only
- 2026-05-08 KST: expanded the Render deployment repair to include container port binding after finding the Docker exec-form entrypoint passes the literal string `${PORT}` instead of Render's numeric port
- 2026-05-08 KST: reclassified from nearby response filtering to a Render deployment repair after Flyway startup failed on a non-empty schema with no `flyway_schema_history`, and inspection showed backend-packaged migration versions diverged from `db/migrations`
- 2026-05-07 KST: updated `LiveMovieService` to keep only the nearest theater per provider in nearby direct-compare results and verified it with `LiveMovieServiceNearbyRefreshTests`
- 2026-05-07 KST: reclassified from nearby collection recovery to a provider-scoped nearby result contract change after the user clarified they want one closest theater per provider such as one LOTTE and one MEGA
- 2026-05-07 KST: reclassified from homepage region UI work to a backend nearby runtime repair after direct-compare verification showed LOTTE and MEGABOX missing and backend logs pinned both failures to `collectors/__init__.py` importing the removed `collectors.cgv` module
- 2026-05-07 KST: reclassified the homepage region task from Kakao-only dynamic dropdowns to a simpler single-address-input flow after the user asked to replace the dropdown menus with automatic region address filling from map search
- 2026-05-07 KST: removed the homepage dropdown's dependency on static all-region data by rewriting `frontend/src/js/pages/script.js` to keep the region controls disabled until Kakao map search injects dynamic options into `dynamicRegions`, while `kakaoMap.js` continues to feed resolved addresses into `window.updateRegionFromMap`
- 2026-05-07 KST: reclassified from hardcoded-region sync repair to a Kakao-only dropdown source refactor after the user requested that homepage region options be populated only from map search results
- 2026-05-07 KST: rewrote `frontend/src/js/pages/script.js` region sync helpers with clean parsing and dong normalization, then reloaded the homepage and verified that map-search input updates the dropdowns to the resolved Seoul/Mapo/Donggyo path
