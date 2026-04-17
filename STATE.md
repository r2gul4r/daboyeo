# STATE

## Current Task

- task: `Filter recommendation candidates by usable start time`
- phase: `verified`
- scope: `Keep collection broad, but make the recommendation API exclude already-started or too-soon showtimes using a configurable start buffer, split no-candidate reasons, and add time-aware scoring where appropriate`
- verification_target: `unit tests for candidate filtering/status behavior plus backend Gradle tests`
- previous_task_note: `Real Lotte/Megabox ingest already produced actual showtimes. The new goal is to filter at recommendation usage time, not during collection.`

## Orchestration Profile

- score_total: `6`
- score_breakdown: `2 recommendation contract change, 1 DB query time filter, 1 config surface, 1 no-candidate status behavior, 1 tests`
- hard_triggers: `recommendation contract extension, database query behavior, user-facing API status/message`
- selected_rules: `spec-first lightweight, security rules for DB/API behavior, preserve user changes, no collector narrowing`
- selected_skills: `none`
- execution_topology: `single-session`
- orchestration_value: `low`
- agent_budget: `0`
- spawn_decision: `no spawn; the change is a tightly coupled backend query/service/test slice`
- efficiency_basis: `handoff cost is higher than gain because repository query, service status, config binding, and tests must evolve together`
- selection_reason: `user approved the plan to collect broadly and filter in recommendation usage`

## Evaluation Plan

- evaluation_need: `full`
- project_invariants:
  - `Collection scripts should keep storing broad real showtime data.`
  - `Do not delete or discard past showtimes from DB.`
  - `Recommendation candidates should be filtered by actual starts_at, not only show_date.`
  - `Recommendation buffer must be configurable.`
  - `Do not expose .env secrets in output.`
- task_acceptance:
  - `Recommendation config has min-start-buffer-minutes with a sane default.`
  - `Upcoming candidate query excludes starts_at before now + buffer.`
  - `Recommendation service distinguishes no stored showtimes from no usable future candidates.`
  - `Time-of-day scoring can adjust recommendations without changing collection.`
  - `Tests cover past, within-buffer, and after-buffer showtimes.`
- non_goals:
  - `No change to periodic collection breadth.`
  - `No fake/demo data insertion.`
  - `No frontend redesign.`
  - `No deployment or external smoke URL check.`
- hard_checks:
  - `Inspect ShowtimeRecommendationRepository and RecommendationService.`
  - `Add/update recommendation package tests.`
  - `gradle test --tests kr.daboyeo.backend.service.recommendation.*`
  - `gradle test`
  - `git diff --check`
  - `git status --short`
- llm_review_rubric:
  - `Filtering happens in usage query, not collection.`
  - `No-candidate status explains the data condition clearly.`
  - `Config default is useful for local demo and can be overridden.`
  - `Tests prove already-started showtimes are not recommended.`
- evidence_required:
  - `Record changed files and verification outputs.`

## Verification Results

- unit_tests:
  - `gradle test --tests kr.daboyeo.backend.service.recommendation.* --tests kr.daboyeo.backend.repository.recommendation.*`: `passed`
  - `gradle test`: `passed`
  - `gradle bootJar`: `passed`
- local_api:
  - `DABOYEO_RECOMMEND_MIN_START_BUFFER_MINUTES=20`
  - `cutoff_local`: `2026-04-17T16:54:48`
  - `POST /api/recommendations fast`: `status=ok, model=gemma-4-e2b-it, recommendation_count=3`
  - `returned_starts_at`: `2026-04-17T18:10:00, 2026-04-17T18:20:00, 2026-04-17T18:40:00`
  - `recommendation_profiles_after_cleanup`: `0`
  - `recommendation_runs_after_cleanup`: `0`
- repository_checks:
  - `findUpcomingCandidates now uses starts_at >= cutoff`
  - `stored showtime count is only used to distinguish no_showtime_data from no_usable_showtimes`

## Retrospective

- task: `Filter recommendation candidates by usable start time`
- score_total: `6`
- evaluation_fit: `full fit; this changed API behavior and DB query semantics`
- orchestration_fit: `single-session fit; query, config, service statuses, and tests were tightly coupled`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `new task replacing verified real-ingest task`
- reviewer_findings: `collection remains broad; filtering moved to recommendation usage query`
- verification_outcome: `tests passed and live fast recommendation returned only showtimes after the 20-minute buffer`
- next_gate_adjustment: `next scheduling work should treat collection cadence separately from recommendation visibility`

## Writer Slot

- writer_slot: `main`
- write_set: `STATE.md, backend/src/main/java/** recommendation/config files, backend/src/test/java/** recommendation tests, backend/src/main/resources/application.yml, .env.example, ERROR_LOG.md if needed`
- write_sets:
  - `main`: `backend recommendation filtering/config/status/tests`
- shared_assets_owner: `main`
- note: `One shared task board is active; no concurrent registry mode.`
- concurrent_note: `No parallel writer is active.`

## Contract Freeze

- contract_freeze: `Do not narrow or delete collected showtime data. Add a recommendation-only usable-start filter based on starts_at >= now + min-start-buffer-minutes, expose the buffer in config/env defaults, distinguish empty database from filtered-out candidate state, and keep fallback behavior when LM fails.`
- note: `This task intentionally touches recommendation usage, not collectors.`
- contract_source: `user request`
- contract_revision: `2026-04-17-recommendation-time-filter`
- verification_target: `recommendation tests and full backend tests`

## Reviewer

- reviewer: `main self-review`
- reviewer_target: `recommendation candidate filtering and no-candidate behavior`
- reviewer_focus: `real data preservation, time filter correctness, config override, regression coverage`

## Last Update

- timestamp: `2026-04-17 16:43:54 +09:00`
- note: `Verified recommendation-only time filtering with tests, rebuilt the jar, and restarted the local backend on the updated code.`
