# STATE

## Current Task

- task: `Make precise analysisPoint AI-generated`
- phase: `implementation`
- scope: `Have E4B/precise generate one compact analysisPoint per recommendation while fast/E2B stays simple and blank`
- verification_target: `backend recommendation tests, frontend syntax check, bootJar, diff check, and local API benchmark`
- previous_task_note: `Precise analysisPoint was server-generated after id-only rerank; user correctly objected that this weakens the AI analysis claim and accepts around 10s latency.`

## Orchestration Profile

- score_total: `6`
- score_breakdown: `1 LM response contract adjustment, 1 backend parser/model change, 1 response mapping change, 1 token budget change, 1 focused regression tests, 1 local API benchmark`
- hard_triggers: `recommendation contract adjustment, local HTTP model calls`
- selected_rules: `spec-first lightweight, preserve existing public fields, add compatible field, preserve model files, preserve user changes`
- selected_skills: `none`
- execution_topology: `single-session`
- orchestration_value: `low`
- agent_budget: `0`
- spawn_decision: `no spawn; LM schema, parser, service fallback, and tests are tightly coupled and small`
- efficiency_basis: `handoff cost is higher than locally updating the compact precise contract and verifying with focused tests plus API benchmark`
- selection_reason: `user pointed out id-only rerank is not enough to honestly call the displayed analysis AI-generated, and said around 10 seconds is acceptable`

## Evaluation Plan

- evaluation_need: `full`
- project_invariants:
  - `Do not change or delete installed model files.`
  - `Keep LM Studio as the local model provider.`
  - `Keep public recommendation request/response shape stable.`
  - `Do not fake recommendation data.`
  - `Do not expose .env secrets in output.`
- task_acceptance:
  - `Liked seed poster genres are captured separately from generic tag weights.`
  - `Recommendation response adds a compatible analysisPoint field without removing reason/caution/valuePoint.`
  - `Precise/E4B LM response includes id plus one compact analysis tag such as #SF취향.`
  - `Precise/E4B analysisPoint uses the AI-generated tag when valid and falls back to deterministic selected-poster genre analysis when invalid or missing.`
  - `Fast/E2B results return a blank/null analysisPoint so the frontend does not render the analysis point line.`
  - `Frontend local preview mirrors the mode difference.`
  - `Fast mode remains mapped to gemma-4-e2b-it and precise mode remains mapped to gemma-4-e4b-it.`
- non_goals:
  - `No model deletion, reinstall, quantization change, or fine-tuning.`
  - `No Ollama migration.`
  - `No frontend redesign.`
  - `No deployment or external smoke URL check.`
  - `No fake seed insertion for recommendation candidates.`
- hard_checks:
  - `Update focused prompt/evidence and weak-text replacement tests.`
  - `Run recommendation package tests.`
  - `Run frontend syntax check.`
  - `Run bootJar.`
  - `Run local API benchmark after restart.`
  - `Run git diff --check.`
  - `git diff --check`
  - `git status --short`
- llm_review_rubric:
  - `Data integrity fix does not discard provider-specific raw data.`
  - `Tag heuristic is conservative, explainable, and maintainable.`
  - `Recommendation diversity preserves value/time choices while avoiding same-movie repetition.`
  - `Prompt/output guard blocks internal implementation vocabulary from user-facing copy.`
  - `Fallback path remains deterministic when LM Studio fails.`
- evidence_required:
  - `Record changed files, tests, ingest replay totals, mismatch/tag counts, API matrix status/timing, quality observations, and verification outputs.`

## Verification Results

- precise_only_analysis_point_task:
  - `RecommendationService`: `passes RecommendationMode through item mapping and returns analysisPoint only when mode == PRECISE`
  - `frontend preview`: `local preview now mirrors production behavior; fast has no analysisPoint and precise shows selected-poster genre analysis`
  - `mode card copy`: `precise mode now advertises poster taste analysis instead of removed caution wording`
  - `RecommendationServiceQualityTests`: `fast expectations changed to blank analysisPoint and a precise-mode coverage test was added`
  - `initial focused test run`: `failed because three fast-mode tests still expected #애니메이션취향; fixed expectations and added precise coverage`
  - `gradle test --tests kr.daboyeo.backend.service.recommendation.RecommendationServiceQualityTests --tests kr.daboyeo.backend.service.recommendation.LocalModelRecommendationClientTests`: `passed after test contract update`
  - `gradle test --tests kr.daboyeo.backend.service.recommendation.* --tests kr.daboyeo.backend.repository.recommendation.*`: `passed`
  - `gradle test`: `passed`
  - `gradle bootJar`: `passed`
  - `node --check frontend\src\js\pages\daboyeoAi.js`: `passed`
  - `git diff --check`: `passed with CRLF warnings only`
  - `ERROR_LOG.md`: `recorded resolved fast analysisPoint expectation failure`
  - `local API benchmark on fresh port 18080`: `fast/E2B 5 runs avg=5.93s min=5.52s max=7.09s, all status=ok count=3 analysisCount=0`
  - `local API benchmark on fresh port 18080 precise`: `precise/E4B 5 runs avg=4.65s min=2.01s max=6.92s, all status=ok count=3 analysisCount=3 firstAnalysis=#SF취향`
  - `benchmark cleanup`: `temporary backend process on port 18080 stopped after measurement`

