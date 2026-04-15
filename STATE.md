# STATE

## Current Task

- task: `Refactor frontend shared HTML into include components`
- phase: `verified`
- scope: `Split duplicated header/footer/top button markup into reusable frontend components and wire every page to load them through a shared include.js bootstrap`
- verification_target: `All frontend HTML entry pages use #header, #footer, and #topButton placeholders with shared include.js and no duplicated shared markup remains inline`
## Orchestration Profile

- score_total: `4`
- score_breakdown: `multi-page frontend refactor=1, shared UI bootstrap timing=1, path normalization across root/sub pages=1, low-risk static verification only=1`
- hard_triggers: `html_rendering_boundary`
- selected_rules: `single-session, preserve user changes, no framework introduction, shared component extraction, static verification first`
- selected_skills: `n/a`
- execution_topology: `single-session`
- delegation_plan: `no delegation; user did not request subagents and the work stays inside a bounded frontend refactor`
- agent_budget: `0 subagents`
- shared_assets_owner: `main`
- selection_reason: `score_total 4 with shared HTML rendering changes across multiple pages, but still bounded and local to frontend files`
## Writer Slot

- owner: `main`
- write_set: `STATE.md, frontend/components/**, frontend/src/js/include.js, frontend/src/js/pages/common.js, frontend/index.html, frontend/src/basic/*.html`
- write_sets:
  - `main`: `STATE.md, frontend/components/**, frontend/src/js/include.js, frontend/src/js/pages/common.js, frontend/index.html, frontend/src/basic/*.html`
  - `worker`: `n/a`
  - `reviewer`: `main self-review`
- note: `writer_slot`, `contract_freeze`, and `write_sets` stay explicit while this scaffold is active.`
- concurrent_note: `Keep one shared task board by default. If same-workspace concurrent threads are intentionally enabled, root STATE.md becomes the registry and per-thread execution state moves into states/STATE.<thread_id>.md.`
## Contract Freeze

- contract_freeze: `Keep the frontend in pure HTML, CSS, and vanilla JS; load shared UI through include.js on DOMContentLoaded; do not introduce frameworks or change user/admin boundaries.`
- note: `Shared UI markup lives in frontend/components and page-specific behavior must wait for injected DOM when it depends on header/footer elements.`
## Seed

- status: `n/a`
- path: `n/a`
- revision: `n/a`
- note: `Tiny follow-up; no new seed required.`

## Reviewer

- reviewer: `main self-review`
- reviewer_target: `frontend shared include component refactor`
- reviewer_focus: `shared markup extraction, include timing, path consistency across root/sub pages, no framework introduction`
## Last Update

- timestamp: `2026-04-15 14:30:00 +09:00`
- note: `Extracted shared frontend header/footer/top button into reusable component files, added include.js, and converted all current HTML entry pages to placeholder-based loading.`
## Retrospective

- task: `Refactor frontend shared HTML into include components`
- score_total: `4`
- selected_profile: `single-session`
- actual_topology: `main-only`
- verification_outcome: `passed git status, WORKSPACE_CONTEXT read, required section checks, confirmed zero inline shared header/footer/top button blocks remain in frontend HTML pages, and confirmed placeholder/include markers across 8 pages`
- collisions_or_reclassifications: `priceComparison.html needed a page-specific header class and delayed initialization after injected header load`
- next_rule_change: `For future shared frontend refactors, prefer injected placeholders plus a components-loaded event so page scripts can bind after async HTML includes`
## Retrospective

- task: `Run CGV signed API live probe`
- score_total: `6`
- selected_profile: `single-session`
- actual_topology: `main-only`
- verification_outcome: `CGV signed API returned movies=54, attributes=16, regions=9, sites=177, dates=6, schedules=2, seats=123 for sample CGV 강남 / 왕과 사는 남자 / 2026-04-14`
- collisions_or_reclassifications: `reclassified from secret-loading repair to live external data probe after user configured .env`
- next_rule_change: `When summarizing generated UTF-8 JSON in Windows PowerShell, always use Get-Content -Encoding UTF8 to avoid mojibake and false JSON parse failures`


## Retrospective

- task: `Restore legitimate CGV API probe path`
- score_total: `6`
- selected_profile: `single-session`
- actual_topology: `main-only`
- verification_outcome: `passed py_compile; cgv_collector_demo.py and cgv_api_probe.py fail cleanly with one-line CGV_API_SECRET missing message because local .env has no CGV_API_SECRET`
- collisions_or_reclassifications: `reclassified from fresh extraction after CGV secret/auth boundary became the blocker`
- next_rule_change: `For CGV live collection, require CGV_API_SECRET presence check before treating failed API probes as collector bugs`


## Retrospective

- task: `Run fresh Lotte and CGV data extraction scripts`
- score_total: `5`
- selected_profile: `single-session`
- actual_topology: `main-only`
- verification_outcome: `Lotte extraction succeeded, CGV failed as expected due missing CGV_API_SECRET, ERROR_LOG appended`
- collisions_or_reclassifications: `none`
- next_rule_change: `Before CGV live probes, verify CGV_API_SECRET presence and load strategy because CGV client reads process env directly`


## Retrospective

- task: `Commit DB schema and ingest foundation`
- score_total: `4`
- selected_profile: `single-session`
- actual_topology: `main-only`
- verification_outcome: `passed compileall, verify_tidb_ingest, secret-pattern scan, git diff check, WORKSPACE_CONTEXT read, and required section Select-String checks`
- collisions_or_reclassifications: `none`
- next_rule_change: `Commit DB foundation changes before starting API/search work to avoid scope mixing`


## Retrospective

- task: `Add DB schema naming contract document`
- score_total: `3`
- selected_profile: `single-session`
- actual_topology: `main-only`
- verification_outcome: `passed git status, WORKSPACE_CONTEXT read, and required section Select-String checks`
- collisions_or_reclassifications: `none`
- next_rule_change: `Read db/SCHEMA_CONTRACT.md before future DB/API/ingest naming changes`


## Retrospective

- task: `Apply TiDB 003 migration and implement Lotte/Megabox ingest`
- score_total: `7`
- selected_profile: `single-session`
- actual_topology: `main-only`
- verification_outcome: `passed compileall, dry-run, 003 schema inspect, repeated bounded ingest, duplicate showtime check, and repository verification commands`
- collisions_or_reclassifications: `write set expanded for scripts/db and db/migrations after migration runner and TiDB ALTER ordering failures`
- next_rule_change: `Keep migration runner comment stripping and prefer one-column ALTER statements for TiDB column-order-sensitive migrations`


