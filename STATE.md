# STATE

## Current Task

- task: `Codex-scored reserve recommendation pool`
- phase: `verified`
- scope: `Freeze and implement the current conversation goal: keep DB/time/filter selection on the server, pass a wider taste-aware candidate pool to Codex/GPT, let Codex/GPT assign per-candidate recommendation scores, and let the server validate those scores while filling missing result slots with the nearest reserve candidates.`
- verification_target: `When direct selected-genre current-showing movies are fewer than 3, final recommendations should include the best scored reserve candidates instead of returning only 1-2 items. Codex/GPT prompts and schemas must ask for a bounded score, and the server must clamp no-direct-taste reserve candidates so they cannot look like perfect matches.`
- previous_task_note: `The previous repair removed weak fillers whenever direct candidates existed, which protected trust but made the UI show too few recommendations when the DB had only 1-2 direct matches.`
- runtime_dependency_note: `No DB write or crawl is needed; this is recommendation ranking and AI handoff behavior.`
- spring_runtime_note: `Run focused recommendation service/client tests and bootJar; restart localhost:5500 runtime after implementation verification if changed code compiles.`
- current_blocker: `none`

## Next Task

- task: `Add separate anime poster pool`
- status: `queued`
- scope: `애니 장르 선택 시 보여줄 포스터 후보를 일반 포스터 풀과 별도로 더 보강한다. 추천 설문 장르 필터링과 포스터 seed 데이터 품질을 같이 확인한다.`
- non_goal: `이번 커밋에서는 구현하지 않고 다음 작업 메모로만 저장한다.`

## Orchestration Profile

- score_total: `8`
- score_breakdown: `3 user-facing recommendation trust regression, 2 AI scoring/schema handoff, 1 wider current-showing candidate pool, 1 server-side score validation, 1 focused service/client tests`
- hard_triggers: `user-facing recommendation correctness; AI handoff contract change; result filling policy changes from strict taste gate to taste-first reserve fill`
- selected_rules: `use Selfdex read-only planning discipline; update STATE before implementation edits; keep search-filter and current/future showtime filtering server-side; pass wider taste-aware distinct candidates to Codex/GPT; let Codex/GPT return a bounded score field; clamp reserve/no-direct-taste scores server-side; verify with focused service/client tests, bootJar, runtime smoke, and diff checks`
- selected_skills: `selfdex`
- execution_topology: `single-session`
- orchestration_value: `low`
- agent_budget: `0`
- spawn_decision: `no spawn; Selfdex was used for read-only planning, but implementation is one coupled RecommendationService/LocalModelRecommendationClient contract and disjoint worker ownership is weak`
- efficiency_basis: `Candidate ordering, AI schema parsing, score validation, and final item mapping must stay coherent in one lane; delegation would add handoff and rework risk.`
- selection_reason: `User changed the goal from strict exclusion to showing nearest reserves and asked whether Codex should score candidates from the start.`

## Evaluation Plan

- evaluation_need: `full`
- project_invariants:
  - `Do not print, commit, or document real DB passwords, OAuth tokens, cookies, API keys, tunnel tokens, or OAuth auth paths.`
  - `Do not expose secrets, private Kakao admin keys, Codex auth, bridge token, local model URL, or local filesystem paths to browser responses.`
  - `Do not overwrite unrelated frontend/backend user edits or current R2 poster assets.`
  - `Preserve existing recommendation flow, Codex bridge runtime, and poster-seed behavior.`
  - `Preserve same-origin Spring-served frontend behavior on localhost:5500.`
  - `Preserve /api/showtimes/refresh as a manual server API unless removal becomes necessary, but do not call it from browser entry flows.`
- task_acceptance:
  - `Explicit preferredGenres remain separate from poster-derived likedGenres in TagProfile.`
  - `Exact DB/search filters and current/future showtime requirements remain server-owned.`
  - `AI candidate pools are taste-aware and distinct by movie, but fill with reserve candidates when direct/poster matches are fewer than the provider limit.`
  - `Codex/GPT response schema includes a bounded numeric score field and prompt tells the model to score from supplied evidence only.`
  - `Server clamps model scores so no-direct selected-genre reserve candidates cannot exceed the existing no-direct cap.`
  - `Fallback mode also fills final result slots with reserve candidates when fewer than 3 direct candidates exist.`
- non_goals:
  - `No schema migration or collector refactor unless the existing ingest path is blocked.`
  - `No browser-triggered crawl request.`
  - `No unbounded all-data crawl or long blocking browser request.`
  - `No CGV signed API work in this task.`
  - `No frontend redesign or survey contract change unless the existing response contract requires it.`
  - `No deploy-domain setup, commit, push, or cleanup of unrelated dirty files.`
- hard_checks:
  - `Update STATE before backend/frontend implementation edits.`
  - `Do not expose TiDB secrets or process stderr details to browser responses.`
  - `Run focused RecommendationServiceCandidateFilterTests, LocalModelRecommendationClientTests, and RecommendationScorerTests if score caps are touched.`
  - `Run bootJar if backend code changes compile.`
  - `Run git diff --check for touched files.`
- llm_review_rubric:
  - `Do not let Codex/GPT score no-direct reserve candidates as perfect taste fits.`
  - `Do not return fewer than 3 final items merely because direct taste matches are sparse when scored reserves exist.`
- evidence_required:
  - `Taste-aware reserve-fill source evidence`
  - `Codex/GPT score schema and parsing evidence`
  - `Focused service/client test evidence`
  - `Runtime smoke evidence if restarted`

## Writer Slot

- writer_slot: `main`
- write_sets: `STATE.md; ERROR_LOG.md if material failures occur; backend/src/main/java/kr/daboyeo/backend/domain/recommendation/RecommendationModels.java; backend/src/main/java/kr/daboyeo/backend/config/RecommendationProperties.java; backend/src/main/java/kr/daboyeo/backend/service/recommendation/RecommendationService.java; backend/src/main/java/kr/daboyeo/backend/service/recommendation/LocalModelRecommendationClient.java; backend/src/main/resources/application.yml; backend/src/test/java/kr/daboyeo/backend/service/recommendation/RecommendationServiceCandidateFilterTests.java; backend/src/test/java/kr/daboyeo/backend/service/recommendation/LocalModelRecommendationClientTests.java; backend/src/test/java/kr/daboyeo/backend/service/recommendation/RecommendationScorerTests.java if scorer caps change`

## Contract Freeze

- status: `frozen for Codex-scored reserve fill`
- source_basis: `User accepted that strict taste gating makes too few results and asked to make Codex handle scoring from the candidate stage while still showing nearest reserve candidates.`
- output_code: `RecommendationService should build a distinct taste-aware candidate pool ordered direct preferredGenre matches, poster/liked genre matches, then scored reserves. LocalModelRecommendationClient should ask Codex/GPT for numeric score s plus analysis fields. RecommendationService should use validated model scores for picked items and clamp no-direct-taste reserves. Search-filter relaxation and fallback scoring remain intact.`
- output_tests: `Focused RecommendationServiceCandidateFilterTests and LocalModelRecommendationClientTests, RecommendationScorerTests if caps change, bootJar if feasible, runtime local/Codex smoke, git diff --check.`
- output_docs: `STATE verification note; ERROR_LOG.md if material verification/runtime failures occur.`
- write_sets: `STATE.md; ERROR_LOG.md if needed; RecommendationModels.java; RecommendationProperties.java; RecommendationService.java; LocalModelRecommendationClient.java; application.yml; RecommendationServiceCandidateFilterTests.java; LocalModelRecommendationClientTests.java; RecommendationScorerTests.java if needed`

## Reviewer

- review_required: `self-review`
- reviewer_focus: `Taste-first reserve fill does not reintroduce perfect-score weak fillers, Codex/GPT score field is parsed and bounded, local-model compatibility remains intact, existing child/avoid/time/seat behavior stays intact`

## Last Update

- timestamp: `2026-05-04 17:32:50 +09:00`
- note: `Verified Codex-scored reserve recommendation pool. Selfdex read-only planner ran but suggested an unrelated collector refactor, so the user-fixed /goal overrode it. RecommendationService now builds a taste-aware distinct pool that keeps direct selected-genre matches first and fills shortfalls with scored reserve candidates. GPT/Codex prompts and schema now require numeric score s, AiPick stores the model score, and RecommendationService uses the validated model score while capping no-direct taste reserves at 74. Codex defaults were widened to 12 fast / 20 precise candidates with larger token budgets. Focused RecommendationServiceCandidateFilterTests, LocalModelRecommendationClientTests, and RecommendationScorerTests passed outside sandbox after the known native-platform.dll issue and one duplicate-helper compile repair; bootJar passed. Runtime is Spring PID 15568 and bridge PID 14580 on localhost:5500 with Codex ready; action/SF local fallback returned 3 items Project Hail Mary 100, Prada 74, Mario 74, and Codex returned 3 items Project Hail Mary 94, Prada 72, Kiki 68.`

- timestamp: `2026-05-04 17:40:00 +09:00`
- note: `Queued next task only: add a separate, richer anime poster pool for the genre-guided recommendation flow. No implementation was started for that task before commit/push.`

- timestamp: `2026-05-04 15:14:00 +09:00`
- note: `Verified GPT/Codex analysis taste-match fix. LocalModelRecommendationClient now keeps tasteMatch candidate-specific and tells GPT/Codex not to claim poster-taste match when tasteMatch is empty. Focused LocalModelRecommendationClientTests passed outside sandbox after the known native-platform.dll failure; bootJar passed; Spring restarted on localhost:5500 as PID 8388 with startup sync disabled only for local restart, and provider health reports Codex ready with Python bridge PID 22172.`

- timestamp: `2026-05-04 15:22:09 +09:00`
- note: `Reclassified into genre-guided recommendation flow work. score_total 8; full evaluation; single-session/no-spawn. Frozen contract: add genre selection before posters, filter/prioritize poster seeds by selected genres, allow three liked posters minimum with five max, send preferredGenres to backend profile scoring, and improve GPT/Codex analysis prompt while keeping candidate tasteMatch honest.`

- timestamp: `2026-05-04 15:33:00 +09:00`
- note: `Runtime smoke with preferredGenres action/SF returned status ok/model codex/count 3 but surfaced a live music title first, showing selected genre weights were working only when candidate tags existed. Scope expanded within the same task to strengthen current-release title-derived genre tags and add RecommendationScorerTests before final verification.`

- timestamp: `2026-05-04 15:37:53 +09:00`
- note: `Verified genre-guided recommendation flow. AI page now has a genre step before posters, filtered/prioritized poster choices, and a three-poster minimum. Backend survey/profile now accepts preferredGenres, GPT/Codex prompt asks for richer concrete analysis, current-release title inference tags Project Hail Mary and Demon Slayer-style titles, and runtime codex smoke returned Project Hail Mary first for action/SF preferredGenres. Spring is running on localhost:5500 as PID 14008 and AI bridge as PID 11528.`

- timestamp: `2026-05-04 15:48:36 +09:00`
- note: `Reclassified into a scoring calibration repair. score_total 7; single-session/no-spawn. Root issue: generic audience, mood, seat, price, and short-runtime bonuses can saturate a no-direct-liked-genre candidate to 100, making Devil Wears Prada-style candidates look like perfect matches for unrelated selected/poster genre profiles.`

- timestamp: `2026-05-04 15:51:58 +09:00`
- note: `Runtime smoke reproduced Prada at 100 after the first cap because explicit preferredGenres and poster-derived likedGenres share one scoring bucket. Reclassified to score_total 8 and expanded write set to separate explicit preferredGenres from secondary poster-derived genres before applying score caps.`

- timestamp: `2026-05-04 15:56:30 +09:00`
- note: `Runtime smoke after preferredGenre separation reduced Prada from 100 to 84, but 84 still reads like a strong recommendation. Scope expanded inside the frozen score calibration repair to lower the no-anchor cap and make GPT/Codex tasteMatch plus fallback analysis use preferredGenres as the direct taste anchor when present.`

- timestamp: `2026-05-04 16:03:23 +09:00`
- note: `Verified genre-anchor scoring repair. No-direct selected-genre match candidates are capped at 74, GPT/Codex tasteMatch now uses preferredGenres when present, focused profile/scorer/prompt tests and bootJar passed outside sandbox after the known native-platform.dll failure, Spring is running on localhost:5500 as PID 14920, bridge as PID 1056, local provider remains offline, Codex provider is ready, local and Codex smokes both kept Prada below 100 with Codex explicitly describing it as a secondary non-direct-match option.`

- timestamp: `2026-05-04 16:10:45 +09:00`
- note: `Reclassified into candidate-pool repair. score_total 8; full evaluation; single-session/no-spawn. Root issue: scored weak fillers still enter AI/fallback pools when direct selected-genre candidates exist, so Prada can remain visible even though it is not the nearest taste match.`

- timestamp: `2026-05-04 16:16:49 +09:00`
- note: `Verified candidate-pool repair. RecommendationService now gates AI/fallback pools to direct preferredGenre matches when any exist, then poster-derived liked genre matches, then broad candidates only when no taste match exists. Distinct movie selection no longer backfills duplicate showtimes. Focused service/scorer tests and bootJar passed outside sandbox after known native-platform.dll failure; Spring localhost:5500 is running as PID 23484 and bridge as PID 10872. Action/SF local fallback and Codex smokes both returned only Project Hail Mary, no Prada, no duplicate titles.`

- timestamp: `2026-05-04 15:08:00 +09:00`
- note: `Reclassified into a new analysis-quality bugfix. score_total 5; single-session/no-spawn. Root cause: LocalModelRecommendationClient.tasteMatchHints copied user-level liked poster genres into candidate tasteMatch when a candidate had no direct liked-genre overlap, allowing GPT/Codex to overclaim that unrelated movies matched action/SF/adventure/history poster taste.`

- timestamp: `2026-05-04 14:56:00 +09:00`
- note: `Verified the five recommendation-smoke goal. RecommendationService now tries exact filters first, then relaxed filters and broad TiDB candidates before returning no_filtered_candidates. Focused recommendation tests and bootJar passed; Spring is running on localhost:5500 as PID 22240 and AI bridge as PID 9200. Five local-provider recommendation smokes all returned count 3, and one Codex sanity smoke returned status ok/model codex/count 3. Local model provider remains offline until 127.0.0.1:1234/v1 is running, so local-provider API responses use fallback scoring.`

- timestamp: `2026-05-04 14:40:00 +09:00`
- note: `Reclassified current /goal from hourly crawling to recommendation reliability. score_total 7; full evaluation; Selfdex selected; single-session/no-spawn. Frozen contract: exact search filters must be tried first, relaxed only when empty, and five localhost:5500 recommendation smokes must all return non-empty recommendations if any usable TiDB candidates exist.`

- timestamp: `2026-05-04 14:27:00 +09:00`
- note: `Verified hourly server-side Lotte/Megabox sync direction. Browser entry/recommend flows no longer call /api/showtimes/refresh; scheduler defaults are hourly/startup with includeCgv=false; Lotte date-time release strings are normalized; recommendation run-history persistence is best-effort. Focused JS checks, backend tests, bootJar, served static checks, provider health, and recommendation smoke passed. Spring is running on localhost:5500 as PID 16960 with AI bridge python PID 17416. Local runtime was restarted with DABOYEO_SHOWTIME_STARTUP_ENABLED=false only to avoid duplicate startup crawl after the verified startup sync attempt; application default remains true.`

- timestamp: `2026-05-04 14:10:00 +09:00`
- note: `Reclassified current /goal from button-triggered crawling to deployed-server periodic crawling. score_total 8; full evaluation; single-session/no-spawn. Frozen contract: backend showtime sync should default to hourly/startup Lotte/Megabox TiDB collection with CGV excluded, while AI recommendation/direct-compare frontend flows stop calling the refresh endpoint and use stored TiDB data.`

- timestamp: `2026-05-04 14:16:00 +09:00`
- note: `Runtime startup sync exposed a Lotte ingest date parsing failure for strings shaped like yyyy-MM-dd 오전/오후 hh:mm:ss. Reclassified within the same task to score_total 9 with data_fidelity_risk and added CollectorBundleIngestCommand plus ingest tests to the write set before repairing normalization.`

- timestamp: `2026-05-04 14:24:00 +09:00`
- note: `Runtime recommendation smoke during startup sync exposed a transient TiDB communications timeout while saving recommendation_runs. Added RecommendationService and focused tests to the write set so run-history persistence can fail soft without blocking recommendation results.`

- timestamp: `2026-05-04 13:36:00 +09:00`
- note: `Verified button-triggered Lotte/Megabox TiDB refresh path. Added /api/showtimes/refresh, main-page AI/direct-compare entry refresh calls, AI recommendation preflight refresh, CGV-deferred response contract, bounded entry discovery defaults, and schedule trimming. Spring is running on localhost:5500 as PID 2700 with bridge worker PID 13688; refresh smoke completed as recent with 4 bundles and 80 showtimes, and recommendation API returned fallback/model gemma-4-e2b-it/count 3 from TiDB candidates while local model remains offline and Codex provider health remains ready.`

- timestamp: `2026-05-04 13:06:23 +09:00`
- note: `Reclassified to the user-fixed /goal. score_total 9; full evaluation; Selfdex selected. Selfdex read-only planner timed out after 180s, so main continues with Selfdex contract discipline and two read-only explorer slices while keeping all implementation writes in main. Goal is button-triggered bounded Lotte/Megabox crawl into TiDB, CGV deferred, backend recommendation/direct-comparison data backed by TiDB.`

- timestamp: `2026-05-04 12:59:47 +09:00`
- note: `Started bounded TiDB data refresh. score_total 8; full evaluation; single-session/no-spawn. Scope is current DB inspection, provider-date dry-run, bounded Lotte/Megabox crawl write, and post-write recommendation candidate verification while preserving the localhost:5500 Codex bridge runtime.`

- timestamp: `2026-05-04 12:56:17 +09:00`
- note: `Verified Codex recommendation analysis runtime on localhost:5500. ACL was updated only for C:\Users\pc07-00\.codex\sessions and C:\Users\pc07-00\.codex\tmp after user approval; an ignored backend/build/tools/codex.exe runtime copy made codex exec available to the bridge worker; Spring is running on PID 19928, bridge worker on PID 13796, provider health reports Codex ready, and POST /api/recommendations with aiProvider=codex returned status ok/model codex/count 3. Local provider remains offline until the OpenAI-compatible local model server is running on 127.0.0.1:1234/v1.`

- timestamp: `2026-05-04 12:51:01 +09:00`
- note: `User explicitly approved the ACL change needed for Codex exec. Continuing with a scoped permission update for C:\Users\pc07-00\.codex\sessions and C:\Users\pc07-00\.codex\tmp only, followed by Codex exec smoke, bridge worker restart, and recommendation POST verification.`

- timestamp: `2026-05-04 12:46:47 +09:00`
- note: `Blocked at Codex exec permission gate. Patched scripts/ai_bridge_agent.py for Windows executable resolution, UTF-8 subprocess decoding, and repo-local manual temp directories; py_compile and git diff --check passed. Spring 5500 and bridge worker can report Codex provider ready, but actual Codex recommendation POST still falls back because codex exec cannot create .codex session files without an ACL change. Local model remains offline because 127.0.0.1:1234/v1/models is not running.`

- timestamp: `2026-05-04 12:40:00 +09:00`
- note: `Runtime smoke found a bridge worker defect: provider health became Codex ready after tokenized Spring/worker restart, but a Codex recommendation POST fell back because Python subprocess execution of codex hit WinError 5. Scope expanded to patch scripts/ai_bridge_agent.py executable resolution and output decoding.`

- timestamp: `2026-05-04 12:35:46 +09:00`
- note: `Started Selfdex-guided runtime finalization. score_total 7; full evaluation; single-session/no-spawn; Selfdex planner command timed out after 180s, so the contract is frozen from local source, STATE evidence, provider health, Codex CLI readiness, and local model endpoint checks.`

- timestamp: `2026-05-04 12:27:00 +09:00`
- note: `Verified frontend GPT wording restore. The AI page source and Spring static mirror now show GPT/GPT-5.5 copy again, while internal provider value remains codex for the bridge route; Spring was rebuilt and restarted on localhost:5500 as PID 18360.`

- timestamp: `2026-05-04 11:45:00 +09:00`
- note: `Started frontend GPT wording restore. score_total 3; single-session/no-spawn; visible provider copy will return from Codex Worker to GPT while keeping the internal codex bridge route intact.`

- timestamp: `2026-05-04 11:38:00 +09:00`
- note: `Verified local Spring port migration to 5500. Default backend port, frontend API fallback origin, bridge worker server, env example, and local docs now point to 5500; Spring is running on localhost:5500 as PID 5228 and the browser search for 서현 renders Kakao map output plus 10 nearby theaters.`

- timestamp: `2026-05-04 11:35:00 +09:00`
- note: `Started local Spring port migration to 5500. score_total 6; single-session/no-spawn; scope is default port/origin alignment plus 5500 runtime/browser verification while preserving nearby map and AI bridge changes.`

- timestamp: `2026-05-04 11:28:00 +09:00`
- note: `Verified nearby theater Kakao SDK fallback repair. score_total 7; single-session/no-spawn; Spring static mirror rebuilt and restarted on 8080 as PID 6572; localhost browser search for 서현 now returns 10 nearby theaters with 목록 모드 fallback instead of SDK not ready / 0 results.`

- timestamp: `2026-05-04 10:30:00 +09:00`
- note: `Verified Codex/local-model AI bridge task. score_total 8; single-session/no-spawn; bridge endpoints, worker script, frontend Codex provider, .env example, focused tests, bootJar, runtime restart, token auth, and persistent provider heartbeat checks are complete.`