- selected_poster_genre_analysis_task:
  - `RecommendationModels`: `RecommendationItem now includes compatible analysisPoint and TagProfile tracks likedGenres from selected posters`
  - `PreferenceProfileBuilder`: `liked seed poster genres are stored as genre weights and as liked genre signals`
  - `RecommendationService`: `analysisPoint chooses candidate/liked genre overlap first, then selected-poster genre fallback, then candidate genre family fallback`
  - `frontend result card`: `renders 분석 포인트 when analysisPoint is present`
  - `local API precise/E4B sample`: `selected Interstellar/Inception/Dune seed posters produced analysisPoint=#SF취향 with status=ok and 3 recommendations`
  - `gradle test --tests kr.daboyeo.backend.service.recommendation.RecommendationServiceQualityTests --tests kr.daboyeo.backend.service.recommendation.LocalModelRecommendationClientTests`: `passed`
  - `gradle test --tests kr.daboyeo.backend.service.recommendation.* --tests kr.daboyeo.backend.repository.recommendation.*`: `passed`
  - `gradle test`: `passed`
  - `gradle bootJar`: `passed`
  - `node --check frontend\src\js\pages\daboyeoAi.js`: `passed`
  - `backend log scan after API sample`: `no WARN/ERROR/Exception/fail lines found`
  - `git diff --check`: `passed with CRLF warnings only`
  - `repository baseline commands`: `git status --short, Get-Content -Raw WORKSPACE_CONTEXT.toml, Select-String section headers completed`

- e4b_precise_optimization_task:
  - `precise/E4B contract`: `changed LM Studio internal precise response from tag text objects to id-only rerank JSON {"r":[1,2,3]}`
  - `public API compatibility`: `unchanged; RecommendationService maps blank precise AI text to deterministic user-facing tags`
  - `precise token budget`: `default/env example/application.yml changed from 320 -> 96 max tokens`
  - `response text budget`: `default/env example/application.yml changed from 72 -> 56 chars`
  - `prompt payload`: `candidate prompt now keeps id, title, b, vp only; duplicate tg/rt/age/th/time/price/seat fields removed`
  - `reason tag quality`: `content warning tags are excluded from reason source tags; they no longer appear as recommendation reasons`
  - `gradle test --tests kr.daboyeo.backend.service.recommendation.LocalModelRecommendationClientTests --tests kr.daboyeo.backend.service.recommendation.RecommendationServiceQualityTests`: `passed`
  - `gradle test --tests kr.daboyeo.backend.service.recommendation.* --tests kr.daboyeo.backend.repository.recommendation.*`: `passed`
  - `gradle bootJar`: `passed`
  - `gradle test`: `passed`
  - `backend log scan after API sample`: `no WARN/ERROR/Exception/fail lines found`
  - `git diff --check`: `passed with CRLF warnings only`
  - `repository baseline commands`: `git status --short, Get-Content -Raw WORKSPACE_CONTEXT.toml, Select-String section headers completed`
  - `local API precise/E4B final sample`: `3/3 status=ok, count=3, times=7.97s/1.52s/3.20s, avg=4.23s, warmed avg=2.36s`
  - `local API fast/E2B comparison sample`: `status=ok, count=3, time=6.27s`

