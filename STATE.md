## Current Task
- task: show direct-compare nearby results using the nearest theater per provider to the searched region
- phase: implement
- reason: the user clarified that nearby direct-compare should keep one closest theater for each provider like LOTTE and MEGA, rather than mixing all nearby theaters or collapsing all providers to one theater

## Orchestration Profile
- score_total: 5
- score_breakdown: provider-scoped nearby contract change 3, backend filtering update 1, focused verification 1
- hard_triggers: none
- selected_rules: verification_required
- selected_skills: none
- selection_reason: the behavior change stays inside one nearby response path and one focused test surface, so single-session implementation is still the fastest low-risk path
- execution_topology: single-session
- orchestration_value: low
- evaluation_need: light
- agent_budget: 0
- spawn_decision: no-spawn
- efficiency_basis: the change is a tightly coupled backend response filter with one dominant acceptance rule, so splitting discovery or implementation would add handoff cost without creating an independent verification slice

## Writer Slot
- writer_slot: main
- write_sets: STATE.md, ERROR_LOG.md, backend/src/main/java/kr/daboyeo/backend/service/LiveMovieService.java, backend/src/test/java/kr/daboyeo/backend/service/LiveMovieServiceNearbyRefreshTests.java

- contract_freeze: nearby direct-compare should return showtimes from only the nearest matched theater for each provider in the result set, while keeping the existing refresh behavior and other filters unchanged
- non_goals: changing the homepage region UI again, reducing the collector refresh radius, or redesigning provider-specific collection logic
- task_acceptance:
  - nearby direct-compare responses keep only the schedules belonging to the nearest theater for each provider in the result set
  - existing nearby refresh triggering and warning behavior remain intact
  - verification shows the per-provider nearest-theater filtering behavior with automated coverage
- hard_checks:
  - run focused backend automated tests for nearby live behavior
  - review the nearby response path for regressions in provider coverage and warnings

## Reviewer
- reviewer: main
- llm_review_rubric: confirm the nearby response now anchors to one nearest theater per provider without changing refresh semantics or non-nearby endpoints
- evidence_required: diff plus focused automated verification

## Last Update
- 2026-05-07 KST: updated `LiveMovieService` to keep only the nearest theater per provider in nearby direct-compare results and verified it with `LiveMovieServiceNearbyRefreshTests`
- 2026-05-07 KST: reclassified from nearby collection recovery to a provider-scoped nearby result contract change after the user clarified they want one closest theater per provider such as one LOTTE and one MEGA
- 2026-05-07 KST: reclassified from homepage region UI work to a backend nearby runtime repair after direct-compare verification showed LOTTE and MEGABOX missing and backend logs pinned both failures to `collectors/__init__.py` importing the removed `collectors.cgv` module
- 2026-05-07 KST: reclassified the homepage region task from Kakao-only dynamic dropdowns to a simpler single-address-input flow after the user asked to replace the dropdown menus with automatic region address filling from map search
- 2026-05-07 KST: removed the homepage dropdown's dependency on static all-region data by rewriting `frontend/src/js/pages/script.js` to keep the region controls disabled until Kakao map search injects dynamic options into `dynamicRegions`, while `kakaoMap.js` continues to feed resolved addresses into `window.updateRegionFromMap`
- 2026-05-07 KST: reclassified from hardcoded-region sync repair to a Kakao-only dropdown source refactor after the user requested that homepage region options be populated only from map search results
- 2026-05-07 KST: rewrote `frontend/src/js/pages/script.js` region sync helpers with clean parsing and dong normalization, then reloaded the homepage and verified that map-search input updates the dropdowns to the resolved Seoul/Mapo/Donggyo path
