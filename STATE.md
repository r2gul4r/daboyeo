# STATE

## Current Task

- task: `Run CGV signed API live probe`
- phase: `verified`
- scope: `Verify newly configured local CGV_API_SECRET without printing it, run bounded CGV signed API collection, and save raw probe outputs under ignored .local storage for schema review`
- verification_target: `CGV signed API returned current movies, attributes, regions, sites, dates, schedules, and seat sample data without printing CGV_API_SECRET`

## Orchestration Profile

- score_total: `6`
- score_breakdown: `external CGV API dependency=1, local secret presence verification=1, auth/signature boundary=1, live data fidelity risk=1, output capture for schema review=1, no secret leakage=1`
- hard_triggers: `external_source_dependency, secret_handling, auth_boundary`
- selected_rules: `single-session, preserve user changes, no browser checks, do not print secrets, no auth bypass, write raw outputs only under ignored .local path, append ERROR_LOG.md on material failures`
- selected_skills: `n/a`
- execution_topology: `single-session`
- delegation_plan: `no delegation; user did not request subagents and this is a bounded script execution/probe task`
- agent_budget: `0 subagents`
- shared_assets_owner: `main`
- selection_reason: `score_total 6 with secret/auth boundary and live CGV dependency; user configured the secret and asked to continue, but no subagents are requested and the probe is bounded`

## Writer Slot

- owner: `main`
- write_set: `STATE.md, ERROR_LOG.md if needed, .local/api-responses/**`
- write_sets:
  - `main`: `STATE.md, ERROR_LOG.md if needed, .local/api-responses/**`
  - `worker`: `n/a`
  - `reviewer`: `main self-review`
- note: `writer_slot`, `contract_freeze`, and `write_sets` stay explicit while this scaffold is active.`
- concurrent_note: `Keep one shared task board by default. If same-workspace concurrent threads are intentionally enabled, root STATE.md becomes the registry and per-thread execution state moves into states/STATE.<thread_id>.md.`

## Contract Freeze

- contract_freeze: `Use only the user-configured legitimate CGV_API_SECRET, do not print or log the value, do not bypass CGV signature/authentication, and keep this step to bounded raw data capture rather than DB mutation.`
- note: `If CGV returns 403, log it as key rotation/revalidation risk and stop rather than attempting circumvention.`

## Seed

- status: `n/a`
- path: `n/a`
- revision: `n/a`
- note: `Tiny follow-up; no new seed required.`

## Reviewer

- reviewer: `main self-review`
- reviewer_target: `CGV signed API live probe and secret handling`
- reviewer_focus: `no auth bypass, no secret leakage, bounded output capture, explicit 403/key-rotation diagnostics`

## Last Update

- timestamp: `2026-04-14 15:29:00 +09:00`
- note: `CGV signed API live probe succeeded and saved bounded outputs under .local/api-responses/fresh-cgv-20260414-152645.`

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