- user_facing_tag_task:
  - `LocalModelRecommendationClient`: `compact LM prompt now requires why/v as 2-4 Korean hashtags, and candidate b/vp hints carry user-facing tags`
  - `RecommendationService`: `reason/valuePoint now reject generic prose or non-tag output; weak/mixed AI text is replaced or filtered into grounded tags`
  - `reason tag scope`: `keeps taste/audience/mood/age/runtime/genre-style tags and removes value-only tags`
  - `valuePoint tag scope`: `keeps time/price/seat/booking tags and removes taste/reason tags`
  - `gradle test --tests kr.daboyeo.backend.service.recommendation.LocalModelRecommendationClientTests --tests kr.daboyeo.backend.service.recommendation.RecommendationServiceQualityTests`: `passed`
  - `gradle test --tests kr.daboyeo.backend.service.recommendation.* --tests kr.daboyeo.backend.repository.recommendation.*`: `passed`
  - `gradle test`: `passed`
  - `gradle bootJar`: `passed`
  - `git diff --check`: `passed with CRLF warnings only`
  - `backend log scan after API sample`: `no WARN/ERROR/Exception/fail lines found`
  - `local API fast/E2B sample`: `200 ok, 7.16s, firstReason=#신나는 #12세, firstValuePoint=#17:00상영 #좌석여유`
  - `local API precise/E4B sample`: `200 ok, 21.62s, firstReason=#전체관람가 #친구랑, firstValuePoint=#15:50상영 #좌석여유 #예매가능`

- current_latency_trim_task:
  - `precise-ai-candidate-limit`: `default/env example/application.yml changed from 6 to 5 so fast and precise both send 5 candidates max`
  - `AI caution generation`: `removed from LM Studio prompt and strict JSON schema; AiPick missing caution parses with null`
  - `frontend caution rendering`: `removed result-card caution paragraph and unused caution CSS`
  - `API compatibility`: `RecommendationItem.caution field remains in the response shape but is blank when AI does not provide it`
  - `gradle test --tests kr.daboyeo.backend.service.recommendation.* --tests kr.daboyeo.backend.repository.recommendation.*`: `passed`
  - `gradle test`: `passed`
  - `gradle bootJar`: `passed`
  - `node --check frontend\src\js\pages\daboyeoAi.js`: `passed`
  - `git diff --check`: `passed with CRLF warnings only`
  - `frontend Chrome CDP benchmark after trim`: `fast/E2B 5회 avg=15.265s, warmed 2-5 avg=14.019s; precise/E4B 5회 avg=29.057s, warmed 2-5 avg=28.257s; all responses status=ok, count=3, uniqueTitles=3`

- compact_lm_json_task:
  - `LM internal response contract`: `changed to compact {"r":[{"id":showtimeId,"why":"...","v":"..."}]}`
  - `public API compatibility`: `existing RecommendationItem reason/valuePoint/caution fields remain unchanged; compact picks are mapped back to AiPick internally`
  - `AI prompt payload`: `candidate keys shortened to id/t/tg/rt/age/th/time/price/seat and tags are available as factual basis`
  - `LocalModelRecommendationClientTests`: `passed`
  - `gradle test --tests kr.daboyeo.backend.service.recommendation.* --tests kr.daboyeo.backend.repository.recommendation.*`: `passed`
  - `gradle bootJar`: `passed`
  - `git diff --check`: `passed with CRLF warnings only`
  - `repository baseline commands`: `git status --short, Get-Content -Raw WORKSPACE_CONTEXT.toml, Select-String section headers completed`
  - `frontend Chrome CDP benchmark after compact JSON`: `fast/E2B 5회 avg=6.199s, warmed 2-5 avg=5.768s; precise/E4B 5회 avg=22.315s, warmed 2-5 avg=21.576s; all responses status=ok, count=3, uniqueTitles=3`

