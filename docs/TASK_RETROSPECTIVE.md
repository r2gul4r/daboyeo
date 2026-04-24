# Task Retrospective

## 2026-04-14 - workspace skeleton

- task: `Build collaboration-ready workspace skeleton`
- score_total: `5`
- selected_profile: `single-session`
- actual_topology: `main only`
- verification_outcome: `repository verification commands passed; Gradle build not run because gradle is not available on PATH`
- collisions_or_reclassifications: `reclassified from completed TiDB schema validation; no file ownership collision found`
- next_rule_change: `When Bootstrap, Flyway baseline, or build wrapper is chosen, record it as a separate stack decision before implementation.`

## 2026-04-21 - movies.html backend plan revision

- task: `Revise backend server plan for movies.html using backend/docs/frontend contract evidence`
- score_total: `5`
- evaluation_fit: `light review fit because the output is a planning artifact, not executable code`
- orchestration_fit: `single-session fit because discovery and edits were tightly coupled and small`
- predicted_topology: `main only`
- actual_topology: `main only`
- spawn_count: `0`
- rework_or_reclassification: `Reclassified from placeholder STATE to a frozen document-revision task before editing the plan`
- reviewer_findings: `Found contract gaps around API base path, endpoint naming, provider normalization, and modal/detail split`
- verification_outcome: `Re-read backend/db/collector docs and frontend liveMovies contract; no repository command verification was needed for doc-only work`
- next_gate_adjustment: `When backend implementation starts, treat API path and provider code normalization as phase-0 freeze items before writing server code`

## 2026-04-21 - project harness application

- task: `Apply the external daboyeo project harness into the workspace`
- score_total: `6`
- evaluation_fit: `light review fit because the output is rule wiring and documentation rather than runtime code`
- orchestration_fit: `single-session fit because the harness import and AGENTS wiring were tightly coupled`
- predicted_topology: `main only`
- actual_topology: `main only`
- spawn_count: `0`
- rework_or_reclassification: `Reclassified from prior doc task into a harness-adoption task before editing workspace rules`
- reviewer_findings: `Found that root markdown files are ignored by .gitignore, so the applied harness persists locally but is not git-tracked by default`
- verification_outcome: `Read external harness, imported it into PROJECT_HARNESS.md, rewired AGENTS.md, and checked persistence limits with git status and .gitignore`
- next_gate_adjustment: `If team-wide sharing is needed later, decide explicitly whether to unignore root control files or add a tracked copy under docs`

## 2026-04-21 - first backend live movie API slice

- task: `Implement GET /api/live/nearby for movies.html`
- score_total: `8`
- evaluation_fit: `light review fit because the slice is implementation-heavy but still centered on one API contract`
- orchestration_fit: `single-session fit because controller/service/repository/frontend alignment shared one write lane`
- predicted_topology: `main only`
- actual_topology: `main only`
- spawn_count: `0`
- rework_or_reclassification: `Reclassified from harness application to backend implementation before editing code`
- reviewer_findings: `Adjusted seat-state handling so group remains a filter condition instead of swallowing spacious/comfortable states`
- verification_outcome: `Schema and frontend contract were re-read; code was implemented; gradle test could not run because gradle is unavailable on PATH and was logged to ERROR_LOG.md`
- next_gate_adjustment: `Before the next backend slice, add a Gradle wrapper or install gradle so repository verification is no longer blocked`

## 2026-04-21 - single-file TiDB schema export

- task: `Create one copy-pasteable TiDB schema SQL file`
- score_total: `4`
- evaluation_fit: `light review fit because the output is one static SQL artifact`
- orchestration_fit: `single-session fit because the task was one-file consolidation`
- predicted_topology: `main only`
- actual_topology: `main only`
- spawn_count: `0`
- rework_or_reclassification: `Reclassified from backend implementation into a bounded DB artifact task before writing`
- reviewer_findings: `Merged the search metric additions from 002 directly into the final showtimes definition and retained seed inserts`
- verification_outcome: `Re-read 001 and 002 source SQL and reviewed the saved merged file under db/sql`
- next_gate_adjustment: `If schema evolution continues, keep migration history in scripts/db and regenerate this single-file bootstrap artifact when needed`

## 2026-04-21 - movie schedules detail slice

- task: `Implement GET /api/live/movies/{movieKey}/schedules and connect the modal`
- score_total: `6`
- evaluation_fit: `light review fit because the slice stayed inside one API contract and one frontend integration surface`
- orchestration_fit: `single-session fit because backend and modal changes shared one tight contract`
- predicted_topology: `main only`
- actual_topology: `main only`
- spawn_count: `0`
- rework_or_reclassification: `Reclassified from db folder cleanup into the next backend slice before editing code`
- reviewer_findings: `Separated modalRawSchedules from allRawSchedules so the modal API response does not corrupt the main list state`
- verification_outcome: `Gradle test and bootJar both succeeded using an explicit local Gradle path and a workspace-local GRADLE_USER_HOME`
- next_gate_adjustment: `Next backend slice can focus on booking-link UX or dedicated frontend-side schedule rendering cleanup rather than base API plumbing`

## 2026-04-23 - backend live endpoint verification coverage

- task: `Add MockMvc verification coverage for live movie endpoints`
- score_total: `5`
- evaluation_fit: `light review fit because the task locked an existing API contract rather than changing feature behavior`
- orchestration_fit: `single-session fit because all edits stayed inside one backend test slice`
- predicted_topology: `main only`
- actual_topology: `main only`
- spawn_count: `0`
- rework_or_reclassification: `Reclassified from prior endpoint implementation into a test-focused verification task before editing`
- reviewer_findings: `Replaced deprecated MockBean usage with MockitoBean so the new MVC tests do not add a Spring Boot deprecation warning`
- verification_outcome: `Added MockMvc tests for nearby success/validation and schedules success/error paths; gradle test passed using an explicit local Gradle path and a workspace-local GRADLE_USER_HOME`
- next_gate_adjustment: `The next backend slice can use these endpoint tests as a contract harness before tightening DB-accurate repository coverage`