- timestamp: `2026-04-30 15:46:00 +09:00`
- timestamp: `2026-04-30 16:18:00 +09:00`
- timestamp: `2026-04-30 17:43:00 +09:00`
- note: `Reclassified PR #2 runtime check into a narrow runtime_fix. score_total remains 9; selected profile remains single-session/no-spawn because the issue is a Spring static mirror mismatch. User confirmed Kakao Maps is registered for port 5500 and the frontend key is correct; Spring on localhost:5500 served index.html with the old Kakao JS key, so backend static index must be aligned and the boot jar refreshed.`
- timestamp: `2026-04-30 17:48:00 +09:00`
- note: `Verified Spring localhost:5500 Kakao Maps key correction. backend static index now uses the same Kakao JS key as frontend/index.html, bootJar was rebuilt with system gradle, Spring was restarted on localhost:5500, /api/health returned 200, and /index.html contains the frontend key with the old key absent.`
- note: `Verified PR #2 Spring static mirror correction. score_total remains 9; single-session; backend static index/style/script/map/region constants now mirror selected frontend files, bootJar was rebuilt, Spring restarted as PID 2368, and 8080/index.html contains kmh region/nearby markers.`

## Verification Results

- taste_focused_candidate_pool_20260504:
  - `timestamp`: `2026-05-04 16:16:49 +09:00`
  - `classification`: `score_total 8; full evaluation; single-session/no-spawn because DB candidate selection, AI handoff, fallback rendering, and distinct movie behavior are one coupled recommendation surface.`
  - `implementation`: `RecommendationService now derives a taste-focused scored pool before AI/fallback selection: direct preferredGenres first, poster-derived likedGenres second, broad candidates only if no taste matches exist. selectDistinctMovieItems no longer fills result slots with duplicate showtimes of the same movie.`
  - `focused_tests`: `gradle test --tests kr.daboyeo.backend.service.recommendation.RecommendationServiceCandidateFilterTests --tests kr.daboyeo.backend.service.recommendation.RecommendationScorerTests passed outside sandbox after native-platform.dll failure.`
  - `build`: `gradle bootJar passed outside sandbox.`
  - `runtime`: `Spring localhost:5500 PID 23484; AI bridge python PID 10872; provider health local offline, codex ready.`
  - `smoke`: `local fallback action/SF request returned count 1, Project Hail Mary 100, hasPrada=false, duplicateTitleCount=0; Codex request returned status ok/model codex/count 1, Project Hail Mary 100, hasPrada=false, duplicateTitleCount=0.`
  - `diff_check`: `git diff --check passed with existing CRLF warnings only.`

- task: `Taste-focused recommendation candidate pool`
- score_total: `8`
- evaluation_fit: `full fit; this directly affects user trust and needed service tests plus real local/Codex runtime evidence.`
- orchestration_fit: `single-session fit; one service selection path plus focused tests was cheaper and safer than delegation.`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the score cap solved false confidence but not weak candidate visibility, so the task was reclassified to candidate-pool gating; runtime then exposed duplicate showtime backfill, which was also removed.`
- reviewer_findings: `Prada-style candidates are no longer shown while any direct selected-genre current-showing movie exists; duplicate showtimes no longer fill movie result cards.`
- verification_outcome: `focused tests, bootJar, provider health, local fallback smoke, Codex smoke, and git diff --check passed.`
- next_gate_adjustment: `future recommendation changes should verify both score correctness and candidate-pool membership, not only final score values.`

- genre_anchor_scoring_repair_20260504:
  - `timestamp`: `2026-05-04 16:03:23 +09:00`
  - `classification`: `score_total 8; full evaluation; single-session/no-spawn because scoring anchor, prompt hints, fallback analysis, and runtime smoke are one tightly coupled recommendation surface.`
  - `implementation`: `Separated explicit preferredGenres from poster-derived likedGenres, used preferredGenres as the direct taste anchor when present, capped no-direct-anchor candidates at 74, and aligned GPT/Codex tasteMatch plus fallback analysis with that anchor.`
  - `focused_tests`: `gradle test --tests kr.daboyeo.backend.service.recommendation.PreferenceProfileBuilderTests --tests kr.daboyeo.backend.service.recommendation.RecommendationScorerTests --tests kr.daboyeo.backend.service.recommendation.LocalModelRecommendationClientTests passed outside sandbox after native-platform.dll failure.`
  - `build`: `gradle bootJar passed outside sandbox.`
  - `runtime`: `Spring localhost:5500 PID 14920; AI bridge python PID 1056; provider health local offline, codex ready.`
  - `smoke`: `local fallback action/SF request returned Project Hail Mary 100, Devil Wears Prada 2 74, Super Mario 74, hasPradaAt100=false; Codex request returned status ok/model codex/count 3, hasPradaAt100=false and described Prada as a secondary option without direct selected-genre match.`
  - `diff_check`: `git diff --check passed with existing CRLF warnings only.`

- task: `Genre-anchor scoring repair`
- score_total: `8`
- evaluation_fit: `full fit; user-facing recommendation trust depended on source-level tests plus runtime local/Codex evidence.`
- orchestration_fit: `single-session fit; the changes touched one coupled recommendation scoring/prompt path and delegation would have raised handoff risk.`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `first cap fixed the perfect score but left a too-high 84 and prompt helpers still needed the same preferredGenre anchor.`
- reviewer_findings: `Prada-style secondary genre matches no longer reach 100 or direct tasteMatch when action/SF is selected; they remain only as low-confidence filler when DB coverage lacks enough direct genre candidates.`
- verification_outcome: `focused tests, bootJar, provider health, local fallback smoke, Codex smoke, and git diff --check passed.`
- next_gate_adjustment: `future recommendation scoring changes should test explicit selected genres separately from poster-derived secondary genres before runtime smoke.`

- genre_guided_recommendation_flow_20260504:
  - `timestamp`: `2026-05-04 15:37:53 +09:00`
  - `classification`: `score_total 9; full evaluation; single-session/no-spawn because frontend flow, backend survey/profile contract, candidate title inference, and GPT/Codex prompt are one coupled recommendation surface.`
  - `implementation`: `Added a genre step before posters, selected-genre poster filtering with fallback priority when fewer than three matching seeds exist, preferredGenres payload, three-poster minimum with five max, backend preferred genre weighting, richer GPT/Codex prompt instructions, larger GPT explanation limits, and current-release title-derived genre tags for Project Hail Mary / Demon Slayer-style / action franchise titles.`
  - `tests`: `node --check passed for frontend/src/js/pages/daboyeoAi.js and backend static mirror. Focused Gradle tests passed outside sandbox after the known native-platform.dll issue: PreferenceProfileBuilderTests, LocalModelRecommendationClientTests, RecommendationScorerTests. bootJar passed outside sandbox.`
  - `runtime`: `Spring restarted on localhost:5500 as PID 14008 with startup sync disabled only for local restart; Python AI bridge restarted as PID 11528. /api/health returned ok and provider health reports codex ready, local offline.`
  - `smoke`: `POST /api/recommendations with aiProvider=codex, preferredGenres action/SF, and three liked posters returned status ok/model codex/count 3; after title-tag repair the first recommendation was Project Hail Mary and the first explanation no longer led with direct-evidence-missing phrasing.`
  - `verification`: `git diff --check completed with CRLF warnings only; WORKSPACE_CONTEXT.toml required sections were checked.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification one scope expansion after runtime smoke exposed weak current-release genre tags; reviewer_findings selected genre signal now affects poster selection, scoring, and prompt context while tasteMatch stays candidate-specific; verification_outcome clean; next_gate_adjustment recommendation-analysis fixes should check both prompt wording and upstream candidate tags before trusting runtime behavior.`

- gpt_taste_match_prompt_fix_20260504:
  - `timestamp`: `2026-05-04 15:14:00 +09:00`
  - `classification`: `score_total 5; full evaluation; single-session/no-spawn because the bug is isolated to LocalModelRecommendationClient prompt construction and focused tests.`
  - `root_cause`: `LocalModelRecommendationClient.tasteMatchHints fell back to analysisHints(profile) when a candidate had no liked-genre overlap, so user-level liked poster genres such as action/SF/adventure/history were copied into every candidate tasteMatch and GPT/Codex could overclaim that unrelated titles matched those genres.`
  - `implementation`: `Removed the fallback from candidate tasteMatch and clarified the GPT/Codex prompt: liked_poster_hints are user-level context only, and the model must not claim poster-taste match when a candidate's tasteMatch is empty.`
  - `tests`: `Added LocalModelRecommendationClientTests coverage for unmatched candidates producing "tasteMatch":[] and directly matching candidates retaining tasteMatch overlap. Focused test passed outside sandbox after native-platform.dll sandbox failure.`
  - `runtime`: `gradle bootJar passed; Spring restarted on localhost:5500 as PID 8388 with DABOYEO_SHOWTIME_STARTUP_ENABLED=false only for local restart; Python AI bridge restarted as PID 22172; /api/health returned ok and provider health reports Codex ready, local offline.`
  - `verification`: `git diff --check for touched files completed with CRLF warnings only.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification none after root-cause confirmation; reviewer_findings user-level poster hints remain present but are scoped away from candidate-specific evidence; verification_outcome clean; next_gate_adjustment GPT-visible candidate fields should be named and populated as evidence, not as fallback narrative hints.`

- recommendation_five_smoke_20260504:
  - `timestamp`: `2026-05-04 14:56:00 +09:00`
  - `classification`: `score_total 7; full evaluation; Selfdex selected; single-session/no-spawn because candidate lookup, focused tests, rebuild, and runtime smoke are one coupled recommendation surface.`
  - `implementation`: `RecommendationService now wraps candidate lookup in findCandidatesWithFallback: exact filters are queried first, active filters are progressively relaxed, and finally broad upcoming TiDB candidates are used before an honest no_filtered_candidates response is returned. The scorer receives the effective filters that found candidates.`
  - `tests`: `gradle test --tests kr.daboyeo.backend.service.recommendation.RecommendationServiceCandidateFilterTests passed outside sandbox after the known native-platform.dll sandbox failure; added tests for relaxed region/time recovery and broad candidate fallback.`
  - `build_runtime`: `gradle bootJar passed; Spring restarted on localhost:5500 as PID 22240 with local startup showtime sync disabled only for this runtime restart; Python AI bridge restarted as PID 9200 with a runtime-only token. /api/health returned ok and provider health reports codex ready, local offline.`
  - `five_smoke`: `Five POST /api/recommendations calls with strict region/date/time/person filters all returned non-empty results: 01-seohyeon-expired-morning count 3, 02-gangnam-brunch count 3, 03-jeju-night count 3, 04-nowhere-large-party count 3, 05-busan-future-brunch count 3. All five local-provider calls returned fallback status because the local model server is offline, but the UI receives result cards.`
  - `codex_sanity`: `One extra POST /api/recommendations with aiProvider=codex returned status ok, model codex, count 3.`
  - `verification`: `git status --short, WORKSPACE_CONTEXT.toml read, WORKSPACE_CONTEXT section check, and git diff --check for the touched files completed; diff check reported CRLF warnings only.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification runtime restart exposed a PowerShell variable/API mismatch that was fixed and logged; reviewer_findings no fabricated recommendations, exact filters still run first, no-data remains honest if all searches are empty, and Codex token stays runtime-only; verification_outcome clean; next_gate_adjustment recommendation UX should distinguish exact-match recommendations from relaxed-condition recommendations.`

- hourly_showtime_sync_20260504:
  - `timestamp`: `2026-05-04 14:27:00 +09:00`
  - `classification`: `score_total 9; full evaluation; Selfdex selected; single-session/no-spawn because scheduler defaults, frontend trigger removal, ingest normalization, and recommendation resilience are coupled in one runtime contract.`
  - `implementation`: `Changed showtime scheduler defaults to hourly cron 0 0 * * * * and startup-enabled true, added DABOYEO_SHOWTIME_INCLUDE_CGV default false, made scheduled sync read includeCgv from config, removed browser entry/recommend refresh calls, cache-busted served JS, normalized Lotte date strings with attached Korean AM/PM time text, and made RecommendationService saveRun best-effort.`
  - `frontend_contract`: `Served index.html references 20260504-hourly-sync, and served main/AI page JS contains no showtimes/refresh, refreshShowtimesForEntry, requestEntryRefresh, or refreshShowtimes call from entry/recommend flows. The manual refresh API/client helper remains available but unused by those flows.`
  - `startup_sync_evidence`: `A real startup sync attempt logged trigger=scheduled, offsets=[0, 1, 2], includeCgv=false, cgvTargets=0, Lotte and Megabox auto-discovery, and successful TiDB writes for Lotte/Megabox bundles after the Lotte date parser repair.`
  - `runtime`: `Latest Spring jar is running on localhost:5500 as PID 16960; AI bridge python process is PID 17416; /api/health returns ok; /api/recommendation/providers/health reports codex ready and local offline. Local restart used DABOYEO_SHOWTIME_STARTUP_ENABLED=false only to prevent duplicate startup crawling after the verified startup sync attempt; application config default remains true.`
  - `recommendation_smoke`: `POST /api/recommendations with local provider returned status fallback, model gemma-4-e2b-it, count 3, first provider MEGABOX from TiDB-backed candidates. A first smoke during active sync exposed recommendation_runs save timeout; after fail-soft patch, focused tests passed and latest runtime returned recommendations.`
  - `verification`: `node --check passed for frontend/static script.js and daboyeoAi.js; rg served/source checks confirm entry/recommend pages no longer call refresh; gradle test passed outside sandbox for ShowtimeSyncServiceTests, CollectorBundleIngestCommandTests, and RecommendationServiceCandidateFilterTests after sandbox native-platform.dll failure; gradle bootJar passed; git diff --check passed with CRLF warnings only.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification runtime startup sync exposed Lotte source-date normalization and recommendation run-history persistence as necessary resilience fixes; reviewer_findings CGV stays excluded, browser no longer triggers crawl, secrets stay server-side, and nonessential run-history write failure no longer blocks recommendation results; verification_outcome clean; next_gate_adjustment periodic data collection should be verified under concurrent recommendation load, not only by unit tests.`

- entry_showtime_refresh_20260504:
  - `timestamp`: `2026-05-04 13:36:00 +09:00`
  - `classification`: `score_total 9; full evaluation; Selfdex selected; main-owned writes with two read-only explorer slices for frontend hook mapping and backend sync architecture.`
  - `selfdex`: `Read-only Selfdex planner timed out after 180s, so the task proceeded with the frozen local contract from STATE.md, source inspection, and read-only explorer evidence.`
  - `implementation`: `Added POST /api/showtimes/refresh, EntryShowtimeRefreshService, entry-only ShowtimeSyncService path excluding CGV, bounded entry refresh config, max schedules per bundle trimming, frontend client helper, main-page AI/direct-compare pre-navigation refresh calls, and AI recommendation preflight refresh.`
  - `runtime`: `Rebuilt bootJar and restarted Spring on localhost:5500 as PID 2700; restarted Codex bridge worker as PID 13688 with runtime-only token and ignored backend/build/tools/codex.exe command.`
  - `refresh_smoke`: `/api/showtimes/refresh reason=ai-entry returned running in about 8s, then reason=recommend-start returned recent after the background job completed with providers LOTTE_CINEMA/MEGABOX, cgvDeferred true, dateCount 1, bundleRequests 4, movies 112, theaters 232, screens 24, showtimes 80.`
  - `recommendation_smoke`: `POST /api/recommendations with local provider returned status fallback, model gemma-4-e2b-it, count 3, first provider MEGABOX; local model remains offline but TiDB-backed candidates are available.`
  - `provider_health`: `/api/recommendation/providers/health reports local offline and codex ready.`
  - `verification`: `node --check passed for changed frontend/static JS; gradle test --tests kr.daboyeo.backend.sync.showtime.ShowtimeSyncServiceTests passed outside sandbox after sandbox native-platform.dll failure; gradle bootJar passed; git diff --check passed with CRLF warnings only.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit main integration plus read-only explorers fit; predicted_topology main integration with read-only explorer support; actual_topology same; spawn_count 2 read-only; rework_or_reclassification initial entry refresh was too broad and was tightened by date/discovery/schedule caps after thread evidence; reviewer_findings CGV is excluded from entry path and secrets are not returned to browser; verification_outcome clean; next_gate_adjustment button-triggered collection must cap schedules before TiDB persistence, not only discovery targets.`

- local_codex_recommendation_runtime_20260504:
  - `timestamp`: `2026-05-04 12:56:17 +09:00`
  - `classification`: `score_total 7; full evaluation; single-session; no spawn because runtime token, Spring process, bridge worker, and Codex exec verification are one coupled surface.`
  - `selfdex`: `The read-only Selfdex planner timed out after 180s, so the task proceeded from local source, STATE evidence, and live runtime checks.`
  - `implementation`: `Patched scripts/ai_bridge_agent.py to resolve the Codex executable, decode subprocess output as UTF-8 with replacement, and create repo-local manual temporary directories instead of Python tempfile directories that hit Windows permission errors.`
  - `runtime`: `After user-approved ACL update for C:\Users\pc07-00\.codex\sessions and C:\Users\pc07-00\.codex\tmp, copied the local Codex binary to ignored backend/build/tools/codex.exe because the WindowsApps packaged executable is denied from the sandbox-outside worker context.`
  - `server`: `Spring is running on localhost:5500 as PID 19928; AI bridge worker is running as Python PID 13796 with a runtime-only bridge token.`
  - `verification`: `Copied codex.exe --version passed outside the sandbox; codex exec schema smoke returned {"ok":true}; /api/health returned ok; /api/recommendation/providers/health reports codex ready and local offline; POST /api/recommendations with aiProvider=codex returned status ok, model codex, and 3 recommendations.`
  - `local_provider_gap`: `Local model provider remains offline because 127.0.0.1:1234/v1/models is not reachable.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification runtime smoke exposed Windows-specific Codex executable and .codex ACL constraints; reviewer_findings bridge token stayed runtime-only and auth files were not copied; verification_outcome Codex analysis path live, local provider awaiting model server; next_gate_adjustment for local Codex bridge work, verify codex exec under the exact worker context before claiming ready.`

- frontend_gpt_wording_restore_20260504:
  - `timestamp`: `2026-05-04 12:27:00 +09:00`
  - `classification`: `score_total 3; light evaluation; single-session; no spawn because this was a small frontend/static mirror copy correction.`
  - `implementation`: `Restored the user-facing provider label/title/copy from Codex Worker/Codex analysis back to GPT/GPT-5.5/GPT analysis in frontend/src/js/pages/daboyeoAi.js and backend/src/main/resources/static/src/js/pages/daboyeoAi.js.`
  - `route_preservation`: `The internal provider value remains codex so the current backend Codex bridge route is preserved while the frontend reads as GPT.`
  - `runtime`: `gradle bootJar passed; Spring was restarted on localhost:5500 as PID 18360; GET /api/health on 127.0.0.1:5500 returned ok.`
  - `verification`: `node --check passed for frontend and Spring-static daboyeoAi.js; git diff --check passed with CRLF warnings only; served JS contains label GPT and title GPT-5.5 and no Codex Worker/Codex analysis visible strings.`
  - `retrospective`: `evaluation_fit light fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification none; reviewer_findings visible copy restored without breaking internal route; verification_outcome clean; next_gate_adjustment distinguish product-facing provider naming from wire provider naming.`

- local_spring_port_5500_20260504:
  - `timestamp`: `2026-05-04 11:38:00 +09:00`
  - `classification`: `score_total 6; light evaluation; single-session; no spawn because the port/default-origin changes are one tightly coupled runtime path.`
  - `implementation`: `Changed Spring default port to DABOYEO_BACKEND_PORT:5500, updated local frontend API defaults to 127.0.0.1:5500 with same-origin behavior for Spring-served pages, updated bridge worker/env defaults to 127.0.0.1:5500, added 5500 to fallback CORS defaults, mirrored changed frontend JS into Spring static resources, and updated local verification docs.`
  - `runtime`: `Stopped old 8080 Spring PID 6572, rebuilt bootJar, and restarted the jar on localhost:5500 as PID 5228; netstat shows 5500 listening and no 8080 listener.`
  - `browser_verification`: `In the in-app browser on http://localhost:5500/?v=20260504-port5500, entering 서현 produced feedback "경기 성남시 분당구 서현동 기준으로 주변 극장을 표시했습니다.", nearby-count 10, address "현재 위치: 경기도 성남시 분당구 중앙공원로39번길 35", list results including 메가박스 분당 and CGV 서현, and map text "현재 위치 100m".`
  - `verification`: `node --check passed for frontend and Spring-static client.js/liveMovies.js/daboyeoAi.js; python -m py_compile passed for scripts/ai_bridge_agent.py; git diff --check passed with CRLF warnings only; gradle bootJar passed outside the sandbox; GET /api/health on 127.0.0.1:5500 returned ok.`
  - `retrospective`: `evaluation_fit light fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification none; reviewer_findings default runtime and browser-visible origin are now aligned with Kakao localhost:5500 behavior; verification_outcome clean; next_gate_adjustment use 5500 as the default Spring local origin unless the user explicitly overrides DABOYEO_BACKEND_PORT.`

- nearby_kakao_sdk_fallback_20260504:
  - `timestamp`: `2026-05-04 11:28:00 +09:00`
  - `classification`: `score_total 7; full evaluation; single-session; no spawn because SDK readiness, geolocation UI, manual search, Spring static mirror, and browser verification all touch the same main-page surface.`
  - `root_cause`: `The Spring-served page could reach a state where Kakao Maps never exposed window.kakao.maps.load, while the frontend kept map/list rendering coupled to SDK readiness; manual search also depended on Kakao geocoding and stale browser cache could keep the old module active.`
  - `implementation`: `Added autoload=false to the Kakao SDK script, added a cache-busted kakaoMap module URL, made Kakao readiness explicit, decoupled nearby list rendering from map rendering, added local theater-database fallback search, and mirrored frontend fixes into backend Spring static resources.`
  - `runtime`: `gradle bootJar passed outside the sandbox; Spring restarted on 127.0.0.1:8080 as PID 6572; GET /api/health returned ok.`
  - `browser_verification`: `In the in-app browser on http://localhost:8080/?v=20260504-nearby-final, entering 서현 and pressing Enter produced feedback "CGV 서현 기준으로 주변 극장을 표시했습니다.", nearby-count 10, first results including CGV 서현, 메가박스 분당, CGV 판교, and map fallback text instead of SDK not ready.`
  - `verification`: `node --check passed for frontend/src/js/api/kakaoMap.js and backend/src/main/resources/static/src/js/api/kakaoMap.js; git diff --check passed with CRLF warnings only; bootJar passed.`
  - `privacy_note`: `Real browser geolocation coordinates were not read during verification; the geolocation success handler now uses the same updateMapWithServerData fallback path verified through manual location search.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification browser evidence shifted the fix from SDK race only to SDK-unavailable fallback; reviewer_findings Spring static mirrors and cache busting are required for 8080; verification_outcome clean with graceful SDK fallback; next_gate_adjustment for external SDK UI, keep data/list rendering independent from map rendering.`