- current_task_final:
  - `python -m py_compile scripts\ingest\collect_all_to_tidb.py`: `passed`
  - `gradle test --tests kr.daboyeo.backend.service.recommendation.* --tests kr.daboyeo.backend.repository.recommendation.*` from `backend`: `passed after aligning the diversity test with the title-first duplicate policy`
  - `gradle test` from `backend`: `passed`
  - `gradle bootJar` from `backend`: `passed`
  - `python scripts\ingest\collect_all_to_tidb.py --provider all --limit-movies 20 --limit-theaters 20 --limit-schedules 120`: `Lotte 120 showtimes / 22 tags, Megabox 120 showtimes / 130 tags`
  - `python scripts\ingest\collect_all_to_tidb.py --provider megabox --limit-movies 1 --limit-theaters 1 --limit-schedules 1`: `backfilled 2 stale showtime movies and repaired 2 showtime movie links after the new reconciliation step`
  - `DB movie_tags_total`: `158`
  - `DB movie_tags_by_provider_type`: `LOTTE_CINEMA age_rating=20 format=2; MEGABOX age_rating=54 format=40 genre=42`
  - `DB megabox_today_title_mismatches`: `0`
  - `DB megabox_today_multi_title_movie_ids`: `0`
  - `DB all_showtime_movie_link_mismatches`: `0`
  - `local API matrix`: `6/6 fast/precise recommendation requests returned status=ok, count=3, uniqueTitles=3, duplicateTitles=0, internalLeaks=0`
  - `local API latency`: `fast 15.9s-18.8s, precise 35.6s-41.3s on the current school PC/LM Studio runtime`
  - `backend logs after API matrix`: `no WARN/ERROR/Exception/fail lines found`
  - `git diff --check`: `passed with CRLF warnings only`
  - `git status --short`: `reviewed`
  - `Get-Content -Raw WORKSPACE_CONTEXT.toml`: `read; existing mojibake is unrelated to this task`
  - `Select-String -Path WORKSPACE_CONTEXT.toml -Pattern section headers`: `passed`

- unit_tests:
  - `gradle test --tests kr.daboyeo.backend.service.recommendation.* --tests kr.daboyeo.backend.repository.recommendation.*` from `backend`: `passed`
  - `gradle test` from `backend`: `passed`
  - `gradle bootJar` from `backend`: `passed`
- lm_studio_runtime:
  - `lms server start --port 1234 --bind 127.0.0.1`: `running`
  - `scripts/local/load_lmstudio_recommendation_models.ps1`: `loaded both models with context=2048, parallel=1, ttl=3600, gpu=max`
  - `lms ps`: `gemma-4-e2b-it and gemma-4-e4b-it idle, context=2048, parallel=1`
  - `nvidia-smi`: `GTX 1070 Ti visible, 8192 MiB total, about 7400 MiB used after model load`
- real_data_refresh:
  - `python scripts/ingest/collect_all_to_tidb.py --provider all --limit-movies 10 --limit-theaters 10 --limit-schedules 30`: `Lotte 30 showtimes and Megabox 30 showtimes upserted for 2026-04-20`
  - `python scripts/ingest/collect_all_to_tidb.py --provider all --limit-movies 20 --limit-theaters 20 --limit-schedules 120`: `Lotte 120 showtimes and Megabox 120 showtimes upserted for 2026-04-20`
  - `candidate_distribution`: `287 stored showtimes, 230 upcoming after buffer, 29 distinct upcoming titles, movie_tags=0`
  - `provider_distribution`: `Lotte 115 upcoming / 2 titles, Megabox 115 upcoming / 29 titles`
- local_api:
  - `POST /api/recommendations fast`: `status=ok, model=gemma-4-e2b-it, elapsed=5.1s, recommendation_count=3`
  - `POST /api/recommendations precise`: `status=ok, model=gemma-4-e4b-it, elapsed=8.7s, recommendation_count=3`
  - `transient fallback check`: `fast 200 / precise 240 max_tokens caused truncated JSON; resolved with fast 280 / precise 320`
  - `multi_case_matrix`: `14/14 requests returned status=ok, no transport errors, no backend WARN/ERROR, no JSON fallback`
  - `latency_matrix`: `fast avg 4.3s, min 2.5s, max 6.5s; precise avg 8.2s, min 6.5s, max 9.2s`
  - `result_diversity`: `4 unique titles across 42 recommendation items, 6 unique title/time/theater signatures`
  - `quality_findings`: `fast E2B sometimes returned weak reason text such as score/tag fragments; precise E4B wording was better but often repeated similar reasons`
  - `feedback_loop`: `disliking the top precise recommendations was accepted, but the next recommendation did not materially diversify because high scoring/tag scarcity kept the same candidates on top`
- data_quality_findings:
  - `showtimes.movie_title <> movies.title_ko`: `139 rows`
  - `movie_ids_with_multiple_showtime_titles`: `at least 2 movie_id groups; one Megabox movie_id was linked to 30 distinct showtime titles`
  - `impact`: `recommendation title is shown from showtimes, but movieId/feedback identity can be wrong until ingest mapping is fixed`
- repository_checks:
  - `git diff --check`: `passed with CRLF warnings only`
  - `git status --short`: `reviewed`
  - `Get-Content -Raw WORKSPACE_CONTEXT.toml`: `read`
  - `Select-String -Path WORKSPACE_CONTEXT.toml -Pattern section headers`: `passed`
  - `recommendation cleanup`: `recommendation_profiles=0, recommendation_runs=0, recommendation_feedback=0 after test sessions were deleted`