## 2026-04-23 - backend root dotenv loading

- task: `Make backend read the workspace root .env directly`
- score_total: `5`
- evaluation_fit: `light review fit because the change is a bounded startup-config hook with clear behavior`
- orchestration_fit: `single-session fit because bootstrap wiring, parser behavior, and tests stayed in one write lane`
- predicted_topology: `main only`
- actual_topology: `main only`
- spawn_count: `0`
- rework_or_reclassification: `Reclassified from endpoint verification into a startup configuration task before editing`
- reviewer_findings: `Kept OS environment variables ahead of .env values by adding the dotenv property source at low precedence and added a test-only start-dir override instead of mutating user.dir in tests`
- verification_outcome: `Added a root .env EnvironmentPostProcessor, parser tests, precedence tests, and passed gradle test using an explicit local Gradle path and a workspace-local GRADLE_USER_HOME`
- next_gate_adjustment: `The next backend verification step can focus on bootRun against TiDB-backed .env values and then repository-level DB accuracy checks`

## 2026-04-23 - frontend live movies API base alignment

- task: `Align movies.html frontend wiring with the backend API base configuration`
- score_total: `4`
- evaluation_fit: `light review fit because the task is one bounded frontend wiring tweak`
- orchestration_fit: `single-session fit because the change stayed inside one page script contract`
- predicted_topology: `main only`
- actual_topology: `main only`
- spawn_count: `0`
- rework_or_reclassification: `Reclassified from backend dotenv work into a frontend wiring task before editing`
- reviewer_findings: `Removed the page-specific backend API hardcode so the live movies page now honors the shared window-level API base override convention`
- verification_outcome: `Reviewed the updated liveMovies.js wiring and confirmed the page still targets /api/live/nearby and /api/live/movies/{movieKey}/schedules while defaulting to backend port 8080`
- next_gate_adjustment: `The next end-to-end check can focus on browser-side rendering against the running backend instead of more API base cleanup`

## 2026-04-23 - CGV collector bundle ingestion

- task: `Add a CGV collector bundle ingestion script for TiDB`
- score_total: `7`
- evaluation_fit: `full review fit because this introduces a DB write path and collector-to-schema mapping`
- orchestration_fit: `single-session fit because the first slice is CGV-only and tightly couples bundle shape, Gradle wiring, and SQL upserts`
- predicted_topology: `main only`
- actual_topology: `main only`
- spawn_count: `0`
- rework_or_reclassification: `Reclassified from frontend wiring into a CGV-only ingestion task after confirming collectors currently return bundles without DB writes`
- reviewer_findings: `Kept write mode explicit through -Write and left dry-run as the default; preserved raw collector payloads in JSON columns`
- verification_outcome: `gradle test passed; ingestCgvBundle dry-run passed with a sample bundle; scripts/ingest_cgv_to_tidb.ps1 dry-run passed; TiDB accepted CAST(? AS JSON)`
- next_gate_adjustment: `Next step should run a real CGV collection with configured CGV_API_SECRET, inspect counts, then use -Write only after the dry-run counts look right`

## 2026-04-24 - 3-provider collector bundle ingestion

- task: `Add shared CGV/LOTTE/MEGABOX bundle ingestion for the movies.html backend`
- score_total: `8`
- evaluation_fit: `full review fit because the slice widens DB writes across three provider bundle shapes`
- orchestration_fit: `single-session fit because provider normalizers, shared SQL upserts, Gradle wiring, and verification all depended on one frozen contract`
- predicted_topology: `main only`
- actual_topology: `main only`
- spawn_count: `0`
- rework_or_reclassification: `Reclassified from merge prep into a multi-provider ingestion task before editing and replaced the CGV-only command with a shared normalizer-based ingest path`
- reviewer_findings: `Changed dry-run so it no longer requires a DB connection, kept provider raw_json instead of inventing missing fields, and preserved the CGV wrapper script for compatibility`
- verification_outcome: `gradle test passed; gradle bootJar passed; ingestCollectorBundle dry-run passed for CGV, LOTTE_CINEMA, and MEGABOX sample bundles; scripts/ingest_cgv_to_tidb.ps1 dry-run passed against a sample CGV bundle`
- next_gate_adjustment: `Next verification step should run real collector bundles from each provider against TiDB and compare inserted counts with live API query results`

## 2026-04-24 - backend scheduled sync automation

- task: `Implement backend schedulers for daily showtime sync and 30-minute seat snapshots`
- score_total: `9`
- evaluation_fit: `full review fit because the slice adds backend automation, external collector invocation, and new DB write paths for seat snapshots`
- orchestration_fit: `single-session fit because scheduler config, python bridge, and persistence helpers shared one contract and one write lane`
- predicted_topology: `main only`
- actual_topology: `main only`
- spawn_count: `0`
- rework_or_reclassification: `Reclassified from manual ingestion into scheduled automation and split the design into showtime sync and seat snapshot sync services`
- reviewer_findings: `Kept schedulers safe-off by default, reused booking_key_json instead of inventing new provider identifiers, and isolated seat snapshot persistence into its own service/table path`
- verification_outcome: `gradle test passed; gradle bootJar passed; added unit coverage for scheduler orchestration and provider seat-status normalization`
- next_gate_adjustment: `Next step should wire real provider targets into configuration, run the schedulers against TiDB, and inspect inserted seat snapshot counts before widening scope`