- codex_local_ai_bridge_20260504:
  - `timestamp`: `2026-05-04 10:30:00 +09:00`
  - `classification`: `score_total 8; full evaluation; single-session; no spawn because the user invoked selfdex but did not explicitly request subagents and the provider/bridge/UI contract was tightly coupled.`
  - `implementation`: `Added a token-protected Spring AI bridge job queue, internal heartbeat/job/result endpoints, Codex provider routing, local-model bridge fallback, Codex/local provider health, and a stdlib Python bridge worker for local OpenAI-compatible models or codex exec.`
  - `frontend`: `Replaced the user-facing GPT provider option with Codex worker copy in frontend/src/js/pages/daboyeoAi.js and the Spring static mirror.`
  - `security`: `Bridge endpoints require X-DABOYEO-BRIDGE-TOKEN; the browser never receives the bridge token, local model URL, or Codex auth state; codex exec runs with read-only sandbox and ask-for-approval never.`
  - `runtime`: `gradle bootJar passed outside the sandbox; Spring restarted on 127.0.0.1:8080 as PID 4364; GET /api/health returned ok.`
  - `bridge_smoke`: `Unauthenticated bridge job polling returns HTTP 401; scripts/ai_bridge_agent.py --once with the repo-local token sends a Codex heartbeat; the persistent bridge agent is running as PID 6236; provider health reports codex ready and local offline until the local OpenAI-compatible model server is reachable.`
  - `verification`: `node --check passed for frontend and Spring-static daboyeoAi.js; python -m py_compile passed for scripts/ai_bridge_agent.py; codex --ask-for-approval never exec --help confirmed the CLI option ordering used by the bridge worker; focused Gradle tests passed for ApiExceptionHandlerTests, AiBridgeJobServiceTests, and LocalModelRecommendationClientTests; git diff --check passed with CRLF warnings only.`
  - `tool_gap`: `Selfdex external planning command timed out after 120s and was logged; the implementation proceeded from local source files and the frozen STATE contract.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification runtime smoke exposed ResponseStatusException being wrapped as 500, so ApiExceptionHandler was narrowed to preserve 401/503/404 statuses; reviewer_findings bridge auth and fallback are now explicit; verification_outcome clean; next_gate_adjustment when adding internal endpoints, smoke unauthenticated and authorized paths before runtime signoff.`

- spring_5500_kakao_key_fix_20260430:
  - `timestamp`: `2026-04-30 17:48:00 +09:00`
  - `classification`: `score_total 9 continuation; single-session/no-spawn; runtime_fix inside the frozen PR #2 static mirror scope.`
  - `root_cause`: `frontend/index.html used the valid Kakao JS key for the localhost:5500 setup, but backend/src/main/resources/static/index.html still used the old key, so Spring-served index on port 5500 loaded the wrong Kakao SDK URL.`
  - `implementation`: `Updated backend static index.html to use the frontend Kakao JS key, rebuilt bootJar with system gradle, and restarted Spring on localhost:5500.`
  - `verification`: `GET /api/health on localhost:5500 returned 200; GET /index.html returned 200 with dapi.kakao.com, the frontend Kakao key present, the old key absent, and #map present; backend/build/resources/main/static/index.html also contains the corrected key.`
  - `tool_gap`: `In-app browser automation could not be used because node_repl failed to start with Access denied, so verification used local HTTP/static checks.`
  - `diff_check`: `git diff --check passed with CRLF warnings only.`
  - `retrospective`: `evaluation_fit light runtime fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification port 5500 made the key mismatch visible; reviewer_findings Spring static mirrors must include key-bearing script tags, not only local JS/CSS files; verification_outcome clean; next_gate_adjustment compare external SDK script tags when mirroring frontend pages into Spring static resources.`

- kmh_index_static_mirror_20260430:
  - `timestamp`: `2026-04-30 17:32:35 +09:00`
  - `classification`: `score_total 9 continuation; full evaluation; single-session; no spawn because the fix was a static mirror gap inside the already frozen PR #2 selective import.`
  - `root_cause`: `frontend/index.html had the kmh region/nearby changes, but http://127.0.0.1:8080/index.html is served from backend/src/main/resources/static/index.html, which was still old.`
  - `implementation`: `Copied the selected imported frontend index dependencies into Spring static mirrors: index.html, style.css, kakaoMap.css, kakaoMap.js, script.js, and constants/regions.js where needed.`
  - `verification`: `node --check passed for backend static script.js, kakaoMap.js, and regions.js; static index contains region-select-wrapper, nearby-section, DABOYEO_REGIONS, clusterer, and module kakaoMap.js markers.`
  - `runtime`: `gradle bootJar passed; Spring jar restarted on 127.0.0.1:8080 as PID 2368; GET /api/health returned 200; GET /index.html?static=kmh returned HasRegionSelect=true, HasNearbySection=true, HasRegionsModule=true.`
  - `diff_check`: `git diff --check passed with CRLF warnings only.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification user observed runtime page mismatch, revealing frontend-source import was not enough for Spring-served static pages; reviewer_findings route served by 8080 now matches selected kmh index frontend; verification_outcome clean; next_gate_adjustment when importing frontend for a Spring-served page, mirror frontend files into backend static before runtime signoff.`

- pr2_kmh_nearby_refresh_region_map_selective_integration_20260430:
  - `timestamp`: `2026-04-30 16:18:00 +09:00`
  - `classification`: `score_total 9; full evaluation; single-session; no spawn because the user did not request delegation and safe import depended on one dirty working tree plus tightly coupled config/sync/frontend route decisions.`
  - `github_pr`: `PR #2 [작업 수정 내용] from kmh to lsh; no issue comments, no review threads, head b7a8147, base bdd0c61, mergeable=false, 228 changed files.`
  - `merge_risk`: `origin/kmh was not merged wholesale because it contains broad frontend/backend/docs/db churn, route moves, recommendation file overlap, and conflicts with current uncommitted work.`
  - `backend`: `Imported nearby background showtime refresh, provider-target resolver, bridge/showtime/seat package reorganization, startup showtime sync gate, 3-day showtime/seat cleanup, Lotte theater-targeted discovery, Megabox area-targeted discovery, LiveMovieRepository overnight time query, and LiveMovieSearchCriteria cross-midnight support.`
  - `config`: `application.yml was manually merged so PR sync/demo/CORS settings were added while current GPT recommendation settings and frontend-origins config were preserved.`
  - `frontend`: `Imported current-route-safe region select data, main-page 3-step region selector, embedded nearby map section, map module, liveMovies region-coordinate resolution, overnight time filtering, and supporting CSS while keeping src/pages routes and excluding src/basic route rewrites/API client deletions.`
  - `collectors_scripts`: `Added Lotte ticketing page caching and showtime location-link repair for Lotte/Megabox ingest; skipped unrelated seat-status normalization change and branch-wide docs/db churn.`
  - `preservation`: `Existing recommendation/GPT/poster files, current daboyeoAi pages, PR #1 allMovies import, CGV seat-layout controller, and current R2/static assets were preserved.`
  - `verification`: `node --check passed for frontend/src/js/pages/script.js, frontend/src/js/api/kakaoMap.js, frontend/src/js/liveMovies.js, and frontend/src/js/constants/regions.js; python -m py_compile passed for collectors/lotte/collector.py and scripts/ingest/collect_all_to_tidb.py; focused Gradle tests passed outside the sandbox for LiveMovieServiceNearbyRefreshTests, NearbyShowtimeRefreshServiceTests, ShowtimeSyncSchedulerTests, ShowtimeCleanupServiceTests, ShowtimeSyncServiceTests, and CgvSeatMapControllerTests; git diff --check passed with CRLF warnings only.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification user added frontend map scope mid-run and Gradle exposed the missing cross-midnight criteria method; reviewer_findings PR is useful but unsafe as a full merge, and current-route selective import avoided API/client and route regressions; verification_outcome clean; next_gate_adjustment include frontend map/liveMovies in future region-search imports before testing.`

- pr1_ksg_frontend_selective_integration_20260430:
  - `timestamp`: `2026-04-30 15:31:24 +09:00`
  - `classification`: `score_total 8; full evaluation; single-session; no spawn because user did not request delegation and the import decision depended on current lsh routes, assets, and dirty recommendation work.`
  - `github_pr`: `Open PR #1 [슬기] 프론트 from ksg to lsh; no issue comments, no review threads, head e08c53c, base bdd0c61, mergeable=false.`
  - `merge_risk`: `HEAD..origin/ksg would delete current R2 poster assets, cgvSeatMap/movie/map pages, liveMovies, map data, and other current lsh frontend files while also conflicting with STATE/ERROR_LOG/backend recommendation files. Whole-branch merge was rejected.`
  - `implementation`: `Selected only frontend/src/pages/allMovies.html and frontend/src/css/allMovies.css from origin/ksg, then repaired favicon paths for the current src/pages route.`
  - `preservation`: `Current frontend/src/pages route structure, index/script seat routes, frontend/src/assets/R2/posters, existing daboyeoAi changes, backend recommendation changes, and untracked poster tag catalog were preserved.`
  - `excluded`: `goods_events/** was not imported because it adds a crawler/package with external public-request behavior and is not wired into the current app request; ksg branch-wide basic/ page moves were also excluded to avoid route deletion.`
  - `verification`: `node --check passed for frontend/src/js/pages/script.js, frontend/src/js/pages/daboyeoAi.js, and backend/src/main/resources/static/src/js/pages/daboyeoAi.js; git diff --check passed with CRLF warnings only; WORKSPACE_CONTEXT required sections were found.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification initial PowerShell pipe git apply failed on UTF-8 patch handling and was resolved via git diff --output; reviewer_findings PR is not safe to merge wholesale but allMovies visual cleanup is safe after path correction; verification_outcome clean; next_gate_adjustment require explicit user approval before importing goods_events crawler or rewriting ksg branch.`

- gpt_fast_precise_analysis_enhancement_20260430:
  - `timestamp`: `2026-04-30 14:59:50 +09:00`
  - `classification`: `score_total 7; full evaluation; single-session; no spawn because user did not request delegation and GPT prompt contract, candidate payload, configuration, and tests were tightly coupled.`
  - `implementation`: `GPT default candidate windows were widened from fast=6/precise=8 to fast=8/precise=12, GPT max tokens were increased to fast=720/precise=1300, and GPT response text limits were widened to fast=180/precise=320 while local model limits stayed compact.`
  - `prompt_contract`: `GPT fast now uses GPT_FAST single-pass evidence-based comparison; GPT precise now uses GPT_PRECISE full-candidate comparison with poster taste, avoid-risk handling, practical showtime value, and tradeoff versus nearby candidates.`
  - `candidate_payload`: `GPT candidate JSON now includes tasteMatch, scheduleFit, practicalValue, watchRisks, and precise-only tradeoffHints in addition to fitHints, without exposing raw scores, matchedTags, or penalties.`
  - `response_quality`: `RecommendationService now preserves longer GPT precise reason/value/analysis text before falling back to grounded code-generated text, while local mode still normalizes compact tag-style responses.`
  - `tests`: `Focused Gradle tests passed for LocalModelRecommendationClientTests, RecommendationServiceCandidateFilterTests, and RecommendationServiceQualityTests after fixing a fast/precise prompt-boundary test failure.`
  - `runtime`: `gradle bootJar passed outside the sandbox; Spring boot jar restarted on 127.0.0.1:8080 as PID 7200; GET /api/health returned 200.`
  - `verification`: `node --check passed for frontend/src/js/pages/daboyeoAi.js and backend/src/main/resources/static/src/js/pages/daboyeoAi.js; git diff --check passed with CRLF warnings only.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification fast prompt still mentioned precise-only tradeoffHints until tests caught it; reviewer_findings GPT is richer but remains grounded to supplied candidates; verification_outcome clean with known sandbox Gradle native-platform.dll issue logged as resolved; next_gate_adjustment if GPT live latency is acceptable, consider showing a slightly richer GPT precise result label in UI copy.`

- poster_tag_catalog_and_back_button_20260430:
  - `timestamp`: `2026-04-30 14:37:42 +09:00`
  - `classification`: `score_total 7; full evaluation; single-session; no spawn because user did not request delegation and the metadata catalog, service loader, tests, static mirror, and browser-comment fix were tightly coupled.`
  - `source_lookup`: `Fetched each of the 50 movie detail pages by movieCd from https://cinematheque.kr/cine/view/{movieCd}; source genres were used as the primary basis, with conservative obvious recommendation tags added for mood, pace, audience, and avoid signals.`
  - `implementation`: `Added korea-boxoffice-top50-poster-tags.json keyed by movieCd and updated PosterSeedService to load those per-movie tags while keeping the random 12 poster seed flow unchanged.`
  - `preference_profile`: `PreferenceProfileBuilderTests now verifies that liked poster seeds add movie-specific genre/mood/audience weights such as action/history/comedy/fantasy instead of only genre:popular.`
  - `ui_fix`: `daboyeoAi.js no longer moves aiBackButton into the split layout; renderSplitLayout calls resetBackButtonToTopbar, and browser DOM on the poster step shows the button under banner before DABOYEO instead of main > section > div > button.`
  - `runtime`: `gradle bootJar passed earlier in this task; Spring boot jar restarted on 127.0.0.1:8080 as PID 13740; GET /api/health returned 200.`
  - `tag_coverage`: `Node JSON check returned manifestCount=50, tagCount=50, missing=[], emptyGenres=[].`
  - `poster_seed_api`: `GET /api/recommendation/poster-seed?limit=12 returned 12 items with movie-specific tags; sampled results included 설국열차 sf/action, 범죄도시2 crime/action/thriller, 캡틴 아메리카: 시빌 워 action/sf/thriller, 명량 history/action, 겨울왕국 2 animation/musical/family.`
  - `browser_check`: `In-app browser loaded /src/pages/daboyeoAi.html?v=20260430-poster-tags, advanced through audience, mood, avoid, and poster steps, and captured a poster-step screenshot; DOM hierarchy confirmed aiBackButton stayed in the topbar banner.`
  - `verification`: `node --check passed for frontend/src/js/pages/daboyeoAi.js and backend/src/main/resources/static/src/js/pages/daboyeoAi.js; focused Gradle tests passed after re-running outside the sandbox; git diff --check passed with CRLF warnings only; WORKSPACE_CONTEXT.toml required section checks passed.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification user clarified per-poster metadata was required, so broad seed defaults were replaced with a searched per-movie catalog; reviewer_findings random poster flow and static mirror stayed intact; verification_outcome clean with known sandbox Gradle/PowerShell JSON parser issues logged as resolved; next_gate_adjustment if metadata quality needs more precision, store explicit source genre text beside normalized tags.`

- ai_recommendation_fallback_differentiation_20260430:
  - `timestamp`: `2026-04-30 13:55:28 +09:00`
  - `classification`: `score_total 7; full evaluation; single-session; no spawn because user did not request delegation and scoring, derived tags, poster seed defaults, and runtime response quality were tightly coupled.`
  - `implementation`: `ShowtimeCandidate.allTags now derives conservative recommendation tags from current provider titles, age ratings, formats, and screen hints for obvious animation/child/family, date/friends/light, horror/thriller/tense, music/live, calm/immersive, and premium visual cases. Raw provider persistence was not changed.`
  - `poster_seed`: `PosterSeedService no longer assigns generic visual/immersive moods, broad alone/friends/family audiences, or rank-derived pace to every KOBIS/R2 poster seed. genre:popular remains a lightweight seed weight but PreferenceProfileBuilder no longer exposes it as a liked genre for analysis text.`
  - `response_text`: `RecommendationService decodes HTML entities in display fields, maps known English genre tags to Korean labels, and suppresses generic provider genre labels such as genre:일반콘텐트, genre:popular, and MEGA-only style labels from reason/analysis tags.`
  - `tests`: `gradle test --tests RecommendationScorerTests --tests PreferenceProfileBuilderTests --tests RecommendationServiceQualityTests passed after fixing an Optional import compile failure.`
  - `runtime`: `gradle bootJar passed; Spring boot jar restarted on 127.0.0.1:8080 as PID 20208; GET /api/health returned 200; provider health reports local and GPT offline, so recommendation status is honest fallback.`
  - `api_comparison`: `Four POST /api/recommendations checks returned 3 fallback cards each: child/light ranked Super Mario and animation titles; friends/tense ranked 살목지 first with #스릴러/#긴장감/#공포; alone/calm ranked 류이치 사카모토: 코다 first; date/exciting ranked 악마는 프라다를 입는다 2 first.`
  - `poster_seed_check`: `GET /api/recommendation/poster-seed?limit=12 returned 12 seeds with genres=popular and empty moods/audiences/pace for the sampled first item.`
  - `verification`: `git diff --check passed with CRLF warnings only; git status shows expected modified task files only; WORKSPACE_CONTEXT.toml required section checks passed.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification runtime comparison revealed poster seed flattening and generic provider label noise after the first derived-tag pass; reviewer_findings fallback now differentiates obvious personas without claiming live model success or mutating raw DB data; verification_outcome clean; next_gate_adjustment if future poster seed metadata becomes richer, replace genre:popular placeholder with real seed genres instead of adding broad defaults.`

- ai_recommendation_display_entity_decode_20260430:
  - `timestamp`: `2026-04-30 13:31:23 +09:00`
  - `classification`: `score_total 6; light evaluation; single-session; no spawn because the concrete defect was one backend response-mapping path plus a focused test.`
  - `quality_probe`: `Four representative recommendation requests returned fallback recommendations after showtime recovery; the date/exciting case exposed a literal HTML entity title &#40;더빙&#41; 슈퍼 마리오 갤럭시.`
  - `implementation`: `RecommendationService now decodes basic named and numeric HTML entities in user-facing RecommendationItem title, theaterName, regionName, screenName, and sanitized AI/fallback text. Raw provider persistence was not changed.`
  - `test_update`: `RecommendationServiceQualityTests adds recommendationDisplayTextDecodesHtmlEntities covering numeric entities, amp, apostrophe, and greater-than in display fields.`
  - `runtime`: `Stopped stale Spring PID 19872, rebuilt boot jar with gradle bootJar, and restarted Spring as PID 14040 on 127.0.0.1:8080.`
  - `verification`: `node --check passed for frontend/src/js/pages/daboyeoAi.js; sandbox Gradle failed on native-platform.dll, then elevated RecommendationServiceQualityTests passed; gradle bootJar passed; GET /api/health returned 200; recommendation POST returned fallback with 3 items and title (더빙) 슈퍼 마리오 갤럭시 with has_entity=false; git diff --check passed with CRLF warnings only.`
  - `retrospective`: `evaluation_fit light fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification read-only quality probe became a narrow implementation after literal provider HTML entities appeared in user-facing titles; reviewer_findings display text is cleaned at API boundary while raw provider data remains preserved; verification_outcome runtime and tests are clean; next_gate_adjustment after data freshness, test multiple persona payloads before judging final demo polish.`

- ai_recommendation_demo_runtime_recovery_20260430:
  - `timestamp`: `2026-04-30 13:21:57 +09:00`
  - `classification`: `score_total 7; full evaluation; single-session; no spawn because server health, DB freshness, provider ingest, and recommendation response were sequentially dependent.`
  - `server`: `Spring boot jar is running as PID 19872 on 127.0.0.1:8080; GET /api/health returned 200.`
  - `static_assets`: `GET /src/pages/daboyeoAi.html returned 200 and loaded daboyeoAi.js with cache-bust markers; GET /src/assets/R2/posters/25-20183782-20183782.webp returned 200 with 795766 bytes.`
  - `poster_seed`: `GET /api/recommendation/poster-seed?limit=12 returned 12 local seed movies with /src/assets/R2/posters/*.webp URLs.`
  - `provider_health`: `GET /api/recommendation/providers/health returned local and GPT providers as offline, so demo recommendations should present fallback honestly rather than live model analysis.`
  - `before_recovery`: `POST /api/recommendations returned status no_usable_showtimes; TiDB read-only coverage showed total_showtimes=634, max_starts_at=2026-04-29 22:25:00, future_now=0, future_plus_30m=0.`
  - `dry_run`: `collect_all_to_tidb.py --provider all --all-provider-dates --max-provider-dates 1 --limit-schedules 20 --dry-run found Lotte 2026-04-30 and Megabox 20260430 provider dates.`
  - `ingest`: `Megabox bounded write for 20260430 upserted movies=28, theaters=11, screens=200, showtimes=200, movie_tags=71, seat snapshots=0.`
  - `after_recovery`: `TiDB coverage showed total_showtimes=834, max_starts_at=2026-04-30 22:55:00, future_now=200, future_plus_30m=200, all currently from MEGABOX.`
  - `recommendation_api`: `POST /api/recommendations with aiProvider=gpt returned status fallback, model gpt-5.5, recommendationCount=3, firstTitle=슈퍼 마리오 갤럭시.`
  - `environment_notes`: `Default sandbox background Spring process did not persist; server was started outside sandbox. Default sandbox Python could not import PyMySQL, while the user Python environment already had PyMySQL 1.1.2.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification read-only runtime verification became bounded DB freshness recovery after no_usable_showtimes and future_now=0; reviewer_findings demo is presentable as fallback, not live GPT/local analysis, and poster images use local R2 assets; verification_outcome runtime recovered; next_gate_adjustment refresh future showtime coverage before judging recommendation UI or model quality.`