## Retrospective

- task: `Limit selected-poster genre analysis point to precise mode`
- score_total: `4`
- evaluation_fit: `light-to-full fit; small mode gate still needed focused tests because public response compatibility and frontend preview behavior changed`
- orchestration_fit: `single-session fit; backend mode gate, preview alignment, and test expectations were one compact contract adjustment`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `focused tests initially failed because old fast-mode expectations still required analysisPoint; updated tests to match precise-only contract`
- reviewer_findings: `fast/E2B now stays simpler; precise/E4B keeps selected-poster genre analysis as the visible differentiator`
- verification_outcome: `focused tests, recommendation package tests, full gradle test, bootJar, frontend syntax check, and diff check passed`
- next_gate_adjustment: `future UI copy should keep fast simple and reserve richer explanation lines for precise mode`

- task: `Add selected-poster genre analysis point`
- score_total: `6`
- evaluation_fit: `full fit; response contract, deterministic fallback, frontend rendering, and local API sample all needed checking`
- orchestration_fit: `single-session fit; genre signal capture, response mapping, and result-card rendering were tightly coupled`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `task shifted from generic non-caution analysis tags to selected-poster genre analysis after user feedback`
- reviewer_findings: `public response remains compatible; analysisPoint does not use caution/avoidance tags; sparse candidate genre data still limits exact overlap quality`
- verification_outcome: `focused tests, recommendation package tests, full gradle test, bootJar, frontend syntax check, diff check, backend log scan, workspace baseline commands, and local precise API sample passed`
- next_gate_adjustment: `future quality work should improve movie_tags genre coverage so analysisPoint can prefer exact candidate overlap more often`

- task: `Optimize E4B precise recommendation latency`
- score_total: `5`
- evaluation_fit: `full fit; E4B latency changes needed local LM Studio API evidence because token-budget-only tuning did not improve enough`
- orchestration_fit: `single-session fit; runtime config, prompt/schema, service fallback quality, and local benchmark were tightly coupled`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `first pass lowered precise max_tokens to 180 and shortened prompt but still averaged 22.79s; second pass switched precise to id-only rerank and reached 4.23s avg`
- reviewer_findings: `public API stayed compatible; E4B model identity preserved; precise still uses E4B for candidate ordering while server generates grounded tags`
- verification_outcome: `focused tests, recommendation package tests, full gradle test, bootJar, diff check, backend log scan, workspace baseline commands, and fast/precise local API samples passed`
- next_gate_adjustment: `future E4B quality work should tune ranking inputs, not reintroduce generated explanation text unless latency budget allows it`

- task: `Use user-facing recommendation tags`
- score_total: `5`
- evaluation_fit: `full fit; prompt shape, deterministic fallback, and live API output all had to be checked because small local models can still mix tag categories`
- orchestration_fit: `single-session fit; recommendation prompt and service post-processing were tightly coupled and cheaper to repair locally`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `after live fast sample mixed reason tags into valuePoint, added service-side tag category filtering`
- reviewer_findings: `public API shape stayed stable; LM internal JSON stayed compact; internal score/tag-token leakage remains blocked`
- verification_outcome: `focused tests, recommendation package tests, full gradle test, bootJar, diff check, backend log scan, and fast/precise local API samples passed`
- next_gate_adjustment: `future quality tuning should change deterministic tag mapping first, then adjust LM prompt only if the fallback output is already good`

- task: `Optimize local LM Studio recommendation runtime`
- score_total: `6`
- evaluation_fit: `full fit; runtime tuning needed executable API evidence because too-low token settings can silently fall back`
- orchestration_fit: `single-session fit; local LM process state, backend budget tuning, and timing had to be iterated on the same machine`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `token budget was raised after fast 200 / precise 240 truncated JSON`
- reviewer_findings: `model identities preserved; public response shape preserved; fallback path still works; CORS was removed from LM server restart because backend is the caller`
- verification_outcome: `expanded matrix confirmed JSON/API stability after optimization, but exposed result diversity limits and a Megabox movie_id data-integrity issue`
- next_gate_adjustment: `fix ingest movie_id mapping and add movie tags before judging recommendation diversity as an AI problem`

