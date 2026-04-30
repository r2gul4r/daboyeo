## Current Task
- task: clean up temporary backend logs and reorganize the backend sync package so the new nearby/showtime/seat responsibilities are easier to navigate without changing behavior
- phase: implement
- reason: the user asked to stop the backend server and tidy the sync folder, and the current package is crowded after adding nearby refresh plus cleanup classes

## Orchestration Profile
- score_total: 8
- score_breakdown: multi-file package refactor 4, runtime wiring preservation 2, acceptance/verification coupling 1, dirty-worktree caution 1
- hard_triggers: contract_instability
- selected_rules: verification_required
- selected_skills: none
- selection_reason: the requested cleanup now spans many moved classes and import rewrites across runtime plus tests, but the package structure is tightly coupled enough that a single write lane is safer than splitting ownership
- execution_topology: single-session
- orchestration_value: low
- evaluation_need: light
- agent_budget: 0
- spawn_decision: no-spawn
- efficiency_basis: package moves and import rewrites touch overlapping runtime surfaces, so delegation would mostly add merge risk and handoff cost

## Writer Slot
- writer_slot: main
- write_sets: STATE.md, backend/src/main/java/kr/daboyeo/backend/**, backend/src/test/java/kr/daboyeo/backend/**, temporary *.log files in workspace root

## Contract Freeze
- contract_freeze: remove temporary workspace log files, then reorganize backend sync classes into clearer subpackages for nearby, seat, showtime, and shared bridge/provider concerns without changing behavior or configuration semantics
- non_goals: adding SQL migrations, changing collector behavior beyond package/import moves, or altering public API behavior
- task_acceptance:
  - temporary backend log files are removed when not locked by a running process
  - sync package classes are grouped by responsibility and imports still compile
  - nearby/showtime/seat behavior remains unchanged after the package cleanup
  - focused tests still pass after the refactor
- hard_checks:
  - run focused sync-layer and live-service tests after the package refactor
  - verify there are no leftover broken imports or orphan classes

## Reviewer
- reviewer: main
- llm_review_rubric: verify the package split is coherent, imports are updated consistently, and no behavior-affecting logic moved unintentionally
- evidence_required: package-structure proof, focused test output, and an explicit note that the cleanup was behavior-preserving

## Last Update
- 2026-04-30 KST: added nearby refresh config, async executor, theater-map target resolver, TTL/in-flight guarded refresh service, LiveMovieService hook, and focused tests passed without schema changes
- 2026-04-30 KST: runtime verification showed nearby refresh requests fire but LOTTE_CINEMA target matching and MEGABOX global area discovery prevent persistence, so implementation resumed for provider-targeted nearby discovery
- 2026-04-30 KST: switched nearby refresh to provider-targeted discovery, added MEGABOX region_code fallback for area lookup, focused tests passed, and end-to-end verification confirmed LOTTE_CINEMA plus MEGABOX nearby theaters updated last_collected_at without schema changes
- 2026-04-30 KST: backend server was stopped for cleanup, and the active task shifted to removing temporary logs plus reorganizing the crowded sync package structure without changing runtime behavior