- cgv_key_scope_no_direct_call:
  - `timestamp`: `2026-04-29 17:31:21 +09:00`
  - `env_scope`: `Root .env has CGV_API_SECRET set and TiDB connection values set; no CGV_ID or CGV_PASSWORD value was present in the redacted key-state scan.`
  - `code_inferred_scope`: `collectors/cgv/api.py signs requests with X-TIMESTAMP and X-SIGNATURE using CGV_API_SECRET and defines public booking/display collection paths for movie list, movie attributes, regions/sites, dates by movie, schedules by movie, and seat data.`
  - `browser_observed_scope`: `Public CGV UI showed movie chart, booking flow, theater/date/showtime selection, and remaining/total seat counts; clicking a showtime produced a login-required modal before seat selection.`
  - `loaded_resource_trace`: `The already-loaded homepage trace showed public CGV display/common/OIDC/ad/analytics resources; no admin, manager, or backoffice path was observed or tested.`
  - `explicit_non_actions`: `No direct CGV API request was made by curl, Python, Postman, fetch, HEAD, OPTIONS, or admin-token flow; no endpoint guessing or scanning was performed.`

- cgv_signature_scope_static_only:
  - `timestamp`: `2026-04-29 17:42:51 +09:00`
  - `method`: `Static repo inspection only; no CGV network request, browser navigation, status probe, or endpoint guessing.`
  - `secret_state`: `.env contains CGV_API_SECRET set; no CGV login username/password key is set.`
  - `implemented_signed_paths`: `searchAtktTopPostrList, searchAtktTopPostrAttrList, searchAllRegionAndSite, searchSiteScnscYmdListByMov, searchSchByMov, and searchIfSeatData.`
  - `inferred_access_ceiling`: `Movie catalog/attributes, region and theater master data, available screening dates, showtime schedules with booking keys and remaining/total seat counts, and seat layout/status data for a chosen public screening if required booking keys are known.`
  - `not_supported_by_current_evidence`: `No account profile, payment, reservation-confirmation, member-only mypage, or admin/manager/backoffice access path was found in the CGV signing client or probe script.`

- cgv_official_loaded_file_security_inspection:
  - `timestamp`: `2026-04-30 09:51:15 +09:00`
  - `method`: `Normal browser loads of https://cgv.co.kr/, /cnm/movieBook, and /cnm/movieBook/movie plus CDP response-body inspection of loaded CGV JS/resources; no custom signed API call, no endpoint probing, no login token, and no admin-like direct access.`
  - `coverage`: `111 unique CGV resources, 63 unique CGV JS files, about 3.6 MB of CGV JS, 15 naturally loaded api.cgv.co.kr paths, and 129 endpoint strings found in loaded public JS.`
  - `public_signing_key`: `The public CGV chunk 1453-58ae862b23257487.js contains an interceptor that creates X-TIMESTAMP and X-SIGNATURE using HmacSHA256 over timestamp|pathname|body; the embedded 43-character signing string equals the local .env CGV_API_SECRET without printing the value.`
  - `signing_scope`: `The frontend applies the signature to request URLs starting with https://api.cgv.co.kr and https://event.cgv.co.kr, sets credentials=include, and adds Bearer accessToken when an accessToken cookie is present.`
  - `naturally_loaded_api_paths`: `/act/resv/actResv/searchHeaderActSiteList; /cnm/atkt/searchAtktTopPostrAttrList; /cnm/atkt/searchAtktTopPostrList; /cnm/atkt/searchOnlyCgvMovList; /cnm/atkt/searchSscnsCdList; /cnm/site/searchAllRegionAndSite; /com/bznsCom/mngrNtce/selectMngrNtceProcedure; /com/bznsCom/screnMng/checkScrenUrlValid; /com/bznsCom/user/searchComcdValList; /met/dsp/scrDsp/search* display paths; /met/emrg/searchMainEmrg.`
  - `static_endpoint_surface`: `Loaded public JS references many cinema/booking read endpoints including searchIfSeatData, searchSchByMov, searchSiteScnscYmdListByMov, seat price/info paths, site/theater paths, and activity reservation search paths; it also references payment/member/write-like paths, but static references do not prove access without server-side auth/session.`
  - `security_interpretation`: `The CGV_API_SECRET is effectively public because it is shipped in CGV frontend JS; X-SIGNATURE alone should not be treated as authorization. Real protection must come from cookies/accessToken/server-side authorization and business checks.`
  - `risk_boundary`: `No /admin, /manager, or /backoffice path was observed in the inspected public route JS; payment/member/reservation endpoints exist in public bundles but were not called or tested for authorization.`

- cgv_api_fetch_attempt_20260429:
  - `timestamp`: `2026-04-29 15:45:53 +09:00`
  - `request_update`: `User explicitly asked to use the API instead of page scraping.`
  - `live_attempt`: `python scripts\cgv_collector_demo.py --mode movies stopped before network I/O because CgvApiClient found no usable CGV_API_SECRET value.`
  - `implementation_status`: `The collector uses CGV signed API endpoints including /cnm/atkt/searchSchByMov and /cnm/atkt/searchIfSeatData; Spring exposes GET /api/cgv/seat-layout and keeps signing server-side.`
  - `verification`: `python py_compile passed for collectors/cgv/api.py, collectors/cgv/collector.py, and scripts/cgv_collector_demo.py; node --check passed for frontend/static cgvSeatMap.js and client.js; sample JSON parsed with 123 seats and 2 zone boxes; frontend/static secret scan found no matches; focused CgvSeatMapControllerTests passed outside sandbox after Gradle native-platform.dll failed inside sandbox; git diff --check passed with CRLF warnings only.`
  - `blocker`: `Set a real CGV_API_SECRET in the root .env or process environment, then rerun scripts\cgv_collector_demo.py --mode seat-layout with current CGV booking keys or call /api/cgv/seat-layout from the Spring backend.`

- cgv_realtime_coordinate_seat_map:
  - `timestamp`: `2026-04-29 17:37:00 +09:00`
  - `classification`: `score_total 9; full evaluation; single-session; no spawn because the Spring endpoint, Python bridge, collector layout output, API client, and coordinate renderer share one tight live-data contract.`
  - `implementation`: `Added GET /api/cgv/seat-layout with validated CGV booking-key params; PythonCollectorBridge now calls CgvCollector.build_seat_layout server-side; cgvSeatMap.html/css/js renders live or uploaded/sample CGV x/y/w/h coordinates with status filters, zoom, selected-seat detail, JSON upload, sample fallback, and 15-second auto-refresh.`
  - `collector`: `CgvCollector build_seat_records, summarize_seat_map, and build_seat_layout now accept optional seat_area_no; scripts/cgv_collector_demo.py exposes --mode seat-layout and --seat-area-no.`
  - `static_runtime`: `The new cgvSeatMap page, CSS, JS, API client, and cgv-seat-layout.sample.json were mirrored from frontend/src into backend/src/main/resources/static/src.`
  - `security`: `CGV signing and CGV_API_SECRET remain server/Python collector concerns; frontend/static scan found no CGV_API_SECRET, X-TIMESTAMP, X-SIGNATURE, api_secret, or secret strings.`
  - `verification`: `python -m py_compile passed for collectors/cgv/collector.py and scripts/cgv_collector_demo.py; node --check passed for frontend and backend-static cgvSeatMap.js and client.js; sample JSON parsed with 123 seats, 2 zone boxes, and status counts special=26 available=65 sold=32; focused Gradle test CgvSeatMapControllerTests passed after elevated Gradle native-platform.dll access; git diff --check passed with CRLF warnings only; frontend/backend static mirrors matched for JS/CSS/client.`
  - `live_limit`: `No live CGV upstream smoke was run because the current env has no usable CGV_API_SECRET and network approval/live keys were not provided.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification static renderer expanded to realtime endpoint after the user clarified the requirement; reviewer_findings explicit RequestParam names avoid compile-flag fragility and browser code never receives signing material; verification_outcome local contract is verified, while true realtime freshness needs a valid current CGV booking key plus CGV_API_SECRET; next_gate_adjustment if this becomes production-facing, add request throttling/caching around the live CGV endpoint.`

- cgv_signed_seat_map_collection:
  - `timestamp`: `2026-04-29 16:38:00 +09:00`
  - `classification`: `score_total 8; full evaluation; single-session; no spawn because user did not request delegation and CGV collector output plus backend snapshot normalization had one tight contract.`
  - `implementation`: `CgvCollector now converts CGV searchIfSeatData seats into stable seat_key, x/y/width/height, normalized_status, and a UI-ready build_seat_layout payload; scripts/cgv_collector_demo.py adds --mode seat-layout.`
  - `backend`: `SeatSnapshotStatusNormalizer now maps CGV code 00 to available, code 01 or sale N to sold, sale Y to available, and blocked names before sale availability; SeatSnapshotPersistenceService accepts seat_key and CGV coordinate aliases.`
  - `security`: `CGV_API_SECRET remains loaded only from process env or repo-root .env; static scan found only the .env.example placeholder plus existing signing header usage.`
  - `verification`: `python -m py_compile passed for collectors/cgv/api.py, collectors/cgv/collector.py, scripts/cgv_collector_demo.py, and scripts/cgv_api_probe.py; synthetic CGV seat payload check returned records=2, remaining=1, zoneBoxes=1; sandbox Gradle failed on native-platform.dll, then elevated focused tests passed for SeatSnapshotStatusNormalizerTests and SeatSnapshotSyncServiceTests; git diff --check passed with CRLF warnings only.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification no schema change was needed because existing snapshot tables can store CGV coordinates; reviewer_findings signed API boundary is intact and CGV status mapping no longer drops 00/01-only rows; verification_outcome collector and backend contract are locally verified, while live CGV API smoke remains intentionally skipped unless explicitly requested; next_gate_adjustment if layout persistence becomes required, add a focused seat_layouts upsert path instead of overloading seat_snapshots.`

- r2_local_poster_seed_routing:
  - `timestamp`: `2026-04-29 14:59:00 +09:00`
  - `classification`: `score_total 4; light evaluation; single-session; no spawn because PosterSeedService, test expectations, build resources, and boot-jar patch formed one narrow seed-source switch.`
  - `implementation`: `PosterSeedService now reads recommendation/korea-boxoffice-top50-posters.json and maps each movie to the existing PosterSeedMovie response shape with id=movieCd and posterUrl=/src/assets/R2/posters/*.webp; DB image storage was not added.`
  - `metadata_note`: `The R2/KOBIS manifest currently has poster path and ranking fields, not rich genre/mood metadata, so the service attaches minimal generic poster preference tags popular, visual, immersive, and audience defaults until a richer local metadata file exists.`
  - `test_update`: `PreferenceProfileBuilderTests now use KOBIS movieCd seed ids and assert the generic local seed preference weights.`
  - `static_runtime`: `korea-boxoffice-top50-posters.json was copied into backend/build/resources/main/recommendation and patched into the boot jar with PosterSeedService.class; Spring was restarted from the patched jar as PID 18200 on 127.0.0.1:8080.`
  - `verification`: `javac compiled PosterSeedService and PreferenceProfileBuilderTests with a minimal classpath; /api/health returned ok; /api/recommendation/poster-seed?limit=12 returned 12 items and all posterUrl values matched /src/assets/R2/posters/*.webp; a representative R2 poster URL returned HTTP 200; POST /api/recommendations accepted three returned KOBIS ids and returned fallback with 3 recommendations; boot jar contains PosterSeedService.class and korea-boxoffice-top50-posters.json; git diff --check passed with CRLF warnings only.`
  - `retrospective`: `evaluation_fit light fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification no DB path was needed; reviewer_findings poster selection is now local-static and presentation-safe, but richer seed metadata should be added later if poster choices need stronger taste differentiation; verification_outcome current 8080 server serves R2 poster seed successfully; next_gate_adjustment keep image binaries in static/R2 and recommendation metadata in classpath JSON rather than movie DB columns.`

- fallback_result_presentation_clarity:
  - `timestamp`: `2026-04-29 14:32:00 +09:00`
  - `classification`: `score_total 5; full evaluation; single-session; no spawn because fallback result rendering, static mirroring, one controller binding fix, and jar patch were tightly coupled to the same demo flow.`
  - `implementation`: `Fallback responses now render neutral fallback result cards, use Fallback 근거 instead of GPT 분석, and the summary separates 요청 엔진 from 실제 처리 so GPT/local offline fallback no longer looks like live model analysis.`
  - `controller_fix`: `RecommendationController now declares @RequestParam(name = "limit", defaultValue = "10") for poster-seed so the endpoint does not depend on Java parameter-name metadata in manually patched jars.`
  - `static_runtime`: `frontend AI JS/CSS/HTML were mirrored into backend static resources, backend build resources, and the current boot jar; RecommendationController.class was recompiled and patched into the boot jar; Spring was restarted from the patched jar as PID 13168 on 127.0.0.1:8080.`
  - `verification`: `node --check passed for frontend/src/js/pages/daboyeoAi.js; javac cleanly recompiled RecommendationController.class with a minimal Spring classpath outside the sandbox after sandbox Gradle-cache access noise; /api/health returned ok; /api/recommendation/poster-seed?limit=12 returned 12 items; 8080 static HTML/JS/CSS contain 20260429-fallback-result, is-fallback-result, Fallback 근거, 요청 엔진, 실제 처리, and fallback CSS markers; POST /api/recommendations with aiProvider=gpt returned status fallback, model gpt-5.5, and 3 recommendations; git diff --check passed with CRLF warnings only.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification verification exposed one backend binding durability bug inside the same AI flow; reviewer_findings fallback UI is now honest about actual processing and poster-seed is no longer compile-flag fragile; verification_outcome demo server is running and the GPT-offline path is presentable; next_gate_adjustment explicitly name RequestParam values on controller methods whenever manual javac/jar patching is part of the workflow.`

- ai_provider_health_visibility_for_demo:
  - `timestamp`: `2026-04-29 14:18:00 +09:00`
  - `classification`: `score_total 5; full evaluation; single-session; no spawn because backend health response, frontend badge UI, and jar mirror were one small coupled contract.`
  - `implementation`: `Added GET /api/recommendation/providers/health returning local/GPT provider, label, expected models, availability, status, and safe message; frontend mode step now fetches it and shows checking/connected/offline badges on provider route buttons.`
  - `runtime`: `Spring was restarted from the patched jar as PID 5500 on 127.0.0.1:8080.`
  - `verification`: `node --check passed for frontend/src/js/api/client.js and frontend/src/js/pages/daboyeoAi.js; javac compiled RecommendationModels, LocalModelRecommendationClient, RecommendationService, and RecommendationController; /api/health returned ok; /api/recommendation/providers/health returned local and GPT as offline because 1234/10531 are not running; 8080 static HTML/JS/CSS contain provider-health markers; POST /api/recommendations still returns fallback with 3 recommendations; git diff --check passed with CRLF warnings only.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification no major reclassification, but jar patch had to be corrected for the new nested record class; reviewer_findings offline model routes are now visible instead of silently becoming fallback; verification_outcome demo can show why GPT/local live analysis is not connected; next_gate_adjustment wildcard nested Java classes must be explicitly included in manual jar patches.`

- recommendation_future_showtime_coverage_recovery:
  - `timestamp`: `2026-04-29 14:08:00 +09:00`
  - `classification`: `score_total 7; full evaluation; single-session; no spawn because DB coverage, collector dry-run, bounded ingest, and API verification were sequentially dependent.`
  - `db_before`: `Read-only JDBC coverage found 474 showtimes, max starts_at 2026-04-28 23:10:00, and 0 usable future candidates for the 2026-04-29 afternoon cutoff.`
  - `collector_dry_run`: `collect_all_to_tidb.py --provider all --all-provider-dates --max-provider-dates 1 confirmed provider date 2026-04-29 was available from Lotte and Megabox.`
  - `ingest`: `Lotte write was blocked by a JSONDecodeError from the provider response before useful upsert; Megabox bounded write inserted/upserted 160 today showtimes, 31 movies, 16 theaters, 160 screens, and 75 movie tags.`
  - `db_after`: `Coverage rose to 634 total showtimes, max starts_at 2026-04-29 22:25:00, and 155 usable future showtimes, all currently from MEGABOX.`
  - `implementation`: `RecommendationService now retries an expired date+timeRange filter without the expired timeRange, preserving date/region/person filters, and the result page displays response.message so the user sees the fallback reason.`
  - `static_runtime`: `frontend AI JS/HTML were mirrored into backend static resources, backend build resources, and the current boot jar; Spring was restarted from the patched jar as PID 6192 on 127.0.0.1:8080.`
  - `verification`: `node --check passed for daboyeoAi.js; javac compiled RecommendationService and RecommendationServiceCandidateFilterTests after elevated Gradle-cache access; 8080 health returned ok; 8080 HTML contains 20260429-filter-relax and JS contains resultNote; POST /api/recommendations with gpt fast and today morning now returns status fallback with 3 recommendations and the expired-timeRange message; git diff --check passed with CRLF warnings only.`
  - `retrospective`: `evaluation_fit full fit; orchestration_fit single-session fit; predicted_topology single-session; actual_topology single-session; spawn_count 0; rework_or_reclassification discovery became a bounded data refresh plus backend guard; reviewer_findings data freshness was the real blocker and today morning needed a durability guard; verification_outcome API moved from no_usable_showtimes/no_filtered_candidates to candidate-backed fallback; next_gate_adjustment check same-day expired filters whenever recommendations depend on current time.`

- gpt_recommendation_prompt_depth_differentiation:
  - `timestamp`: `2026-04-29 13:40:00 +09:00`
  - `classification`: `score_total 6; full evaluation; single-session; no spawn because provider-mode prompt contract, response shaping, result UI, and jar mirror are tightly coupled.`
  - `implementation`: `GPT fast/precise now use provider-specific prompts, wider GPT JSON fields (why/a/v/c), GPT candidate/token budgets, caution parsing, narrative preservation, and GPT analysis/caution result-card styling while local Gemma keeps compact tag-oriented behavior.`
  - `config`: `Added GPT-specific candidate and max-token environment knobs: GPT fast 6 candidates/520 tokens, GPT precise 8 candidates/900 tokens by default.`
  - `static_runtime`: `frontend AI JS/CSS/HTML and application.yml/classes were mirrored into backend static resources, backend build resources/classes, and the current boot jar; Spring is running as PID 12836 on 127.0.0.1:8080.`
  - `verification`: `node --check passed for daboyeoAi.js; Gradle test could not start because native-platform.dll failed; javac compiled RecommendationModels, RecommendationProperties, LocalModelRecommendationClient, and RecommendationService; 8080 health returned ok; 8080 static JS/CSS/HTML contain gpt-depth result UI markers; GPT recommendation POST accepted aiProvider=gpt and model=gpt-5.5 but returned no_usable_showtimes because the current DB has no usable future showtime candidates; git diff --check passed with CRLF warnings only.`

- ai_poster_image_failure_fallback:
  - `timestamp`: `2026-04-29 12:39:00 +09:00`
  - `classification`: `score_total 2; light evaluation; single-session; no spawn because this was a narrow frontend image-error fallback plus Spring static mirror.`
  - `root_cause`: `poster seed currently returns external posterUrl values and posterPath null, and the old img error handler only removed src, leaving a blank framed card.`
  - `implementation`: `renderPosterCard now always renders a styled ai-poster-image-fallback title panel and switches to it when posterUrl is absent or the img error event fires.`
  - `static_runtime`: `frontend AI JS/CSS/HTML were mirrored into backend static resources, backend build resources, and the current boot jar; Spring was restarted as PID 9768 on 127.0.0.1:8080.`
  - `verification`: `node --check passed for daboyeoAi.js; 8080 health returned ok; 8080 HTML/JS/CSS contain 20260429-poster-fallback, ai-poster-image-fallback, and is-image-missing rules; git diff --check passed with CRLF warnings only.`

- ai_poster_card_frame_polish:
  - `timestamp`: `2026-04-29 12:31:00 +09:00`
  - `classification`: `score_total 2; light evaluation; single-session; no spawn because this was a narrow CSS poster-frame polish plus Spring static mirror.`
  - `implementation`: `Poster cards now use a dark padded cinematic frame, inner inset highlight, subtler hover lift, and cyan selected state instead of the flat pale border/background.`
  - `static_runtime`: `frontend AI CSS/HTML were mirrored into backend static resources, backend build resources, and the current boot jar; Spring was restarted as PID 20664 on 127.0.0.1:8080.`
  - `verification`: `8080 health returned ok; 8080 HTML/CSS contains 20260429-poster-frame and the poster-card frame rules; app browser reached the poster step and showed the updated framed poster cards; git diff --check passed with CRLF warnings only.`

- ai_first_step_back_button:
  - `timestamp`: `2026-04-29 12:22:00 +09:00`
  - `classification`: `score_total 2; light evaluation; single-session; no spawn because this was a narrow JS navigation hotfix plus Spring static mirror.`
  - `implementation`: `First-step 이전 now calls browser history back with a main-page fallback, while later AI guide steps keep the existing internal previous-step behavior.`
  - `static_runtime`: `frontend AI JS/HTML were mirrored into backend static resources, backend build resources, and the current boot jar; Spring was restarted as PID 13016 on 127.0.0.1:8080.`
  - `verification`: `node --check passed for daboyeoAi.js; /api/health returned ok; 8080 HTML/JS contains 20260429-first-back and goToPreviousPage; app browser confirmed first-step 이전 returns to index history and second-step 이전 returns to the audience step; git diff --check passed with CRLF warnings only.`

