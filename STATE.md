# STATE

## Current Task

- task: `Implement local Gemma fast/precise AI recommendation v1`
- phase: `verify`
- scope: `remove frontend recommendation UI/client changes; keep backend recommendation APIs, local Ollama adapter, anonymous recommendation storage, DB migration, env examples`
- verification_target: `backend tests when Java/Gradle are available, repository verification commands, code review`

## Orchestration Profile

- score_total: `7`
- score_breakdown: `2 local HTTP LLM integration through Ollama, 1 anonymous server-side storage, 1 DB/API contract extension, 1 cleanup after scope correction`
- hard_triggers: `user input, local HTTP model call, anonymous recommendation data storage, recommendation contract extension`
- selected_rules: `spec-first, security rules for user input/external request/storage, preserve user changes, no browser checks unless requested`
- selected_skills: `none`
- execution_topology: `mixed`
- orchestration_value: `medium`
- agent_budget: `2`
- spawn_decision: `use one read-only explorer for E2B/Ollama discovery while main implements; reserve one reviewer after integration`
- efficiency_basis: `E2B/Ollama discovery is separable and read-only, implementation write ownership stays with main, reviewer can independently check API/UI/security after patch, rework risk is lower with late review`
- selection_reason: `score_total 7 from LLM, storage, DB/API, and frontend integration triggers; user granted standing subagent authorization during implementation, so only separable discovery/review work is delegated`

## Evaluation Plan

- evaluation_need: `full`
- project_invariants:
  - `CGV, Lotte Cinema, Megabox showtime data remains the core source for current movie recommendation candidates.`
  - `Collectors preserve provider-specific raw data characteristics.`
  - `Comparison/recommendation uses minimum common fields and movie_tags without renaming stable provider keys.`
  - `Do not hardcode secrets, tokens, API keys, or private model credentials.`
  - `Validate user input at API boundaries and escape rendered frontend content.`
  - `Default verification is local commands plus code review; no deployed URL smoke check unless requested.`
  - `Do not touch dist/**, generated/**, vendor/**, or .git/**.`
- task_acceptance:
  - `Expose anonymous recommendation session, poster seed, recommendation, feedback, and reset APIs.`
  - `Support fast and precise modes mapped to local model providers from environment.`
  - `Use code scoring first, local Gemma JSON reordering/explanation second, deterministic fallback on AI failure.`
  - `Do not include frontend recommendation UI in this task.`
  - `Store anonymous profile, recommendation runs, and feedback server-side, with reset support.`
- non_goals:
  - `No login implementation.`
  - `No Oracle Cloud deployment or cloud-to-local AI bridge.`
  - `No external movie metadata API dependency for v1 poster seed.`
  - `No public Ollama exposure.`
- hard_checks:
  - `git status --short`
  - `Get-Content -Raw WORKSPACE_CONTEXT.toml`
  - `Select-String -Path WORKSPACE_CONTEXT.toml -Pattern '^\[workspace\]','^\[architecture\]','^\[editing_rules\]','^\[verification\]'`
  - `gradle test if Java 21 and Gradle are available on PATH`
- llm_review_rubric:
  - `No secrets or model credentials are hardcoded.`
  - `User input is bounded and validated before storage/model prompt construction.`
  - `AI response parsing failure cannot break recommendation output.`
  - `Child audience hard filter is preserved.`
  - `No frontend implementation remains in this task.`
- evidence_required:
  - `Record verification command results, unavailable Java/Gradle gaps, diff review notes, and any material failures.`
- note: `Hard checks outrank LLM review.`

## Writer Slot

- writer_slot: `main`
- write_set: `frontend/src/basic/daboyeoAi.html, frontend/src/js/api/client.js, frontend/src/css/daboyeoAi.css, frontend/src/js/pages/daboyeoAi.js, STATE.md`
- write_sets:
  - `main`: `frontend recommendation cleanup files and STATE.md`
  - `explorer`: `read-only local Ollama/E2B model discovery`
  - `reviewer`: `read-only final API/UI/security review`
- shared_assets_owner: `main`
- note: `One shared task board is active; no concurrent registry mode.`
- concurrent_note: `No parallel writer is active.`

## Contract Freeze

- contract_freeze: `POST /api/recommendation/sessions, DELETE /api/recommendation/sessions/{anonymousId}, GET /api/recommendation/poster-seed, POST /api/recommendations, POST /api/recommendations/{runId}/feedback; fast and precise modes both use local Ollama provider with env-selected model names; frontend implementation is out of scope for this task`
- note: `Implement the plan exactly as pinned by the user unless a verification blocker requires reclassification.`
- contract_source: `user-provided implementation plan`
- contract_revision: `2026-04-15-local-gemma-recommendation-v1`
- verification_target: `backend tests when available plus repository verification commands and self-review`

## Reviewer

- reviewer: `reserved subagent reviewer plus main self-review`
- reviewer_target: `recommendation API contracts, LLM fallback behavior, input validation, anonymous data reset`
- reviewer_focus: `security, verification gaps, data contract preservation, recommendation fallback correctness`

## Last Update

- timestamp: `2026-04-15 18:03:00 +09:00`
- note: `Frontend changes removed; backend recommendation work and database/model configuration remain.`
