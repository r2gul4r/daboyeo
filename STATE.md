## Current Task
- task: limit nearby Megabox refresh persistence to theaters inside the local nearby target set so broad area-code collection does not upsert unrelated theaters
- phase: verify
- reason: the user asked to modify Megabox collection so region searches only collect/output nearby movie-chain data instead of persisting broad area-code bundles

## Orchestration Profile
- score_total: 6
- score_breakdown: provider-specific collection scope 2, persistence filtering 2, nearby-refresh radius cap 1, focused test update 1
- hard_triggers: implementation_depends_on_discovery_result
- selected_rules: verification_required
- selected_skills: none
- selection_reason: Megabox still requires area-code API discovery, but refresh target resolution can be capped at 3km and persistence can be scoped to those resolved nearby theater ids
- execution_topology: single-session
- orchestration_value: low
- evaluation_need: light
- agent_budget: 0
- spawn_decision: no-spawn
- efficiency_basis: the change is a tightly coupled filter in one refresh path plus focused tests, so delegation would add handoff without independent write sets

## Writer Slot
- writer_slot: main
- write_sets: STATE.md, backend/src/main/java/kr/daboyeo/backend/config/**, backend/src/main/java/kr/daboyeo/backend/sync/nearby/**, backend/src/main/resources/application.yml, backend/src/test/java/kr/daboyeo/backend/sync/nearby/**

## Contract Freeze
- contract_freeze: nearby refresh target resolution is capped to a configurable 3km radius by default; Megabox may still discover schedules by areaCode, but each collected bundle must be filtered before persistence so only schedules, theaters, and screens for stale nearby Megabox theater ids are stored
- non_goals: changing database schema, rewriting Python collectors, changing frontend filters, increasing wait time, or changing Lotte collection behavior beyond the shared nearby refresh target radius cap
- task_acceptance:
  - Megabox nearby refresh filters collected bundle data to the stale nearby theater ids for that area before persisting
  - nearby refresh target resolution uses a 3km default cap even when the search response radius is larger
  - unrelated Megabox theaters from broad area-code responses are not upserted during nearby refresh
  - filtered bundles keep the movie rows needed by the remaining schedules
  - focused tests cover Megabox filtering and preserve stale/fresh refresh behavior
- hard_checks:
  - run focused NearbyShowtimeRefreshServiceTests
  - run relevant backend test subset if the focused check exposes shared behavior risk

## Reviewer
- reviewer: main
- llm_review_rubric: verify Megabox filtering is applied only to nearby refresh persistence, does not mutate Lotte, and does not drop movies referenced by retained schedules
- evidence_required: focused test output showing the Megabox bundle passed to persistence excludes unrelated theater schedules

## Last Update
- 2026-05-04 KST: verified Megabox output after restart; API returned MEGA rows for Gangnam search and frontend rendered MEGA movie cards plus MEGA-only schedule modal without console errors
- 2026-05-04 KST: rebuilt/restarted backend on port 8080 and started frontend static server on port 5500; backend health and frontend movies page responded successfully
- 2026-05-04 KST: implemented 3km default nearby refresh radius cap and Megabox bundle filtering before persistence; focused nearby refresh/resolver tests passed
- 2026-05-04 KST: reclassified task to Megabox nearby refresh scope filtering with single-session implementation and focused verification
- 2026-05-04 KST: active task reclassified to nearby live search contract changes so empty first-lookups wait briefly for refresh and stop returning demo fallback
- 2026-05-04 KST: added bounded nearby refresh waiting plus pending response semantics, focused backend tests passed, and runtime nearby response now returns real data without demo fallback