- ai_provider_route_selector:
  - `timestamp`: `2026-04-29 11:40:00 +09:00`
  - `classification`: `score_total 5; full evaluation; single-session; no spawn because the provider selector UI, aiProvider request contract, backend routing, and Spring static mirror form one tight slice.`
  - `implementation`: `The mode step now renders a local/GPT route selector, stores the selected provider as daboyeoAiProvider, updates fast/precise card model labels between Gemma local and GPT-5.5 reasoning labels, and sends aiProvider with recommendation requests.`
  - `backend`: `RecommendationRequest accepts aiProvider, AiProvider defaults to local, RecommendationProperties exposes local/GPT base URL, model, and reasoning settings, and LocalModelRecommendationClient routes OpenAI-compatible calls by provider without exposing OAuth tokens to the browser.`
  - `startup_fix`: `The added RecommendationProperties compatibility constructor required @ConstructorBinding on the canonical record constructor; without it, the patched jar exited because Spring looked for a default constructor.`
  - `static_runtime`: `frontend AI files were mirrored into backend/src/main/resources/static, backend/build/resources/main/static, and the current boot jar; Spring is running from the patched jar as PID 13160 on 127.0.0.1:8080.`
  - `verification`: `node --check passed for frontend/src/js/pages/daboyeoAi.js; javac 21 compiled the changed recommendation classes; Gradle test remains blocked by native-platform.dll; /api/health returned status ok; POST /api/recommendations accepted aiProvider=local and aiProvider=gpt with models gemma-4-e2b-it and gpt-5.5; app browser confirmed GPT switch changes the card labels to GPT-5.5 reasoning low/high; git diff --check passed with CRLF warnings only.`

- durable_spring_tidb_dotenv_mapping:
  - `timestamp`: `2026-04-29 11:10:00 +09:00`
  - `classification`: `score_total 6; full evaluation; single-session; no spawn because dotenv parsing, Spring property-source precedence, jar startup, and API recovery were one tight config slice.`
  - `root_cause`: `The root .env first key was read as a BOM-prefixed key instead of TIDB_HOST, and the current boot jar also lacked BOOT-INF/classes/META-INF/spring.factories, so the EnvironmentPostProcessor was not registered in that jar.`
  - `implementation`: `RootDotenvLoader now strips a UTF-8 BOM, RootDotenvEnvironmentPostProcessor derives DABOYEO_DB_* plus spring.datasource.* from TIDB_* with TiDB Cloud TLS and timeout defaults, and docs/env example explain the durable mapping.`
  - `verification`: `Gradle test could not start because native-platform.dll failed to load; javac 21 compiled the changed config classes; manual Java config harness returned MANUAL_CONFIG_CHECK_OK; the patched boot jar includes RootDotenv classes and BOOT-INF/classes/META-INF/spring.factories; /api/health returned 200; POST /api/recommendation/sessions returned 200 in 2142 ms.`
  - `runtime`: `Spring backend is running from the normal jar as PID 2112 on 127.0.0.1:8080.`

- korea_boxoffice_top50_poster_webp_seed:
  - `timestamp`: `2026-04-28 16:23:00 +09:00`
  - `classification`: `score_total 8; full evaluation; single-session; no spawn because ranking, poster lookup, conversion, and manifest verification were one coupled generated-data pipeline.`
  - `source`: `KOBIS official former/all-time box-office HTML saved locally at .local/boxoffice-posters/kobis-former-boxoffice.html; filtered to releaseDate 2010-01-01 through 2026-12-31 and re-ranked 1-50 by admissions.`
  - `output_metadata`: `backend/src/main/resources/recommendation/korea-boxoffice-top50-posters.json contains 50 movies with rank, KOBIS all-time rank, movieCd, Korean title, release date, admissions, gross, screens, poster source URL, local poster path, dimensions, and sha256.`
  - `output_assets`: `frontend/src/assets/R2/posters contains 50 generated .webp poster files referenced by the metadata.`
  - `poster_source`: `each poster was resolved from the KOBIS movie detail popup endpoint /kobis/business/mast/mvie/searchMovieDtl.do using the movieCd from the official ranking row, then converted locally to WEBP with Pillow quality=86.`
  - `verification`: `python -m json.tool passed for korea-boxoffice-top50-posters.json; manifest check found 50 movies, 50 unique ranks, and 0 missing .webp files; poster directory has 50 .webp files; git diff --check passed with existing CRLF warnings only.`
  - `data_note`: `KOBIS cumulative admissions can change due to rereleases and ongoing 2026 releases, so the ranking is a 2026-04-28 local snapshot.`

- canonical_ai_recommendation_smoke:
  - `frontend_route`: `frontend/src/js/pages/script.js now routes the main AI 추천받기 CTA to ./src/pages/daboyeoAi.html; other AI links already point to daboyeoAi.html.`
  - `duplicate_page`: `frontend/src/pages/ai.html still exists pending explicit action-time confirmation before local file deletion.`
  - `model_config`: `application.yml keeps fast-model=gemma-4-e2b-it and precise-model=gemma-4-e4b-it; frontend mode labels still show E2B Q4 and E4B Q4.`
  - `local_model_endpoint`: `http://127.0.0.1:1234/v1/models was initially unreachable; LM Studio was started with lms server start and now returns the configured model list.`
  - `local_model_loaded`: `LM Studio server is now running on 127.0.0.1:1234; gemma-4-e2b-it and gemma-4-e4b-it are both listed by /v1/models, loaded by lms ps, and returned HTTP 200 from /v1/chat/completions with a ping response.`
  - `spring_backend`: `Gradle cannot start because native-platform.dll loading fails, but the existing build/libs jar starts with Java 21 when launched in a sandbox-external hidden PowerShell process.`
  - `health`: `/api/health returned status=ok from localhost:8080 after the jar launch.`
  - `recommendation_api`: `/api/recommendation/poster-seed?limit=3 returned 200, but POST /api/recommendation/sessions timed out after 8 seconds; the frontend therefore still falls back to local preview mode.`
  - `browser`: `App browser opened http://localhost:5500/src/pages/daboyeoAi.html?v=backend-live; the visible page is the canonical AI recommendation flow at 1 / 5 상황.`
  - `verification`: `node --check passed for script.js and daboyeoAi.js; git diff --check passed with CRLF warnings only.`

- ai_browser_comment_polish:
  - `timestamp`: `2026-04-28 15:15:52 +09:00`
  - `classification`: `score_total 3; single-session; no spawn; browser comments are a narrow frontend rendering/layout polish on the canonical AI page.`
  - `scope`: `Remove option description text from glass buttons and move the poster batch button close to the poster grid with stronger visibility.`
  - `write_sets`: `frontend/src/pages/daboyeoAi.html, frontend/src/js/pages/daboyeoAi.js, frontend/src/css/daboyeoAi.css`
  - `verification_target`: `node --check daboyeoAi.js, git diff --check, and browser DOM/style sanity for the commented elements.`
  - `implementation`: `Option buttons now render only their title span; poster batch navigation is inside .ai-poster-stage as .ai-poster-next-button, styled as a visible circular button over the poster grid; daboyeoAi.html cache-bust query was updated so the app browser loads the patched JS/CSS.`
  - `verification`: `node --check passed for daboyeoAi.js; git diff --check passed with CRLF warnings only; app browser confirmed .ai-glass-btn-desc count 0, old selected description text count 0, .ai-poster-right-top count 0, .ai-poster-next-button count 1 and visible.`

- ai_single_click_option_flow:
  - `timestamp`: `2026-04-28 15:32:01 +09:00`
  - `classification`: `score_total 3; single-session; no spawn; one narrow interaction fix on the AI option buttons.`
  - `scope`: `Remove the mobile first-click expansion gate so option buttons advance on one click, while keeping the expanded visual affordance as hover/focus styling.`
  - `write_sets`: `frontend/src/pages/daboyeoAi.html, frontend/src/js/pages/daboyeoAi.js, frontend/src/css/daboyeoAi.css`
  - `verification_target`: `node --check daboyeoAi.js, git diff --check, and browser one-click progression from audience to mood.`
  - `implementation`: `Removed the mobile innerWidth/is-expanded click gate from renderOptionList, deleted obsolete is-expanded CSS/mobile description rules, added focus-visible styling matching hover affordance, and bumped the AI page CSS/JS cache-bust query.`
  - `verification`: `node --check passed for daboyeoAi.js; git diff --check passed with CRLF warnings only; static search found no is-expanded or mobile innerWidth gate; app browser confirmed one click changes from audience to mood, with desc count 0 and expanded count 0.`

- ai_poster_bidirectional_buttons:
  - `timestamp`: `2026-04-28 15:41:31 +09:00`
  - `classification`: `score_total 3; single-session; no spawn; narrow poster-step control placement change.`
  - `scope`: `Move the next poster batch button to the comment-ping position at the right side of the poster grid and add a matching previous button on the opposite side.`
  - `write_sets`: `frontend/src/pages/daboyeoAi.html, frontend/src/js/pages/daboyeoAi.js, frontend/src/css/daboyeoAi.css`
  - `verification_target`: `node --check daboyeoAi.js, git diff --check, and browser DOM sanity for left/right poster batch controls.`
  - `implementation`: `Poster stage now renders previous and next batch buttons; next sits to the right of the grid, previous sits to the opposite left, with responsive fallback inside the grid area on narrow viewports.`
  - `verification`: `node --check passed for daboyeoAi.js; git diff --check passed with CRLF warnings only; app browser confirmed .ai-poster-nav-button count 2, prev count 1, next count 1, old top control count 0, and both controls visible.`

- ai_poster_exact_five_manual_continue:
  - `timestamp`: `2026-04-28 15:49:51 +09:00`
  - `classification`: `score_total 3; single-session; no spawn; narrow poster-step interaction logic change.`
  - `scope`: `Disable poster completion until exactly five posters are selected and prevent automatic transition when the fifth poster is selected.`
  - `write_sets`: `frontend/src/pages/daboyeoAi.html, frontend/src/js/pages/daboyeoAi.js, frontend/src/css/daboyeoAi.css`
  - `verification_target`: `node --check daboyeoAi.js, git diff --check, and browser poster-step sanity for disabled-before-five and enabled-at-five behavior.`
  - `implementation`: `Removed poster auto-advance scheduling, changed completion readiness from minimum 3 to exactly 5 selected posters, set the poster completion button disabled/aria-disabled until ready, added disabled CTA styling, and bumped daboyeoAi.html cache-bust query.`
  - `verification`: `node --check passed for daboyeoAi.js; git diff --check passed with CRLF warnings only; static search found no MIN_LIKE_COUNT, POSTER_AUTO_ADVANCE_MS, schedulePosterProgress, or isPosterDiagnosisComplete; app browser confirmed button disabled at 0/3/4 selected, enabled at 5 selected, and the page remains on the poster step after the fifth selection.`

- local_r2_poster_folder_placeholder:
  - `timestamp`: `2026-04-28 15:57:16 +09:00`
  - `classification`: `score_total 2; single-session; no spawn; placeholder folder only after user corrected scope.`
  - `scope`: `Create only a local R2-like poster folder placeholder; do not keep downloaded or generated poster assets yet.`
  - `write_sets`: `frontend/src/assets/R2/posters/.gitkeep, backend/src/main/resources/recommendation/poster-seed.json`
  - `implementation`: `Removed 53 generated/downloaded poster image files from frontend/src/assets/R2/posters, restored poster-seed.json to the previous external posterUrl state from the existing backend bin resource copy, and left frontend/src/assets/R2/posters/.gitkeep so the folder exists in git.`
  - `verification_target`: `poster folder exists with only .gitkeep; poster-seed.json parses and no longer points at /src/assets/R2/posters.`

- ai_remove_local_preview_fallbacks:
  - `timestamp`: `2026-04-28 16:24:56 +09:00`
  - `classification`: `score_total 4; single-session; no spawn; frontend recommendation flow cleanup after real poster assets were added.`
  - `scope`: `Remove local preview poster and recommendation fallbacks so the AI flow depends on real poster seed/API responses instead of generated picsum preview data.`
  - `write_sets`: `frontend/src/js/pages/daboyeoAi.js, frontend/src/pages/daboyeoAi.html`
  - `verification_target`: `node --check daboyeoAi.js, static search confirms no preview/picsum fallback path, and poster API failure paths show errors instead of synthetic preview data.`
  - `implementation`: `Removed local preview session ids, generated picsum poster seeds, local preview recommendation response generation, preview feedback bypass, and preview fallback branches; poster API failures now render the poster error state and recommendation failures render the recommendation error state.`
  - `verification`: `node --check passed for daboyeoAi.js; git diff --check passed with CRLF warnings only; static search found no preview fallback code in daboyeoAi.js, with only the no-preview cache-bust token remaining in daboyeoAi.html.`

- backend_static_frontend_mirror:
  - `timestamp`: `2026-04-28 16:37:00 +09:00`
  - `classification`: `score_total 5; single-session; no spawn; frontend static mirror into Spring resources and immediate local backend viewing.`
  - `scope`: `Mirror the current frontend static tree into Spring static resources so the AI page can be opened from localhost:8080 instead of the frontend-only 5500 server.`
  - `write_sets`: `backend/src/main/resources/static/**, backend/build/resources/main/static/**, backend/build/libs/daboyeo-backend-0.1.0-SNAPSHOT.jar`
  - `verification_target`: `localhost:8080/ and localhost:8080/src/pages/daboyeoAi.html serve the mirrored frontend after backend restart; API health still returns ok.`
  - `verification`: `Spring startup from the existing boot jar takes about 13 seconds; launching it outside the sandbox keeps the process alive. 127.0.0.1:8080/api/health returned status=ok, / returned 200, /src/pages/daboyeoAi.html returned 200, and the app browser opened http://127.0.0.1:8080/src/pages/daboyeoAi.html?v=backend-static-live3 with title DABOYEO AI 영화 취향 가이드.`

- ai_backend_same_origin_and_map_fallback:
  - `timestamp`: `2026-04-28 17:20:00 +09:00`
  - `classification`: `score_total 5; single-session; no spawn; runtime bugfix across same-origin API calls, Spring static mirror, and movie theater map fallback.`
  - `scope`: `Fix AI recommendation Failed to fetch from the 127.0.0.1:8080 app browser page and keep movieTheaterMap usable when Kakao/geolocation fails.`
  - `write_sets`: `frontend/src/js/api/client.js, frontend/src/pages/daboyeoAi.html, frontend/src/pages/movieTheaterMap.html, frontend/src/js/pages/movieTheaterMap.js, frontend/src/css/movieTheaterMap.css, backend/src/main/resources/static/**, backend/build/resources/main/static/**, backend/build/libs/daboyeo-backend-0.1.0-SNAPSHOT.jar`
  - `implementation`: `API client now uses window.location.origin when served from port 8080, avoiding localhost vs 127.0.0.1 CORS mismatch; movieTheaterMap legacy inline logic was removed and replaced with a standalone fallback-safe script and styled result cards; static resources and the boot jar were refreshed.`
  - `runtime`: `Spring is running as PID 6892 with TiDB mapped from local .env to DABOYEO_DB_* plus connectTimeout/socketTimeout=8000; secrets were not printed.`
  - `verification`: `JDBC probe reached TiDB and counted recommendation_profiles; POST /api/recommendation/sessions now returns 200 in about 1.3s instead of 30s 503; /src/pages/daboyeoAi.html and /src/pages/movieTheaterMap.html return 200 with updated assets; node --check passed for client.js and movieTheaterMap.js; git diff --check passed with CRLF warnings only; app browser confirmed AI page loaded and no Failed to fetch log was produced on the refreshed AI page.`

- provider_future_date_showtime_ingest:
  - `timestamp`: `2026-04-28 17:38:00 +09:00`
  - `classification`: `score_total 7; single-session; no spawn; external provider date discovery and TiDB write verification were one coupled ingest loop.`
  - `scope`: `Add all-provider future-date ingest support and run a bounded 3-date ingest to restore AI recommendation candidates.`
  - `write_sets`: `scripts/ingest/collect_all_to_tidb.py, STATE.md`
  - `implementation`: `collect_all_to_tidb.py now supports --all-provider-dates, --max-provider-dates, and --megabox-date-days; Lotte uses provider-exposed future play dates, while Megabox uses a bounded future playDe range because the API path is date-param based.`
  - `dry_run`: `python scripts/ingest/collect_all_to_tidb.py --provider all --all-provider-dates --max-provider-dates 3 --dry-run selected Lotte dates 2026-04-28..2026-04-30 from 28 provider dates and Megabox dates 20260428..20260430.`
  - `runtime`: `PyMySQL was installed after user approval; sandbox Python could not see Roaming site-packages, so the ingest ran with sandbox-external Python. Secrets were not printed.`
  - `ingest_result`: `Bounded run --provider all --all-provider-dates --max-provider-dates 3 --limit-movies 8 --limit-theaters 8 --limit-schedules 80 inserted/upserted 80 Lotte showtimes and 80 Megabox showtimes.`
  - `verification`: `DB showtimes increased to 474 total, current/future starts_at rows increased to 160, max starts_at is 2026-04-28 23:10:00, and POST /api/recommendations returned 3 recommendation items instead of the empty candidate response; python py_compile passed and git diff --check passed with CRLF warnings only.`

- fresh_three_provider_crawl:
  - `output_dir`: `.local/api-responses/fresh-all-20260427-174156`
  - `CGV`: `movies=59, attributes=13, regions=9, sites=177, dates=7, schedules=13, seats=144; sample=CGV 강남 / 악마는 프라다를 입는다 2 / 20260429 / 1관 (Laser) / 2400-2609`
  - `LOTTE_CINEMA`: `movies=43, cinemas=239, play_dates=28, schedules=2, seats=142; sample=가산디지털 / 왕과 사는 남자 / 2026-04-27 / 2관 / 22:10-24:17`
  - `MEGABOX`: `movies=68, area_branches=116, schedules=240, seats=116; sample=강남 / 살목지 / 20260427 / 르 리클라이너 1관 / 18:20-20:05`
  - `classification`: `classification_summary.json separates common movies/theaters/screens/showtimes/seat snapshot fields from provider-specific raw/meta fields and candidate separate tables: collection_runs, canonical_movies/movie_provider_links, seat_layouts, provider_status_codes, raw payload archive index, showtime_price_options.`
  - `notable_data_shape`: `CGV and Lotte can return post-midnight raw times such as 2400-2609 or 24:17, so ingest must normalize starts_at/ends_at across date boundaries while preserving start_time_raw/end_time_raw.`
  - `security`: `CGV_API_SECRET was used from local environment/.env path and was not printed.`
  - `errors`: `none`

- team_db_setup_doc:
  - `docs/TEAM_DB_SETUP.md`: `added a team guide for per-person DB names with shared db/migrations schema alignment, canonical internal table names, placeholder-only .env examples, setup commands, schema-change rules, data-vs-schema distinction, security notes, and a short team-share summary.`
  - `table_names`: `documented the 20 canonical tables expected by the backend, collectors, and verification scripts: schema_migrations, providers, collection_runs, movies, canonical_movies, movie_provider_links, theaters, screens, showtimes, showtime_prices, seat_layouts, seat_layout_items, seat_snapshots, seat_snapshot_items, provider_status_codes, provider_raw_payloads, movie_tags, recommendation_profiles, recommendation_runs, recommendation_feedback.`
  - `table_grouping`: `clarified that provider-specific data is not stored in cgv_*, lotte_*, or megabox_* tables; shared tables use provider_code/external_* keys and raw_json/provider_meta_json for provider-specific payloads.`
  - `security`: `checked the new doc for password/secret/token/host patterns; only placeholders such as your-db-host, your-db-user, and your-db-password are present.`
  - `verification`: `static table-name and grouping search found all canonical table names plus the common/provider-specific clarification in the guide; scripts/verify/verify_tidb_ingest.py now checks the same 20-table contract and required migrations 001-005.`

- team_db_contract_finalization:
  - `db/migrations/005_collection_contract_extensions.sql`: `adds collection_runs, canonical_movies, movie_provider_links, seat_layouts, seat_layout_items, provider_status_codes, and provider_raw_payloads, then records schema_migrations version 005.`
  - `provider_status_seed`: `seeds observed seat mappings from the fresh crawl: CGV 00=available, CGV 01=sold, Lotte 0=sold, Lotte 50=available, Megabox GERN_SELL=available, Megabox SCT04=unavailable.`
  - `collectors/common/normalize.py`: `updated Lotte seat status normalization to match the fresh sample where 50 matched the 2 remaining seats and 0 matched the 140 non-remaining seats.`
  - `db/SCHEMA_CONTRACT.md`: `documents migration 005 roles, stable keys, midnight time rules, provider status seed evidence, seat layout separation, and raw payload constraints.`
  - `scripts/verify/verify_tidb_ingest.py`: `now fails fast when required schema tables or migrations 001-005 are missing before printing ingest table counts.`
  - `java_ingest_time_rule`: `CollectorBundleIngestCommand now preserves start_time_raw/end_time_raw while normalizing 2400, 2609, and 24:17 into cross-date starts_at/ends_at values for CGV/Lotte/Megabox bundles.`
  - `verification`: `py_compile passed for normalize.py and verify_tidb_ingest.py; normalize assertions passed for CGV/Lotte/Megabox observed status codes; migration dry-run listed 001-005; migration 005 split into 10 SQL statements; static contract search found all 20 table names in docs and verifier; security placeholder scan found no concrete secrets; git diff --check passed with CRLF warnings only.`
  - `gradle_gap`: `gradle test --tests kr.daboyeo.backend.ingest.CollectorBundleIngestCommandTests could not start because local Gradle failed to load native-platform.dll; logged in ERROR_LOG.md as an environment/runtime issue.`

- frontend_page_tree_normalization:
  - `moved`: `frontend/movies.html and all frontend/src/basic/*.html pages now live under frontend/src/pages/.`
  - `routes`: `index.html CTAs, components/header.html nav links, and script.js route constants now point at src/pages paths.`
  - `paths`: `moved movies/map/AI/seat/price pages now use ../css, ../js, ../assets, and ../../favicon paths from src/pages.`
  - `cleanup`: `removed the now-empty frontend/src/basic directory and ignored/generated http-server-e2e log files from the working tree.`
  - `deferred`: `direct-comparison location/person-count semantics and 17:00~02:00 handling remain intentionally unresolved per user instruction.`
  - `verification`: `node --check passed for script.js, daboyeoAi.js, seatRecommendMbti.js, and liveMovies.js; py_compile passed for map helper scripts; static HTML local-link checker found 0 missing local href/src targets across 16 HTML files; stale src/basic route search returned no active source hits; git diff --check passed with CRLF warnings only.`