- task: `Fix recommendation data integrity and result quality`
- score_total: `8`
- evaluation_fit: `full fit; real ingest, DB integrity queries, backend tests, and local LM API checks were all needed because the original issue mixed stale data, prompt quality, and ranking behavior`
- orchestration_fit: `delegated-parallel fit; ingest and backend slices were disjoint enough for workers, while main kept reconciliation and integration verification`
- predicted_topology: `delegated-parallel`
- actual_topology: `delegated-parallel plus main integration repair`
- spawn_count: `2`
- rework_or_reclassification: `focused diversity test initially encoded movie_id-first behavior; adjusted to title-first because stale Megabox movie_id data can collapse different titles`
- reviewer_findings: `added cross-run showtime/movie reconciliation because schedule-only upsert fixed new rows but left stale bad links behind`
- verification_outcome: `all hard checks passed; DB link mismatches are now zero and API matrix shows no internal-token leaks or duplicate titles`
- next_gate_adjustment: `for future ingest fixes, include stale-row reconciliation in the first contract instead of verifying only freshly reprocessed rows`

- task: `Trim recommendation LLM output for frontend latency`
- score_total: `5`
- evaluation_fit: `light fit; focused tests and syntax checks were enough because the public response shape stayed compatible`
- orchestration_fit: `single-session fit; backend prompt/schema and one frontend render line were too tightly coupled for delegation`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `none`
- reviewer_findings: `JSON keys were already English; speed win comes from fewer generated Korean value tokens and fewer required schema fields`
- verification_outcome: `candidate limit and caution removal compiled and tests passed`
- next_gate_adjustment: `next latency pass should benchmark after restarting backend with new jar/env and then consider lowering max_tokens`

- task: `Compact LM Studio recommendation response JSON`
- score_total: `4`
- evaluation_fit: `light fit; parser and prompt contract tests plus recommendation package tests covered the changed behavior`
- orchestration_fit: `single-session fit; compact schema, parser, prompt, and tests were one small backend slice`
- predicted_topology: `single-session`
- actual_topology: `single-session`
- spawn_count: `0`
- rework_or_reclassification: `none`
- reviewer_findings: `public API shape stayed stable; compact LM response maps back into existing AiPick flow; candidate prompt now includes tags without score/matchedTags/penalties`
- verification_outcome: `focused tests, recommendation package tests, bootJar, diff check, and workspace baseline commands passed`
- next_gate_adjustment: `restart backend and re-run frontend benchmark before changing token budgets further`

## Writer Slot

- writer_slot: `main`
- write_set: `STATE.md, ERROR_LOG.md, backend/src/main/java/kr/daboyeo/backend/service/recommendation/RecommendationService.java, backend/src/test/java/kr/daboyeo/backend/service/recommendation/RecommendationServiceQualityTests.java, frontend/src/js/pages/daboyeoAi.js`
- write_sets:
  - `worker_ingest`: `scripts/ingest/collect_all_to_tidb.py and related ingest tests/helpers if needed`
  - `worker_backend`: `backend/src/main/java/kr/daboyeo/backend/service/recommendation/** and backend/src/test/java/kr/daboyeo/backend/service/recommendation/**`
  - `main`: `STATE.md, MULTI_AGENT_LOG.md, ERROR_LOG.md, integration review, real-data verification`
- shared_assets_owner: `main`
- note: `One shared task board is active; no concurrent registry mode.`
- concurrent_note: `No parallel writer is active.`

## Contract Freeze

- contract_freeze: `Preserve existing reason/caution/valuePoint fields and keep analysisPoint compatible; precise/E4B returns compact LM JSON objects with id plus AI-generated analysis tag, while fast/E2B returns blank analysisPoint.`
- note: `This task intentionally touches recommendation response mapping, tests, and result-card rendering only; no model file, ingest, deployment, or broad redesign.`
- contract_source: `user request`
- contract_revision: `2026-04-20-ai-generated-precise-analysis-point`
- verification_target: `backend recommendation tests, frontend syntax check, bootJar, diff check, and local API benchmark`

## Reviewer

- reviewer: `main self-review`
- reviewer_target: `AI-generated precise recommendation analysis point`
- reviewer_focus: `existing API compatibility, fast blank analysisPoint, precise AI-generated analysisPoint validation, fallback safety, latency under acceptable demo range`

## Last Update

- timestamp: `2026-04-20 16:23:00 +09:00`
- note: `Reclassified precise analysisPoint to be generated by E4B rather than server-only because user accepts around 10s latency for honest AI analysis.`
