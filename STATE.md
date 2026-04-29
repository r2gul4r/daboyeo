## Current Task
- task: align the current branch `db/` directory with `origin/lsh`
- phase: implement
- reason: the user explicitly requested that the database directory be brought over from `origin/lsh` so the local structure matches the team DB contract

## Orchestration Profile
- score_total: 3
- score_breakdown: branch-to-branch db sync 2, local verification 1
- hard_triggers: none
- selected_rules: verification_required
- selected_skills: none
- selection_reason: the user asked for a bounded `db/` sync from another branch, so one local write lane can update that directory directly and then verify the resulting structure
- execution_topology: single-session
- orchestration_value: low
- evaluation_need: light
- agent_budget: 0
- spawn_decision: no-spawn
- efficiency_basis: the work crosses a few files but stays tightly coupled around one ingest pipeline and one browser outcome, so handoff cost is higher than keeping one write lane while the contract is still moving

## Writer Slot
- writer_slot: main
- write_sets: STATE.md, db/**

## Contract Freeze
- contract_freeze: replace the current branch `db/` directory contents with the versions from `origin/lsh`, then verify the expected schema contract and migration files exist locally.
- non_goals: applying migrations to a live DB, changing non-`db/` files, or editing the imported DB files beyond the requested branch sync
- task_acceptance:
  - local `db/` matches `origin/lsh` for tracked files
  - `db/SCHEMA_CONTRACT.md` and `db/migrations/005_collection_contract_extensions.sql` exist after the sync
- hard_checks:
  - verify the synced `db/` file list locally

## Reviewer
- reviewer: main
- llm_review_rubric: the resulting `db/` tree should reflect the tracked files from `origin/lsh` without spilling changes into non-`db/` paths
- evidence_required: local file list and git status proof for `db/`

## Last Update
- 2026-04-29 KST: task reclassified to sync the `db/` directory from `origin/lsh`