- mbti_map_overlay_cleanup:
  - `frontend/src/css/seatRecommendMbti.css`: `removed .theater-map::before, which was drawing the oval guide curve and two translucent vertical overlay columns inside the seat map.`
  - `frontend/src/basic/seatRecommendMbti.html`: `cache-busted the MBTI seat page stylesheet query to 20260427-map-cleanup.`
  - `verification`: `static search found no remaining theater-map overlay selector or old column-gradient markers; git diff --check passed with CRLF warnings only.`

- kmh_kakao_map_restore:
  - `source`: `imported frontend/src/css/kakaoMap.css, frontend/src/map/theaters.json, map/location_service.py, and map/populate_theaters.py from origin/kmh with git archive to preserve file bytes.`
  - `frontend/movies.html`: `restored the ./src/css/kakaoMap.css stylesheet link before movies.css.`
  - `security`: `replaced the hardcoded Kakao REST API key in map/populate_theaters.py with KAKAO_REST_API_KEY environment-variable loading.`
  - `verification`: `python -m py_compile passed for the restored map helper scripts; imported byte hashes matched origin/kmh before the secret-removal patch; git diff --check passed with CRLF warnings only.`

- direct_compare_cta_routing:
  - `frontend/index.html`: `changed 직접 비교하기 from a button to an anchor with ./movies.html fallback href.`
  - `frontend/src/js/pages/script.js`: `nearbyBtn now saves the current search context and navigates to movies.html with region, date, timeStart, timeEnd, and personCount query parameters.`
  - `frontend/src/css/style.css`: `kept the anchor styled like the existing 직접 비교하기 button with no underline and pointer affordance.`
  - `verification`: `node --check passed for script.js; git diff --check passed with CRLF warnings only.`

- location_cta_routing:
  - `frontend/index.html`: `changed 내 위치 from an unlinked button to an anchor pointing at ./src/basic/movieTheaterMap.html.`
  - `frontend/src/css/style.css`: `updated .btn-map so the anchor keeps the previous button-like alignment, color, and pointer behavior.`
  - `verification`: `node --check passed for script.js; git diff --check passed with CRLF warnings only.`

- location_cta_and_map_logo_polish:
  - `frontend/src/css/style.css`: `set .btn-map to a fixed 52px button-height flex anchor with stable padding, line-height, no underline, and nowrap text.`
  - `frontend/src/basic/movieTheaterMap.html`: `changed header and footer logo image paths from /assets/logo.svg to /src/assets/logo.svg?v=20260427-logo and linked logos back to ../../index.html.`
  - `frontend/index.html`: `cache-busted the 내 위치 map-page link to ./src/basic/movieTheaterMap.html?v=20260427-logo so the app browser reloads the updated map page.`
  - `verification`: `confirmed http://localhost:5500/src/assets/logo.svg?v=20260427-logo returns 200; node --check passed for script.js; git diff --check passed with CRLF warnings only.`

- ai_result_actions_cleanup:
  - `frontend/src/js/pages/daboyeoAi.js`: `result-card actions now render 예매하기 and 좌석표보기 only; 끌려요/별로예요 feedback buttons were removed from the card renderer.`
  - `frontend/src/css/daboyeoAi.css`: `replaced obsolete feedback-button result styling with ai-seatmap-button secondary CTA styling.`
  - `frontend/src/basic/daboyeoAi.html`: `cache-busted the AI page CSS/JS query strings to 20260424-result-actions.`
  - `verification`: `node --check passed for daboyeoAi.js; git diff --check passed with CRLF warnings only; static search found no 예매보기/끌려요/별로예요/ai-feedback-button references in the AI JS/CSS targets.`

- kmh_selective_integration:
  - `source`: `fetched origin/kmh at 88cc169 after it advanced from 1434eee.`
  - `included`: `backend live movie API/controller/service/repository/domain/sync/ingest files, backend tests, db/sql schema docs, ingest PowerShell scripts, frontend liveMovies.js, frontend movies.html, and movies.css.`
  - `manual_merge`: `kept existing AI recommendation backend config/files while adding validation dependency, ingest Gradle tasks, root .env loader, scheduling, live sync config, and unified error response handling.`
  - `preserved`: `existing uncommitted AI result-card cleanup remained intact; origin/kmh stale branch-wide frontend/doc/tooling deletions were not accepted.`
  - `fix`: `restored CGV seat status normalization test strings from mojibake-like ???? to 판매완료/사용불가 and expanded normalizer Korean keywords.`
  - `verification`: `node --check passed for liveMovies.js and daboyeoAi.js; gradle test passed; git diff --check passed with CRLF warnings only.`

- kmh_utf8_restore:
  - `cause`: `the GitHub branch content was UTF-8-clean; the earlier local import likely corrupted Korean by streaming git diff through a PowerShell text pipeline.`
  - `restore_method`: `used git archive --output for origin/kmh and extracted the archive before copying files back, avoiding text-pipeline re-encoding.`
  - `scope`: `restored imported kmh frontend movies.html/liveMovies.js/movies.css plus newly added backend/db/script files from the archive; kept existing lsh AI cleanup and manual backend config merge.`
  - `verification`: `literal ?? search returned no matches across imported frontend/backend surfaces; node --check passed for liveMovies.js and daboyeoAi.js; gradle test passed; git diff --check passed with CRLF warnings only.`

- current_task:
  - `classification`: `reclassified from MBTI image exploration into concrete frontend implementation`
  - `workspace_status`: `existing dirty worktree is limited to prior STATE.md and MULTI_AGENT_LOG.md edits before implementation starts`
  - `main_page_sources`: `frontend/index.html, frontend/src/css/style.css, frontend/src/css/common.css, frontend/src/css/daboyeoAi.css`
  - `main_page_style_findings`: `dark cinema background image, black base canvas, purple and lilac accents, Pretendard-heavy sans-serif typography, glassy cards with thin borders, gradient CTAs, icon-led feature cards`
  - `seat_section_baseline`: `the current home page already exposes a four-card seat-recommendation teaser with MBTI, couple, group, and random seating hooks`
  - `implementation_target`: `frontend/src/basic/seatRecommendMbti.html, frontend/src/css/seatRecommendMbti.css, frontend/src/js/pages/seatRecommendMbti.js, frontend/index.html, frontend/src/css/style.css, and frontend/src/js/pages/script.js`
  - `visual_contract`: `headerless page, compact back/context line, cinematic theater background, floating MBTI 4x4 selector, INTJ-style result panel, glowing seat-map recommendations, one CTA`
- implementation:
  - `frontend/src/basic/seatRecommendMbti.html`: `new headerless MBTI별 좌석 추천 page added`
  - `frontend/src/css/seatRecommendMbti.css`: `added a dark cinematic seat-recommendation visual system with responsive MBTI grid, result cards, and theater seat map`
  - `frontend/src/js/pages/seatRecommendMbti.js`: `added static MBTI profile selection logic with 16 profiles, trait/reason rendering, random selection, and highlighted seat zones`
  - `frontend/index.html`: `main seat section copy and MBTI/CTA entry points now route into the implemented page`
  - `frontend/src/js/pages/script.js`: `data-seat-flow click and keyboard routing added for the main seat section`
  - `frontend/src/css/style.css`: `clickable seat-flow cards get pointer affordance`
  - `frontend/src/basic/daboyeoAi.html, frontend/src/css/daboyeoAi.css, frontend/src/js/pages/daboyeoAi.js`: `restored to HEAD-equivalent content after route separation so the existing AI 추천 page remains preserved`
- verification:
  - `node --check frontend\src\js\pages\seatRecommendMbti.js`: `passed`
  - `node --check frontend\src\js\pages\script.js`: `passed`
  - `node --check frontend\src\js\pages\daboyeoAi.js`: `passed after route preservation`
  - `git diff --check`: `passed; CRLF normalization warnings only`
  - `git status --short`: `expected modified main/style/script/state files plus new seatRecommendMbti page assets`
- refinement:
  - `data_fidelity`: `No credible public dataset for exact MBTI별 영화관 좌석 선호 퍼센트를 found; the UI now labels seat percentages as computed 성향 적합도 instead of real measured preference data.`
  - `subagents`: `Sartre returned read-only mock-fidelity gaps; Helmholtz created frontend/src/assets/seat-mbti-sprite.svg.`
  - `frontend/src/basic/seatRecommendMbti.html`: `restructured into mock-like left selector/map and right recommendation rail with profile art, keyword box, lower metrics, disclaimer, and bottom CTA.`
  - `frontend/src/css/seatRecommendMbti.css`: `rebuilt visual system for denser MBTI cards, stronger purple glow, right rail spacing, curved/perspective theater map, zone labels, and per-seat score states.`
  - `frontend/src/js/pages/seatRecommendMbti.js`: `added sprite icons, fit metrics, 10x16 per-seat computed percentages, zone labels, and honest score rendering.`
  - `browser_verification`: `in-app browser loaded http://localhost:5500/src/basic/seatRecommendMbti.html?flow=mbti; rendered 160 seat cells, 160 percentage labels, 16 card icons, 3 lower metrics, and 1 score disclaimer.`
  - `final_checks`: `node --check passed for seatRecommendMbti.js and script.js; git diff --check passed with CRLF warnings only.`
- refinement_followup:
  - `frontend/src/basic/seatRecommendMbti.html`: `moved the page into a top hero, equal-height MBTI/result row, and full-width theater-map section; removed the separate lower note box so the map owns the lower space.`
  - `frontend/src/assets/seat-mbti-sprite.svg`: `added 16 dedicated mbti-* SVG symbols with transparent backgrounds for the card/profile icons.`
  - `frontend/src/js/pages/seatRecommendMbti.js`: `mapped every MBTI to a dedicated sprite, removed mbti-card-zone rendering, synced the profile icon, and added explicit aisle grid columns for wider seat spacing.`
  - `frontend/src/css/seatRecommendMbti.css`: `removed old card-zone styling, enlarged card icons, compacted the result panel to match the selector row, and expanded the theater map to an 18-column full-width grid.`
  - `verification`: `node --check passed for seatRecommendMbti.js and script.js; SVG parsed as XML with 31 symbols; 16 MBTI mappings all resolve to sprite IDs; git diff --check passed with CRLF warnings only.`
  - `browser_note`: `localhost served the updated HTML/JS with seat-main-row and without mbti-card-zone rendering; the existing in-app tab snapshot stayed on the previous DOM despite reload attempts, so the user may need a hard refresh to see the latest assets in that tab.`
- remove_old_svg:
  - `frontend/src/js/pages/seatRecommendMbti.js`: `removed the remaining seat-mbti-sprite.svg dependency and replaced metric-chip SVG uses with lightweight text markers.`
  - `frontend/src/css/seatRecommendMbti.css`: `replaced metric SVG sizing with .fit-metric-icon styling so the lower chips no longer depend on the old sprite.`
  - `frontend/src/assets/seat-mbti-sprite.svg`: `deleted after the user explicitly requested removing the old SVG.`
  - `verification`: `node --check passed for seatRecommendMbti.js; Select-String found no remaining seat-mbti-sprite.svg or old icon-id references under frontend/src; git diff --check passed with CRLF warnings only.`
- browser_comment_polish:
  - `frontend/src/basic/seatRecommendMbti.html`: `forced the hero title into two intentional lines and added left/right side-focus map labels.`
  - `frontend/src/css/seatRecommendMbti.css`: `changed title highlighting to em tags, added map label positions for front/rear/aisle/group/side, and softened generated PNG sprite rendering.`
  - `frontend/src/js/pages/seatRecommendMbti.js`: `dynamic map labels now follow the selected zone position; B/C rows are marked as front in aria/title while lower rows are marked as rear.`
  - `frontend/src/assets/seat-mbti-sprite-gpt.png`: `resampled from 1254x1254 to 1280x1280 so each sprite cell is an exact 320px square instead of a fractional 313.5px source cell.`
  - `verification`: `node --check passed for seatRecommendMbti.js; git diff --check passed with CRLF warnings only; static source checks found the forced title line, side labels, front label class, and no old SVG reference.`
- front_label_and_defringe:
  - `frontend/src/basic/seatRecommendMbti.html`: `added a default 전방 생동석 map label near the screen and cache-busted the page asset query.`
  - `frontend/src/css/seatRecommendMbti.css`: `styled the static front label with a warmer front-zone accent.`
  - `frontend/src/js/pages/seatRecommendMbti.js`: `added frontZoneStaticLabel and hide logic so the default front label does not duplicate dynamic front recommendations.`
  - `frontend/src/assets/seat-mbti-sprite-gpt.png`: `decontaminated transparent-edge RGB and reduced partial-alpha edge pixels from 61,858 to 45,373 while keeping the 1280x1280 exact 4x4 grid.`
  - `verification`: `node --check passed for seatRecommendMbti.js; git diff --check passed with CRLF warnings only; static source checks found frontZoneStaticLabel and no old SVG reference.`
- icon_micro_blur:
  - `frontend/src/css/seatRecommendMbti.css`: `added blur(0.28px) to MBTI card icons and blur(0.2px) to the larger selected-profile icon to soften remaining cutout-edge jaggies.`
  - `frontend/src/basic/seatRecommendMbti.html`: `cache-busted CSS/JS query strings to 20260424-icon-micro-blur.`
  - `verification`: `git diff --check passed with CRLF warnings only; static source checks found the micro-blur filters and no old SVG reference.`
- icon_blur_035:
  - `frontend/src/css/seatRecommendMbti.css`: `raised both generated PNG card/profile icon filters to blur(0.35px).`
  - `frontend/src/basic/seatRecommendMbti.html`: `cache-busted the CSS query string to 20260424-icon-blur035.`
  - `verification`: `git diff --check passed with CRLF warnings only; static source checks found two blur(0.35px) filters and no old SVG reference.`
- icon_blur_06:
  - `frontend/src/css/seatRecommendMbti.css`: `raised both generated PNG card/profile icon filters to blur(0.6px).`
  - `frontend/src/basic/seatRecommendMbti.html`: `cache-busted the CSS query string to 20260424-icon-blur06.`
  - `verification`: `git diff --check passed with CRLF warnings only; static source checks found two blur(0.6px) filters.`
- dynamic_zone_labels:
  - `frontend/src/basic/seatRecommendMbti.html`: `removed the always-visible frontZoneStaticLabel and cache-busted CSS/JS query strings to 20260424-dynamic-zone-labels.`
  - `frontend/src/css/seatRecommendMbti.css`: `removed the one-off warm .map-zone-label-static-front styling so front labels use the same recommendation-label visual system.`
  - `frontend/src/js/pages/seatRecommendMbti.js`: `removed frontZoneStaticLabel and hasDynamicFrontLabel duplicate-guard code because front labels now only appear through selected profile primary/secondary data.`
  - `verification`: `node --check passed for seatRecommendMbti.js; git diff --check passed with CRLF warnings only; static source checks found no frontZoneStaticLabel, static-front style, or hasDynamicFrontLabel references.`
- ksg_import:
  - `source`: `fetched origin/ksg at 78873c7 Top3 모두보기 만드는중.`
  - `included`: `frontend/src/assets/AIbackgroundImg.jpg, frontend/src/css/allMovies.css, frontend/src/css/dd.css, frontend/src/css/daboyeoAi.css, frontend/src/js/pages/daboyeoAi.js, frontend/src/pages/ai.html, frontend/src/pages/allMovies.html, frontend/src/pages/dd.html.`
  - `excluded`: `patch.js because it is a one-off helper script that rewrites daboyeoAi.js, not a runtime frontend asset.`
  - `integration`: `main AI CTA now routes to ./src/pages/ai.html through AI_PAGE_URL, the popular-movie 모두 보기 link routes to ./src/pages/dd.html, and the MBTI seat flow remains routed through ./src/basic/seatRecommendMbti.html.`
  - `cleanup`: `removed trailing whitespace from imported daboyeoAi.js after git diff --check flagged it.`
  - `verification`: `node --check passed for frontend/src/js/pages/daboyeoAi.js and frontend/src/js/pages/script.js; git diff --check passed with CRLF warnings only.`
- review_fix:
  - `frontend/src/js/pages/daboyeoAi.js`: `restored daboyeoSearchContext reading, searchFilters payload wiring, search-condition summaries, and context-aware preview showtimes.`
  - `frontend/src/js/pages/daboyeoAi.js`: `changed the precise recommendation mode tags from fast-mode E2B wording to E4B/precision wording.`
  - `verification`: `node --check passed for frontend/src/js/pages/daboyeoAi.js and frontend/src/js/pages/script.js; git diff --check passed with CRLF warnings only; static checks found payload.searchFilters and E4B precise tags.`
- ksg_incremental_refresh:
  - `source`: `fetched origin/ksg at a944a88 with two new commits after 78873c7.`
  - `included`: `latest frontend/src/pages/allMovies.html, frontend/src/css/allMovies.css, frontend/src/css/common.css, main popular-movie route to allMovies, and safe AI layout tweaks.`
  - `excluded`: `patch.js plus unrelated xx/temp file deletions; did not overwrite daboyeoAi.js wholesale because origin/ksg would remove local searchFilters/E4B review fixes.`
  - `path_fix`: `allMovies favicon and common script URLs were adjusted from ksg root-relative assumptions to the current src/pages location.`
  - `verification`: `node --check passed for daboyeoAi.js and script.js; git diff --check passed with CRLF warnings only; static checks confirmed payload.searchFilters and E4B precise tags remain.`

## Writer Slot

- writer_slot: `main`
- write_set: `STATE.md, ERROR_LOG.md if needed, frontend/src/pages/daboyeoAi.html, frontend/src/js/pages/daboyeoAi.js, frontend/src/css/daboyeoAi.css, backend static mirror files, backend recommendation provider routing files, backend/src/main/resources/application.yml, .env.example, backend/README.md`
- write_sets:
  - `main`: `STATE.md, ERROR_LOG.md if needed, frontend/src/pages/daboyeoAi.html, frontend/src/js/pages/daboyeoAi.js, frontend/src/css/daboyeoAi.css, backend static mirror files, backend recommendation provider routing files, backend/src/main/resources/application.yml, .env.example, backend/README.md`
- shared_assets_owner: `main`
- note: `One shared task board is active; no concurrent registry mode.`
- concurrent_note: `No subagents are used; provider selector UI and backend request routing are one integrated contract.`

## Contract Freeze

- contract_freeze: `Add GPT/local provider selection to the 5/5 AI recommendation mode screen, make fast/precise card model labels follow the selected provider, send aiProvider to the backend, and route local vs GPT-compatible AI calls by configurable backend settings.`
- note: `Keep local provider as the default demo path; do not implement OAuth login or expose tokens in frontend code.`
- contract_source: `user request`
- contract_revision: `2026-04-29-ai-provider-route-selector`
- verification_target: `frontend syntax check, backend compile or Gradle result, local/GPT aiProvider API smoke, 8080 static/browser visibility, git diff --check`

## Reviewer

- reviewer: `main self-review`
- reviewer_target: `provider selector visibility, request payload contract, configurable GPT/local backend routing, no secret leakage, and 8080 static mirror freshness`
- reviewer_focus: `make GPT/local selection real without exposing OAuth internals or breaking the local default demo path`

## Last Update

- timestamp: `2026-04-29 13:40:00 +09:00`
- note: `GPT recommendation prompt depth differentiation verified: GPT fast/precise now use richer prompt/schema/result UI, jar is refreshed, and Spring is running as PID 12836; live recommendation currently stops at no_usable_showtimes due DB showtime freshness.`

- timestamp: `2026-04-29 12:39:00 +09:00`
- note: `Poster image fallback verified: external image failure now shows a title fallback instead of a blank card, Spring static jar was refreshed, and server is running as PID 9768.`

- timestamp: `2026-04-29 12:31:00 +09:00`
- note: `Poster card frame polish verified: dark padded poster frame, inset highlight, hover/selected states, Spring static jar refreshed, and server is running as PID 20664.`

- timestamp: `2026-04-29 12:22:00 +09:00`
- note: `First-step AI guide 이전 button hotfix verified: browser history back works from the audience step, internal step-back still works from mood, Spring static jar was refreshed, and server is running as PID 13016.`

- timestamp: `2026-04-29 11:40:00 +09:00`
- note: `AI provider route selector verified: browser shows local/GPT selector and GPT-5.5 reasoning labels, frontend sends aiProvider, backend accepts local/gpt routing, patched jar is running on PID 13160, and local/gpt recommendation payloads both avoid 4xx contract failures.`

- timestamp: `2026-04-29 11:10:00 +09:00`
- note: `Durable dotenv fix verified: BOM-safe .env loading, DABOYEO_DB_* and spring.datasource.* derivation, current jar spring.factories registration, health 200, and recommendation session 200 from normal jar runtime.`

- timestamp: `2026-04-29 11:20:00 +09:00`
- note: `Reclassified browser comment into AI provider route selector implementation: add GPT/local UI on the mode screen, update card model labels, send aiProvider, and route backend AI settings by provider.`

- timestamp: `2026-04-29 10:47:00 +09:00`
- note: `Reclassified the session failure follow-up into a durable Spring TiDB dotenv mapping implementation with config/test/doc write set.`

- timestamp: `2026-04-29 10:42:00 +09:00`
- note: `Runtime session failure triage: /api/health was ok but /api/recommendation/sessions timed out because Spring was first started with the default localhost DB URL, then with a malformed TiDB URL, then with useSSL=false; restarted the sandbox-external Spring process with .env TiDB values, corrected URL interpolation, and useSSL=true.`

- timestamp: `2026-04-29 00:10:00 +09:00`
- note: `Created docs/AI_CODEX_OAUTH_DEPLOYMENT_PLAN.md with the agreed Oracle Cloud + Codex OAuth gateway implementation plan; git diff --check passed with existing CRLF warnings only.`

- timestamp: `2026-04-29 00:00:00 +09:00`
- note: `Reclassified the active task to a documentation-only Codex OAuth deployment plan with write set limited to STATE.md and docs/AI_CODEX_OAUTH_DEPLOYMENT_PLAN.md.`

- timestamp: `2026-04-28 00:20:00 +09:00`
- note: `Completed the team-shareable DB contract package with migration 005, docs, verifier updates, observed provider status mappings, and midnight showtime normalization checks; Gradle remains blocked by local native-platform.dll loading.`

- timestamp: `2026-04-28 00:35:00 +09:00`
- note: `User requested DB README application instructions plus commit and push; staying on branch lsh and publishing the completed DB contract package.`

- timestamp: `2026-04-28 00:00:00 +09:00`
- note: `Reframed writer slot and contract freeze around the team DB schema-sharing package before final verifier/docs close-out.`

- timestamp: `2026-04-24 17:32:00 +09:00`
- note: `Fixed live movies frontend review findings while deferring unfinished backend collector/API robustness by user request.`

- timestamp: `2026-04-24 17:12:00 +09:00`
- note: `Restored kmh files from a git archive to preserve UTF-8 Korean bytes and verified no literal question-mark mojibake remains in the imported surfaces.`

- timestamp: `2026-04-24 17:04:00 +09:00`
- note: `Reclassified the task into literal mojibake repair after confirming imported kmh files contain saved question marks rather than recoverable encoding bytes.`

- timestamp: `2026-04-24 16:58:00 +09:00`
- note: `Integrated origin/kmh selectively, preserved current lsh/AI work, fixed the imported seat status test failure, and verified JS plus backend tests.`

- timestamp: `2026-04-24 16:44:00 +09:00`
- note: `Reclassified the request into selective origin/kmh integration after confirming origin/kmh diverges from early main and would be unsafe as a blanket merge.`

- timestamp: `2026-04-24 16:34:00 +09:00`
- note: `Reclassified the new browser-comment request into a tiny AI result-card action cleanup and patched the renderer/CSS/cache-bust targets.`

- timestamp: `2026-04-24 16:18:00 +09:00`
- note: `Imported the latest ksg allMovies refresh selectively and preserved local AI recommendation review fixes.`

- timestamp: `2026-04-24 16:10:00 +09:00`
- note: `Reclassified the new request into an incremental selective import from origin/ksg after fetching two new ksg commits.`

- timestamp: `2026-04-24 15:36:00 +09:00`
- note: `Fixed the accepted review findings by restoring saved search filters in the AI recommendation payload and correcting precise-mode labels.`

- timestamp: `2026-04-24 15:30:00 +09:00`
- note: `Re-scoped the current import task into review-fix and final commit/push after two code-review findings were accepted.`

- timestamp: `2026-04-24 15:12:26 +09:00`
- note: `Imported selected ksg frontend files, excluded patch.js, wired the main AI and popular-movie entry points, and verified JS/diff checks.`

- timestamp: `2026-04-24 15:08:30 +09:00`
- note: `Reclassified the task into a selective ksg frontend import after confirming origin/ksg contains AI page work, Top3/discovery pages, and scratch patch.js.`

- timestamp: `2026-04-24 14:52:04 +09:00`
- note: `Removed the static front-zone tag and made front labels recommendation-driven only.`

- timestamp: `2026-04-24 14:49:50 +09:00`
- note: `Reclassified the browser comment into removing the static front tag and making front labels recommendation-driven.`

- timestamp: `2026-04-24 14:20:07 +09:00`
- note: `Raised generated PNG icon blur to 0.6px and verified the diff check.`

- timestamp: `2026-04-24 14:13:13 +09:00`
- note: `Raised generated PNG icon blur to 0.35px and verified the diff check.`

- timestamp: `2026-04-24 14:13:13 +09:00`
- note: `Re-scoped the tiny icon polish follow-up to raise generated PNG icon blur to 0.35px.`

- timestamp: `2026-04-24 14:08:45 +09:00`
- note: `Applied sub-pixel blur to generated MBTI PNG icons and cache-busted the page assets.`

- timestamp: `2026-04-24 14:07:24 +09:00`
- note: `Reclassified the follow-up into a tiny CSS micro-blur polish for the generated MBTI PNG icons.`

- timestamp: `2026-04-24 13:55:54 +09:00`
- note: `Added the default front-zone label and defringed the generated PNG sprite cutout edges.`

- timestamp: `2026-04-24 13:52:40 +09:00`
- note: `Reclassified the second browser-comment polish into adding a default front label and defringing the generated PNG sprite.`

- timestamp: `2026-04-24 13:14:00 +09:00`
- note: `Applied the latest browser comments: title line break, PNG sprite smoothing, side left/right labels, and screen-relative front/rear map semantics.`

- timestamp: `2026-04-24 13:07:32 +09:00`
- note: `Reclassified the new browser comments into a single-session polish pass for title wrapping, PNG icon rendering, and theater-map zone semantics.`

- timestamp: `2026-04-24 12:31:51 +09:00`
- note: `Deleted the old SVG sprite after replacing the last metric-chip SVG references and verifying that frontend/src no longer references it.`

- timestamp: `2026-04-24 12:27:25 +09:00`
- note: `Reclassified the follow-up into removing the old SVG dependency; remaining metric icon references must be replaced before confirming local deletion.`

- timestamp: `2026-04-24 12:19:17 +09:00`
- note: `Reclassified the accepted-layout follow-up into alias-based generated MBTI sprite integration using the imagegen workflow.`

- timestamp: `2026-04-24 12:07:13 +09:00`
- note: `Applied the latest browser-comment follow-up: removed MBTI card zone text, added 16 larger MBTI-specific SVG symbols, matched the top result panel to the selector row, and expanded the lower theater map with aisle spacing.`

- timestamp: `2026-04-24 11:24:34 +09:00`
- note: `Refined the MBTI seat page toward the accepted mock with sprite icons, right-panel lower metrics, bottom CTA spacing, and computed per-seat fit percentages with a data-fidelity disclaimer.`

- timestamp: `2026-04-24 11:03:40 +09:00`
- note: `The annotated main CTA was hardened with a direct onclick route to seatRecommendMbti.html?flow=mbti and verified in the in-app browser from localhost:5500.`

- timestamp: `2026-04-24 10:47:38 +09:00`
- note: `Implemented the separate headerless MBTI seat-recommendation frontend, wired the current main seat section into it, preserved the existing AI guide route, and completed lightweight JS/diff verification.`

- timestamp: `2026-04-24 09:44:10 +09:00`
- note: `The user selected MBTI별 성향 좌석 추천 as the preferred topic, so the design task was narrowed again from multiple seat flows to five MBTI-focused frontend concepts.`
- timestamp: `2026-04-24 09:44:10 +09:00`
- note: `Five parallel subagents returned MBTI-only concept briefs covering a 16-type grid, four-quadrant map, result dashboard, unknown-MBTI quiz, and theater-seat atlas.`
- timestamp: `2026-04-23 16:00:39 +09:00`
- note: `The user rejected the standalone-page context, so the contract was narrowed again: every new concept must depict a subpage opened from the current main-page seat section.`
- timestamp: `2026-04-23 16:00:39 +09:00`
- note: `Five new parallel subagents returned click-through briefs for MBTI, couple, group, random, and the 명당 좌석 찾기 gateway page.`
- timestamp: `2026-04-23 14:52:28 +09:00`
- note: `The task was reclassified from the completed Stitch PPT work into a new read-only seat-guidance design exploration based on the actual daboyeo main-page style system.`
- timestamp: `2026-04-23 14:52:28 +09:00`
- note: `Five parallel subagents returned distinct variant briefs covering hero-led, quiz-led, seat-map-led, couple-group-led, and MBTI-card-led seat recommendation concepts.`
- timestamp: `2026-04-22 12:58:32 +09:00`
- note: `The task was narrowed again: the real target is a 1440x1026 in-progress Figma presentation matching the attached Desktop-1/Desktop-2 style, not just a generic Figma Slides deck.`
- timestamp: `2026-04-22 12:58:32 +09:00`
- note: `The user approved creating a fresh session, so execution moved from planning around tool limits into direct project generation.`
- timestamp: `2026-04-22 13:10:11 +09:00`
- note: `Fresh Stitch project 13482283388031437931 now contains the body-slide generations from 문제 제기 through 마무리, with exact 1440x1026 enforcement still limited by Stitch canvas defaults.`
- timestamp: `2026-04-22 13:16:07 +09:00`
- note: `A full PPT reformat pass was applied across the generated slides to remove the earlier webpage/landing-page feel and force presentation-style composition.`
- timestamp: `2026-04-22 13:59:02 +09:00`
- note: `The user redirected the work into repo-direction verification, so the active phase was reset from implementation to read-only investigation.`
- timestamp: `2026-04-22 13:59:02 +09:00`
- note: `Repo-direction investigation completed from root context files and per-folder README documents; the dominant product direction is collectors-first movie showtime comparison rather than a ticketing-automation PPT narrative.`
- timestamp: `2026-04-22 14:11:40 +09:00`
- note: `Implementation truth was cross-checked in backend/build.gradle and backend/src/main/resources/application.yml, confirming Java 21, Spring Boot 3.5.13, Spring JDBC, Flyway, MySQL Connector/J, and LM Studio-backed recommendation settings as secondary extension scope.`
- timestamp: `2026-04-22 14:11:40 +09:00`
- note: `The active phase is back to implementation so the PPT can be regenerated from the frozen repo-grounded storyline instead of patching the older automation-centered deck.`
- timestamp: `2026-04-22 14:29:30 +09:00`
- note: `A new Stitch project 10979052864160268633 now contains the repo-grounded replacement deck from 문제 제기 through 마무리, including the corrected detailed implementation slides around collectors and minimal common schema.`
- timestamp: `2026-04-22 14:29:30 +09:00`
- note: `A final deck-wide consistency edit succeeded and unified the slides into a single presentation tone with white body slides, navy accents, a dark closing slide, and reduced web/app-like chrome.`
- timestamp: `2026-04-22 14:38:20 +09:00`
- note: `The user accepted the corrected deck direction but flagged the remaining exact-size mismatch, so the active task has been reclassified from content verification to tool-capability investigation for exact 1440x1026 resizing.`
- timestamp: `2026-04-22 14:38:20 +09:00`
- note: `Current tool discovery found no direct Figma write/resize action in this session, only Stitch generation/edit plus Figma read/generate connectors.`
- timestamp: `2026-04-22 14:42:50 +09:00`
- note: `The user asked for an accessible Figma workspace, so execution moved from pure capability investigation into creating a fresh Figma file within the available connector surface.`
- timestamp: `2026-04-22 14:45:40 +09:00`
- note: `That fallback was stopped before creation because the user made it explicit that a workspace is meaningless without real edit capability, so the task is now blocked on missing Figma write tooling.`
- timestamp: `2026-04-22 14:53:41 +09:00`
- note: `The user redirected the work again: the active task is now a read-only review of the repo-grounded Stitch deck in project 10979052864160268633 to find content gaps or weak points.`
- timestamp: `2026-04-22 14:59:05 +09:00`
- note: `The user asked to fix the reviewed deck, so the task moved back into implementation with a repo-truth refresh across README, collectors, frontend, backend, and schema contract files before editing Stitch screens.`
- timestamp: `2026-04-22 15:21:59 +09:00`
- note: `Refined Stitch outputs were generated for sections 2-10, but the tool created new screen IDs rather than replacing the original final-row canvas instances in place.`

## Retrospective

- task: `GPT recommendation prompt depth differentiation`
- score_total: `6`
- evaluation_fit: `full fit; the change affected prompt contract, provider config, service sanitization, result-card rendering, and running jar state`
- orchestration_fit: `single-session fit; one request/response contract controlled all edits and delegation would raise mismatch risk`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the prior poster fallback task was complete, then the user clarified that Gemma/GPT are engine choices and each keeps fast/precise modes with GPT always more analytical`
- reviewer_findings: `local Gemma still uses compact tag output, while GPT can now return narrative reason, analysis, practical value, and caution fields; the frontend visibly marks GPT analysis/caution cards`
- verification_outcome: `node --check passed; Gradle remained blocked by native-platform.dll, so javac compiled the changed backend classes; 8080 health/static checks passed; GPT POST accepted the route but returned no_usable_showtimes because data freshness is currently the blocking runtime condition`
- next_gate_adjustment: `before judging GPT result quality in-browser, refresh future showtime coverage so recommendation requests reach the model path instead of stopping at candidate availability`

- task: `AI poster image failure fallback`
- score_total: `2`
- evaluation_fit: `light fit; acceptance was a focused resilience fix for missing external poster images`
- orchestration_fit: `single-session fit; one JS/CSS surface plus Spring static mirror needed no delegation`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the poster frame polish revealed a separate runtime image-loading failure path, so a fallback panel was added without changing backend API behavior`
- reviewer_findings: `poster cards now avoid the blank-frame failure mode by showing a title fallback and 준비 중 copy when posterUrl is missing or the img error event fires`
- verification_outcome: `node --check passed for daboyeoAi.js; 8080 health and static JS/CSS/HTML checks passed; git diff --check passed with CRLF warnings only`
- next_gate_adjustment: `when external media URLs are used in demo UI, add visible broken-media fallbacks at the component boundary instead of only relying on source data completeness`

- task: `AI poster card frame visual polish`
- score_total: `2`
- evaluation_fit: `light fit; acceptance was a focused visual correction on one card family`
- orchestration_fit: `single-session fit; one CSS surface and static mirror needed no delegation`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the old pale border and flat gray backing made poster images feel detached from the card`
- reviewer_findings: `the new darker frame, inset highlight, and more integrated selected state make posters feel contained without changing the poster data or layout`
- verification_outcome: `8080 static CSS checks, app-browser poster-step visual check, Spring health, and git diff --check passed`
- next_gate_adjustment: `for poster-heavy UI, prefer a matte/inset frame over bright outline borders when the background is already dark and atmospheric`

- task: `AI guide first-step back button browser navigation`
- score_total: `2`
- evaluation_fit: `light fit; acceptance was one visible button behavior plus static freshness`
- orchestration_fit: `single-session fit; one JS handler and cache-busted static mirror were cheaper than delegation`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the existing button was visible on the first step but only handled internal stepBackMap entries, so it became a no-op there`
- reviewer_findings: `first-step 이전 now has a real browser-back meaning, and later steps keep the current survey-step back behavior`
- verification_outcome: `node --check, 8080 health/static checks, app-browser first-step and second-step back checks, and git diff --check passed`
- next_gate_adjustment: `when a shared navigation button is visible on a step without internal history, give it a page-level fallback instead of leaving an empty branch`

- task: `AI provider route selector on recommendation mode step`
- score_total: `5`
- evaluation_fit: `full fit; the task combined a user-visible route selector, request-contract change, provider-specific backend routing, and running 8080 jar/static verification`
- orchestration_fit: `single-session fit; the UI state, payload shape, Spring properties, and jar patching were tightly coupled around the same aiProvider contract`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the first patched jar failed because adding a secondary constructor to a configuration-properties record made Spring require an explicit @ConstructorBinding on the canonical constructor`
- reviewer_findings: `the final route keeps local as default, uses GPT-5.5 with low/high reasoning labels, keeps GPT gateway configuration on the backend, and avoids exposing OAuth or API secrets to the browser`
- verification_outcome: `node --check, javac, 8080 health, local/gpt recommendation API smoke, app-browser GPT label switch, and git diff --check passed; Gradle remains blocked by native-platform.dll`
- next_gate_adjustment: `when adding overloads to Spring @ConfigurationProperties records, bind the canonical constructor explicitly before runtime jar verification`

- task: `Durable Spring TiDB dotenv mapping`
- score_total: `6`
- evaluation_fit: `full fit; the user explicitly rejected a temporary workaround and the failure crossed dotenv parsing, Spring datasource binding, jar packaging, and runtime API recovery`
- orchestration_fit: `single-session fit; one config/runtime lane was cheaper and safer than delegation because each discovery result directly changed the same fix surface`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the initial env mapping fix was not enough because the root .env had a UTF-8 BOM on TIDB_HOST and the stale boot jar was missing spring.factories registration`
- reviewer_findings: `the durable contract now handles BOM-safe .env parsing, derives both DABOYEO_DB_* and spring.datasource.* keys, preserves explicit env overrides, defaults TiDB Cloud to TLS, and avoids logging secrets`
- verification_outcome: `Gradle remained blocked by native-platform.dll; javac/manual harness passed; the patched normal jar returned health 200 and recommendation session 200`
- next_gate_adjustment: `when a Spring environment postprocessor fix seems ignored, check both property source contents and packaged registration resources before blaming DB connectivity`

- task: `Repo-direction verification from WORKSPACE_CONTEXT and README set`
- score_total: `4`
- evaluation_fit: `light fit; the task needed grounded reading and a concise fact-vs-inference summary`
- orchestration_fit: `single-session fit; one lane kept the repo narrative consistent across documents`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the active PPT/design task was paused because the user asked to reset the narrative against repository truth sources first`
- reviewer_findings: `the main mismatch was narrative drift: earlier slide work leaned toward ticketing automation/RPA as the product core, while the repo docs center on 3-site showtime collection, comparison, and filtering`
- verification_outcome: `WORKSPACE_CONTEXT plus root/per-folder README files were read; db schema contract was also checked because db/README explicitly points to it as canonical naming guidance`
- next_gate_adjustment: `before continuing any presentation work, anchor the storyline to collectors-first comparison/search architecture and treat recommendation or alerting as secondary/extension scope unless the repo docs are revised`

- task: `Repo-truth PPT rebuild from section 2 onward`
- score_total: `7`
- evaluation_fit: `full fit; the work needed source-of-truth document reads, implementation truth checks, concrete Stitch artifacts, and an explicit note about tool limits`
- orchestration_fit: `single-session fit; reading repo truth, reshaping the narrative, and regenerating the deck were tightly coupled and cheaper to keep in one lane`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the earlier automation-heavy PPT story was discarded after the user required README-grounded direction verification; one UI/UX slide generation timed out and was replaced through a shorter fallback generation before a deck-wide consistency pass`
- reviewer_findings: `the regenerated deck now centers on 3-provider showtime collection, minimal common schema, backend DB/API boundaries, vanilla frontend comparison/search, and extension-scope recommendation rather than inventing ticketing automation as the product core`
- verification_outcome: `local repo docs and stack-truth files were read, new Stitch project 10979052864160268633 was generated for sections 2-10, and a final edit_screens pass unified all 10 slides into one presentation system`
- next_gate_adjustment: `for future presentation work in this repo, treat backend/build.gradle and application.yml as stack truth, use README plus schema contract as product-direction truth, and start with a strong anti-web-slide constraint in the first generation prompt`

- task: `Exact 1440x1026 frame enforcement follow-up`
- score_total: `5`
- evaluation_fit: `light fit; the issue is now tool capability and delivery accuracy rather than content correctness`
- orchestration_fit: `single-session fit; one lane can verify tool availability faster than splitting the work`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the task was reclassified after the user approved the rebuilt content but rejected the still-incorrect frame size`
- reviewer_findings: `the remaining gap is not deck narrative but exact frame enforcement; Stitch generation and edit paths still surface larger desktop canvases, and current connector discovery did not expose a direct Figma resize/write tool`
- verification_outcome: `tool search confirmed only Stitch generation/edit and Figma read/generate endpoints in this session; no direct frame resize action is currently available`
- next_gate_adjustment: `when exact pixel sizing matters for presentation work, verify write-capable Figma tooling before investing heavily in Stitch-generated slides`

- task: `Accessible Figma workspace fallback creation`
- score_total: `5`
- evaluation_fit: `light fit; the goal is now to hand off an editable Figma file while keeping the unresolved exact-size limitation explicit`
- orchestration_fit: `single-session fit; deck outline and file creation stay cheaper in one lane`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `after the exact-size blocker was confirmed, the task shifted again because the user preferred a shared Figma workspace over continuing inside Stitch only`
- reviewer_findings: `the key distinction is that a Figma workspace can be created now, but exact 1440x1026 frame enforcement still depends on unavailable write tooling`
- verification_outcome: `not pursued; the user rejected the fallback because it would still not allow actual modification`
- next_gate_adjustment: `do not create a nominal shared workspace when the missing capability is the core value of the request`

- task: `Repo-grounded Stitch deck content review`
- score_total: `4`
- evaluation_fit: `light fit; the work needed slide-content inspection and repo-truth comparison rather than code-level verification`
- orchestration_fit: `single-session fit; one lane kept the final-row identification and content judgment consistent`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the blocked exact-size follow-up was paused because the user asked for a read-only content review of the corrected deck instead`
- reviewer_findings: `the final row still contains major drift in the technology, UI/UX, value/limitations, and detailed implementation slides; several claims remain generic or invented, and the row order itself is out of sequence for a live presentation`
- verification_outcome: `project metadata and final-row screens from Stitch project 10979052864160268633 were inspected directly, and the review targeted the later unified row rather than the older duplicated screens in the same project`
- next_gate_adjustment: `if this deck is revised, fix slide order first and then rewrite 4, 7, 9, 6-1, and 6-2 before polishing the lighter sections`

- task: `Repo-grounded Stitch deck content revision`
- score_total: `6`
- evaluation_fit: `light fit; the work needed repo-truth confirmation, targeted slide rewrites, and output ID tracking rather than local code verification`
- orchestration_fit: `single-session fit; one lane kept the repo truth, review findings, and Stitch revisions aligned`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the task moved from critique into implementation after the user asked to fix the deck directly`
- reviewer_findings: `the refined outputs replaced the incorrect stack slide, collectors architecture slide, collection/schema details, UI/UX story, team collaboration story, effect/limitations slide, and final closing summary with repo-grounded presentation copy`
- verification_outcome: `Stitch edit passes produced refined screens for sections 2-10 and their IDs were recorded; the remaining tool limitation is that the refined screens were emitted as new screens instead of replacing the original canvas instances`
- next_gate_adjustment: `for future Stitch deck repair work, expect edit_screens to create new refined outputs and plan for explicit output-ID tracking or manual canvas replacement`

- task: `Fresh Stitch session PPT body generation from section 2 onward`
- score_total: `6`
- evaluation_fit: `light fit; the task needed concrete generated slide evidence and a clear note about sizing limits`
- orchestration_fit: `single-session fit; outline extraction, prompt shaping, and screen generation were faster in one lane`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the task first tried Figma Slides, then was reclassified again after the user clarified the real target was an in-progress 1440x1026 presentation file and explicitly approved a fresh session; after first-generation review, a second deck-wide pass was needed because the screens still felt too web-like`
- reviewer_findings: `section coverage and visual tone were achieved, and the second pass removed much of the webpage feel, but exact 1440x1026 enforcement was still not guaranteed by Stitch screen generation`
- verification_outcome: `fresh Stitch project 13482283388031437931 generated 10 body-slide screens from 문제 제기 through 마무리 and then successfully applied a deck-wide PPT reformat pass; create_design_system failed and list_screens returned empty, so per-call screen metadata was used as evidence`
- next_gate_adjustment: `for future PPT work, add a strong anti-web-slide constraint in the very first generation prompt instead of waiting for a review pass`

- task: `Figma Slides deck generation from image-defined section plan`
- score_total: `6`
- evaluation_fit: `light fit; the task needed output confirmation and section-coverage review rather than deep repo testing`
- orchestration_fit: `single-session fit; image interpretation, prompt shaping, and tool verification stayed cheaper in one lane`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the first full generate_deck call hit an unexpected response-type failure, so a smaller validation deck was used to confirm tool behavior before regenerating the full deck`
- reviewer_findings: `the main delivery risk was tool behavior and the Figma Slides image-reference limitation, not repository code quality`
- verification_outcome: `test deck and full deck generation both succeeded; local state tracking and workspace verification commands were completed`
- next_gate_adjustment: `when generate_deck fails ambiguously, first rerun with a 3-slide minimum sanity case to separate schema issues from connector behavior`

- task: `STATE board recovery during AI page renewal`
- score_total: `2`
- evaluation_fit: `light fit; a readable task board was required before continuing implementation`
- orchestration_fit: `single-session fit; the fix was an internal state recovery only`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `STATE.md became encoding-corrupted during inline edits and was rebuilt as a clean UTF-8 task board`
- reviewer_findings: `no product-code change yet; this was internal orchestration recovery`
- verification_outcome: `STATE.md is readable again and current-task contract is explicit`
- next_gate_adjustment: `when patching large state sections, prefer smaller apply_patch steps or rewrite the board cleanly once instead of mixed-encoding incremental edits`

- task: `Fresh Stitch session concept generation from main index only`
- score_total: `8`
- evaluation_fit: `full fit; the outcome required concrete design artifacts rather than local code edits`
- orchestration_fit: `single-session fit; prompt design and Stitch generation stayed tightly coupled`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the task was reclassified away from concept comparison into a fresh-session generation flow seeded only by the main index page`
- reviewer_findings: `the new concept is structurally distinct from the previous AI page and prior Stitch outputs, but no local implementation decision has been made yet`
- verification_outcome: `fresh Stitch project 18001920443669087555 and screen b4c22a52b73745cda879558cde5c5da2 were generated successfully`
- next_gate_adjustment: `when the user asks for a truly fresh concept, start a new Stitch project immediately instead of iterating inside the earlier comparison project`

- task: `Second fresh Stitch session with Audience Gallery layout`
- score_total: `8`
- evaluation_fit: `full fit; the design outcome needed concrete Stitch artifacts and comparison evidence`
- orchestration_fit: `single-session fit; main-brand extraction, prompt writing, and generation stayed cheaper in one lane`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the fresh-session task was refined again to exclude the central pass layout and force a new macro-structure`
- reviewer_findings: `the new concept is meaningfully different from the Screening Pass idea because it shifts to a full-width audience-card rail instead of a dominant center panel`
- verification_outcome: `fresh Stitch project 7742688576431333902 and screen b41e5db6e26e40a49f27c4aeb2330a9d were generated successfully`
- next_gate_adjustment: `when generating another comparison concept, explicitly ban the latest layout pattern in the prompt so Stitch cannot drift back toward it`

- task: `Audience Gallery full-flow Stitch expansion`
- score_total: `8`
- evaluation_fit: `full fit; the accepted concept had to be extended screen-by-screen against the real frontend step order`
- orchestration_fit: `single-session fit; one lane kept the concept continuity and prompt calibration cheaper than delegation`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the task shifted from concept exploration to accepted-concept serial generation in the same Stitch project`
- reviewer_findings: `each step now has its own micro-layout while remaining inside the same Nocturne Premiere family; the result screen reads like a recommendation service rather than a survey summary`
- verification_outcome: `step screens for mood, avoid, posters, mode, and results were all generated successfully in project 7742688576431333902`
- next_gate_adjustment: `when the user accepts a concept, pin the real frontend step order first and then generate the remaining screens in that order to avoid dead-end variants`

- task: `Step 2 and Step 3 visual enrichment`
- score_total: `6`
- evaluation_fit: `light fit; this was a focused refinement pass on two accepted screens`
- orchestration_fit: `single-session fit; direct Stitch edits were cheaper than reopening broader concept exploration`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the accepted flow remained intact, but Step 2 and Step 3 were revised because they felt too text-heavy`
- reviewer_findings: `Step 2 now has cinematic image cues per mood tile and Step 3 now has elegant iconography, which improves scanability without breaking the premium tone`
- verification_outcome: `Stitch edit_screens succeeded for both targeted screens in project 7742688576431333902`
- next_gate_adjustment: `when a step feels text-heavy, prefer imagery or restrained iconography over emoji so the premium tone stays intact`

- task: `Step 2 and Step 3 image-led refinement`
- score_total: `6`
- evaluation_fit: `light fit; this was a focused second-pass refinement on two accepted screens`
- orchestration_fit: `single-session fit; direct Stitch edits stayed the cheapest path`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the first visual refresh was not strong enough because Step 3 still leaned on icons, so both screens were pushed further toward image-led choices`
- reviewer_findings: `Step 2 now reads much closer to Step 1 with distinct per-option imagery, and Step 3 no longer feels like a text/icon utility board`
- verification_outcome: `Stitch edit_screens succeeded again for both targeted screens in project 7742688576431333902`
- next_gate_adjustment: `when the user asks for Step 1-like richness, treat that as a request for per-option owned imagery rather than abstract texture or iconography`

- task: `Step 2 and Step 3 layout redo`
- score_total: `6`
- evaluation_fit: `light fit; this was a constrained design correction pass`
- orchestration_fit: `single-session fit; direct Stitch edits remained the fastest path`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the image-led pass still missed the mark, so Step 2 and Step 3 were restructured again at the layout level`
- reviewer_findings: `Step 2 now stays within the dark Nocturne palette and Step 3 feels structurally complete with a sixth neutral choice card`
- verification_outcome: `Stitch edit_screens succeeded for both redesigned screens in project 7742688576431333902`
- next_gate_adjustment: `when imagery clashes with the theme, change the layout container and image role together instead of only swapping pictures`

- task: `Step 2 button polish and Step 3 image replacement`
- score_total: `5`
- evaluation_fit: `light fit; this was a narrow polish pass on two already accepted redesigns`
- orchestration_fit: `single-session fit; direct edits were fastest`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `Step 2 needed one more interaction polish pass and Step 3 needed only image replacement, not another full concept reset`
- reviewer_findings: `Step 2 no longer has awkward detached button placement and Step 3 keeps the stronger 6-slot layout while using more fitting imagery`
- verification_outcome: `Stitch edit_screens succeeded for the targeted polish pass in project 7742688576431333902`
- next_gate_adjustment: `when a user flags button placement after a layout redo, keep the layout and polish affordance integration instead of reopening the whole composition`

- task: `Local Step 3 internet-image implementation`
- score_total: `8`
- evaluation_fit: `full fit; the task shifted from design evidence to real local UI implementation and needed code-level verification`
- orchestration_fit: `single-session fit; one tight JS/CSS slice was cheaper than delegation`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the task was reclassified from Stitch-only design iteration into a local Step 3 implementation using external image sources`
- reviewer_findings: `the avoid step now reads like the accepted concept much better because six cards have distinct imagery while multi-select behavior remains intact`
- verification_outcome: `node --check passed for daboyeoAi.js and script.js; git diff --check passed with CRLF warnings only`
- next_gate_adjustment: `when a user says generated visuals feel dull, switch quickly to sourced imagery and implement the accepted slice locally instead of over-iterating prompts`

- task: `Seat-guidance concept variants from main-page visual language`
- score_total: `7`
- evaluation_fit: `full fit; the task needed real file-based style extraction, explicit variant separation, and artifact-level output rather than a loose brainstorm`
- orchestration_fit: `delegated-parallel fit; one local style freeze plus five disjoint read-only variant briefs justified the user-requested parallel agent layout`
- predicted_topology: `delegated-parallel`
- actual_topology: `delegated-parallel`
- spawn_count: `5`
- rework_or_reclassification: `the completed Stitch PPT task was closed and a new design-only task was opened after the user shifted focus to lightweight personality-based seat guidance`
- reviewer_findings: `the strongest common direction was to keep the black-and-purple cinematic brand family while replacing explicit AI framing with lighter personality, MBTI, zone, or scenario-based guidance`
- verification_outcome: `frontend/index.html, style.css, common.css, and daboyeoAi.css were read; five subagent briefs were collected and frozen into a single final render direction`
- next_gate_adjustment: `when the user asks for multiple mock variants, freeze the brand contract locally first and then split only the creative emphasis across parallel agents`

- task: `Click-through seat subpages from the main recommendation section`
- score_total: `7`
- evaluation_fit: `full fit; the task needed a contract correction from standalone pages to post-click internal pages plus explicit navigation-context review`
- orchestration_fit: `delegated-parallel fit; once the click-through rule was frozen, MBTI, couple, group, random, and gateway subpages had disjoint design ownership`
- predicted_topology: `delegated-parallel`
- actual_topology: `delegated-parallel`
- spawn_count: `5`
- rework_or_reclassification: `the earlier output was invalidated because the user clarified the page context; the task was reclassified around the existing main-page seat section and its click targets`
- reviewer_findings: `the decisive correction was to show breadcrumb or back navigation and make every screen read like an internal page reached from the current section rather than a new landing page`
- verification_outcome: `the main-page section screenshot and frontend style files remained the grounding reference; five fresh subagent briefs were collected for MBTI, couple, group, random, and the CTA gateway`
- next_gate_adjustment: `when generating UI concept images from a section screenshot, freeze the pre-click and post-click relationship explicitly before asking subagents for variants`

- task: `MBTI-only seat-recommendation frontend concept variants`
- score_total: `7`
- evaluation_fit: `full fit; the task needed single-topic narrowing, five distinct MBTI UI approaches, and artifact-level image output`
- orchestration_fit: `delegated-parallel fit; all variants share the MBTI entry contract but have disjoint design emphasis`
- predicted_topology: `delegated-parallel`
- actual_topology: `delegated-parallel`
- spawn_count: `5`
- rework_or_reclassification: `the task was narrowed from multi-flow seat pages after the user chose MBTI별 성향 좌석 추천 as the strongest direction`
- reviewer_findings: `the strongest MBTI directions are the 16-type selector, quadrant map, result dashboard, unknown-MBTI quiz, and seat-map atlas because each keeps one topic while changing the interaction model`
- verification_outcome: `five MBTI-only subagent briefs were collected and frozen for separate image rendering`
- next_gate_adjustment: `when the user likes one concept from a variant set, narrow the next generation around that concept rather than adding more unrelated feature branches`
- timestamp: `2026-04-24 10:37:54 +09:00`
- note: `The user moved from image generation to implementation, so the task was reclassified into a single-session frontend edit with a frozen headerless MBTI seat-page contract.`

- task: `Headerless MBTI seat-recommendation frontend implementation`
- score_total: `7`
- evaluation_fit: `full fit; the work needed visual-contract fidelity, static interaction correctness, and main-page routing verification`
- orchestration_fit: `single-session fit; HTML, CSS, JS data rendering, and main entry wiring were tightly coupled and cheaper to keep in one lane`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the image exploration task became a concrete frontend implementation after the user asked to build from the accepted mock`
- reviewer_findings: `the implementation keeps the page headerless, removes heavy AI framing from the seat section, uses MBTI as the lightweight input, keeps one primary CTA, and preserves the original AI guide route separately`
- verification_outcome: `node --check passed for seatRecommendMbti.js, script.js, and preserved daboyeoAi.js; git diff --check passed with CRLF warnings only`
- next_gate_adjustment: `for future seat-flow work, keep post-click pages visually tied to the main section and avoid reintroducing global nav unless the main page itself gets one`

- task: `Mock-fidelity refinement for MBTI seat recommendation page`
- score_total: `8`
- evaluation_fit: `full fit; the work needed visual comparison, data-fidelity handling, subagent asset participation, browser verification, and code checks`
- orchestration_fit: `delegated-parallel then main integration fit; visual critique and sprite creation were separable, while main kept HTML/CSS/JS integration ownership`
- predicted_topology: `delegated-parallel then main integration`
- actual_topology: `delegated-parallel then main integration`
- spawn_count: `2`
- rework_or_reclassification: `the task expanded from annotated tweaks into a mock-fidelity refinement with computed seat percentages and honest data labeling`
- reviewer_findings: `the prior page had empty lower recommendation-panel space, weak CTA anchoring, flat seat map, missing card icons, and weak profile art compared with the mock`
- verification_outcome: `browser verification found 160 rendered seat percentages, 16 card icons, 3 metric chips, and one disclaimer; node --check and git diff --check passed`
- next_gate_adjustment: `when MBTI or personality UI implies external behavioral data, explicitly separate measured data from computed demo fit scores before rendering percentages`

- task: `Annotated MBTI card icon and theater-map layout follow-up`
- score_total: `6`
- evaluation_fit: `full fit; the follow-up touched visual proportions, generated SVG assets, interaction rendering, and layout spacing`
- orchestration_fit: `single-session fit; the requested edits were tightly coupled across the same HTML/CSS/JS/SVG surfaces`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the right panel and theater map were restructured after the user pointed out the selector/result/map proportions`
- reviewer_findings: `MBTI cards no longer show zone labels, all 16 profile mappings resolve to dedicated sprite symbols, and the map now has explicit aisle columns instead of margin-based seat pushing`
- verification_outcome: `node --check passed for changed JS, SVG parsed as XML, static contract checks passed, localhost served the updated files, and git diff --check passed with CRLF warnings only`
- next_gate_adjustment: `for future in-app browser checks, cache-bust the page URL earlier because stale tab snapshots can otherwise look like implementation failure`

- task: `Remove old MBTI SVG sprite dependency`
- score_total: `7`
- evaluation_fit: `light fit; the acceptance was concrete: no old SVG asset or references should remain`
- orchestration_fit: `single-session fit; the JS fallback, CSS marker styling, and asset deletion were one tight cleanup slice`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the generated PNG sprite became the accepted direction, so the previous SVG sprite was removed instead of polished further`
- reviewer_findings: `the old SVG dependency is gone, metric chips still have non-SVG visual markers, and the MBTI cards/profile remain on the generated PNG sprite`
- verification_outcome: `node --check passed for seatRecommendMbti.js; Select-String found no old SVG or icon-id references under frontend/src; git diff --check passed with CRLF warnings only`
- next_gate_adjustment: `when a generated asset is rejected outright, remove both the asset and all dependent rendering code rather than leaving a hidden fallback path`

- task: `Browser-comment polish for title, icon rastering, and map semantics`
- score_total: `6`
- evaluation_fit: `light fit; the comments were concrete visual/semantic corrections on the current page`
- orchestration_fit: `single-session fit; the HTML title, CSS sprite/map labels, JS label placement, and PNG resampling were one tightly coupled page slice`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the previous task was complete, then the user supplied a new browser-comment polish pass`
- reviewer_findings: `title wrapping is now intentional, the sprite sheet no longer has fractional 4-way cell boundaries, side focus labels distinguish left and right, and front/rear labels now follow the visible screen orientation`
- verification_outcome: `node --check passed for seatRecommendMbti.js; git diff --check passed with CRLF warnings only; sprite image is now 1280x1280 with dimensions divisible by 4`
- next_gate_adjustment: `for generated sprite sheets, normalize final dimensions to an exact grid before wiring them into CSS background-position`

- task: `Default front label and PNG cutout-edge defringe`
- score_total: `5`
- evaluation_fit: `light fit; the comments were narrow and directly verifiable`
- orchestration_fit: `single-session fit; the label, duplicate guard, and PNG cleanup were one page-asset slice`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the previous map polish still left front as only a dynamic zone, so a static front label was added`
- reviewer_findings: `the map now shows 전방 생동석 by default, dynamic front choices hide the static front label to avoid duplication, and the PNG edge matte was cleaned by RGB bleeding plus low-alpha cleanup`
- verification_outcome: `node --check passed for seatRecommendMbti.js; git diff --check passed with CRLF warnings only; sprite remains 1280x1280 and partial-alpha edge pixels dropped from 61858 to 45373`
- next_gate_adjustment: `for chroma-keyed UI sprites, run matte decontamination before judging final icon sharpness in browser`

- task: `Micro-blur polish for generated MBTI icons`
- score_total: `3`
- evaluation_fit: `light fit; this was a tiny CSS-only visual tweak`
- orchestration_fit: `single-session fit; no delegation value for a two-line filter adjustment`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the user preferred trying blur after matte cleanup still left outline jaggies`
- reviewer_findings: `card icons now get blur(0.28px) and the larger profile icon gets blur(0.2px), keeping the effect below obvious out-of-focus territory`
- verification_outcome: `git diff --check passed with CRLF warnings only; static source checks found the blur filters and updated cache-bust query`
- next_gate_adjustment: `if micro-blur still looks poor, replace the raster sprite with cleaner regenerated icons instead of stacking stronger blur`

- task: `Raise MBTI icon blur to 0.6px`
- score_total: `3`
- evaluation_fit: `light fit; this was a direct CSS value adjustment requested by the user`
- orchestration_fit: `single-session fit; no delegation value for a tiny filter adjustment`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `0.35px was applied but visually too subtle, so the blur was raised to 0.6px`
- reviewer_findings: `card and profile icons now both use blur(0.6px), with the CSS query bumped to avoid stale stylesheet loads`
- verification_outcome: `git diff --check passed with CRLF warnings only; static source checks found the blur(0.6px) filters and updated cache-bust query`
- next_gate_adjustment: `if 0.6px looks too soft, tune down slightly or regenerate cleaner non-chroma icon assets`

- task: `Recommendation-driven theater-map zone labels`
- score_total: `4`
- evaluation_fit: `light fit; the user flagged one static label and the fix is directly verifiable`
- orchestration_fit: `single-session fit; no delegation value for a small HTML/CSS/JS cleanup`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the always-on front tag looked like a recommendation label, so it was removed instead of restyled`
- reviewer_findings: `front labels now use the same dynamic primary/secondary recommendation-label path as other zones; the one-off warm front styling and duplicate guard are gone`
- verification_outcome: `node --check passed for seatRecommendMbti.js; git diff --check passed with CRLF warnings only; static source checks found no frontZoneStaticLabel, static-front style, or hasDynamicFrontLabel references`
- next_gate_adjustment: `zone tags that look like recommendations should be generated from selected recommendation data, not shown as permanent map annotations`

- task: `Selective ksg frontend import`
- score_total: `7`
- evaluation_fit: `full fit; cross-branch import needed route compatibility, scratch-file exclusion, and static JS verification`
- orchestration_fit: `single-session fit; one tight frontend integration surface was cheaper than delegation`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the request changed from completed seat-page polish to importing teammate frontend work from origin/ksg`
- reviewer_findings: `AI page refresh and Top3/discovery pages were imported while patch.js was excluded; main AI and popular-movie entry points now route to imported pages; MBTI seat routing remains intact`
- verification_outcome: `node --check passed for daboyeoAi.js and script.js; git diff --check passed with CRLF warnings only`
- next_gate_adjustment: `future ksg imports should continue inspecting scratch/helper files before accepting all branch changes`

- task: `Team-shareable DB schema contract finalization`
- score_total: `8`
- evaluation_fit: `full fit; the output is a shared DB contract spanning migrations, ingest semantics, verifier behavior, and team-facing docs`
- orchestration_fit: `single-session fit; SQL, Java time normalization, Python status mapping, and docs all describe the same contract and were safer in one lane`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `fresh provider seat-status evidence expanded the scope from table creation into seeded status-code mapping and Lotte normalizer correction`
- reviewer_findings: `the shareable contract now covers migrations 001-005, 20 canonical table names, post-midnight showtime normalization, observed seat-status mapping, layout-vs-snapshot separation, and raw-payload safety`
- verification_outcome: `migration dry-run, py_compile, status-normalizer assertions, migration split check, static contract search, secret placeholder scan, and git diff --check passed; Gradle test could not start due local native-platform.dll loading`
- next_gate_adjustment: `when fresh crawl classification exposes status-code counts, compare them against existing normalizers before freezing the DB contract`

- task: `Codex-scored reserve recommendation pool`
- score_total: `8`
- evaluation_fit: `full fit; the work changed AI schema, score validation, result filling policy, and runtime provider behavior`
- orchestration_fit: `single-session fit; Selfdex was used read-only, but implementation stayed coupled across one recommendation contract`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `the prior strict taste gate was intentionally relaxed into taste-first reserve filling after the user asked to avoid 1-2 item outputs`
- reviewer_findings: `direct taste candidates remain first, reserve candidates can fill empty slots, Codex/GPT now return model score s, and server validation caps no-direct taste reserves at 74`
- verification_outcome: `focused recommendation service/client/scorer tests passed outside sandbox; bootJar passed; runtime local fallback and Codex smokes returned 3 action/SF recommendations with reserve scores below the direct match`
- next_gate_adjustment: `for sparse live DB pools, prefer tiered reserve fill plus visible lower scores over strict exclusion that makes the UI look empty`
