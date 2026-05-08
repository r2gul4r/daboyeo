## 2026-04-28T00:00:00+09:00
- time: 2026-04-28 KST
- location: backend verification command
- summary: local `gradle test` could not run because `gradle` is not installed in the current environment PATH
- details: resolved by downloading a local Gradle 8.9 distribution, generating wrapper files, and running tests via `gradlew.bat` with `GRADLE_USER_HOME` pointed to `backend/.gradle-user-home`
- status: resolved

## 2026-04-30T12:45:00+09:00
- time: 2026-04-30 KST
- location: LOTTE manual re-sync attempt
- summary: a one-off startup sync on port `8083` stalled inside the Lotte discovery phase before any new `stored` logs appeared, so the current Lotte rows were backfilled directly from theater-map metadata instead
- details: rebuilt the backend with the new `TheaterLocationEnricher` patch and launched a temporary instance with `DABOYEO_SHOWTIME_STARTUP_ENABLED=true` and `DABOYEO_SHOWTIME_DATE_OFFSETS=0`. The app reached `Showtime sync starting ... offsets=[0]` and spawned a Python collector process, but after several minutes it produced no `stored`, `cleanup`, or `completed` logs, so the run was treated as stalled. To reflect the Java-side region inference immediately for existing data, a one-off TiDB update matched `LOTTE_CINEMA` theaters against `frontend/src/map/theaters.json`, filled usable theater addresses, set inferred `region_name`, and propagated that value into missing `showtimes.region_name`. The backfill updated `75` theater rows and `170` showtime rows; for today's Lotte showtimes the share with non-empty `region_name` improved from `0/288` to `239/288` (`?쒖슱=235`, `寃쎄린=4`, `UNKNOWN=49`).
- status: open

## 2026-04-30T10:38:00+09:00
- time: 2026-04-30 KST
- location: backend startup sync verification
- summary: startup sync on the rebuilt 3-day backend began persisting showtimes, but one TiDB connection dropped mid-run so end-to-end completion and cleanup could not yet be confirmed
- details: after rebuilding the backend JAR and starting it locally, logs showed `Showtime sync starting ... offsets=[0, 1, 2]` and multiple `Showtime sync stored provider=LOTTE_CINEMA ... showtimes=...` entries, confirming startup-triggered DB writes with the new 3-day window. During the same run, `CollectorBundlePersistenceService.persist()` hit `Communications link failure` and `EOFException` while toggling auto-commit on a TiDB connection, and no final `Showtime sync completed` or cleanup log had appeared by the time verification paused.
- status: open

## 2026-04-28T16:40:00+09:00
- time: 2026-04-28 KST
- location: local frontend and backend runtime verification
- summary: the frontend could not fetch backend data from port `4173`, and the Kakao SDK script failed to initialize during browser verification
- details: backend health and `/api/live/nearby` responded normally on `http://localhost:8080`, but the frontend origin `http://127.0.0.1:4173` was not included in `DABOYEO_FRONTEND_ORIGINS`, so browser fetches failed there until the frontend was served on allowed port `5173`. For the Kakao issue, the browser console first logged `window.kakao` as present and then hit the guard `!window.kakao || !kakao.maps || !kakao.maps.services`, which means the global object existed but the Maps/services modules did not initialize. That points more strongly to a Kakao app-key or allowed-domain configuration mismatch than to a pure frontend code bug.
- status: open

## 2026-04-28T16:45:00+09:00
- time: 2026-04-28 KST
- location: backend CORS follow-up verification
- summary: local frontend port `4173` is now accepted by the backend after resource rebuild and restart
- details: updated the default CORS origin list in `application.yml`, `BackendCorsProperties`, and `.env.example`, ran `backend\\gradlew.bat classes`, restarted the backend, and verified that a GET request with `Origin: http://127.0.0.1:4173` now returns `Access-Control-Allow-Origin: http://127.0.0.1:4173`. The in-app browser on `http://127.0.0.1:4173/movies.html` no longer showed the earlier backend connection failure state.
- status: resolved

## 2026-04-28T16:52:00+09:00
- time: 2026-04-28 KST
- location: homepage interaction repair
- summary: the homepage `吏곸젒 鍮꾧탳?섍린` and `???꾩튂` button flows were repaired for the local runtime
- details: rewrote `frontend/src/js/pages/script.js` to initialize reliably even when the script runs after `DOMContentLoaded`, exposed `window.openMovieComparison` as a stable fallback target, and added an inline click fallback on the homepage button. Rewrote `frontend/src/js/api/kakaoMap.js` so the location button still opens the modal and shows a clear fallback message even when Kakao Maps/services does not initialize. Browser verification confirmed that `吏곸젒 鍮꾧탳?섍린` now navigates to `movies.html?...` and `???꾩튂` opens the modal with a diagnostic status message instead of silently doing nothing.
- status: resolved

## 2026-04-28T17:20:00+09:00
- time: 2026-04-28 KST
- location: movie theater map page
- summary: nearby theater markers and list rendering were restored on the theater map page
- details: the inline Kakao map script in `frontend/src/basic/movieTheaterMap.html` was crashing because it tried to write into a missing `#placesList` node, and its brand filter selector targeted `.brand-chip` while the markup used different classes. The page was updated so the list container and filter buttons match the script, marker cleanup and list rendering were stabilized, and browser verification showed theater cards plus multiple map markers rendering together.
- status: resolved

## 2026-04-29T11:58:00+09:00
- time: 2026-04-29 KST
- location: backend startup auto-discovery sync
- summary: startup sync hung before persistence because discovery subprocesses were writing oversized JSON payloads to stdout
- details: after adding automatic Lotte/Megabox discovery, the backend reached `Showtime sync starting...` and then stopped making progress without `stored` or `completed` logs. The likely cause was the Python discovery helper serializing full collector rows including `raw` objects for every movie and theater, which can block on the subprocess stdout pipe before the parent reads it. The discovery script output was reduced to only the minimal fields needed for target selection so startup sync can continue.
- status: resolved

## 2026-04-29T16:23:43+09:00
- time: 2026-04-29 KST
- location: local live movie compare investigation
- summary: the `吏곸젒 鍮꾧탳?섍린` flow could not load collected data because the backend API was not actually listening on `localhost:8080`
- details: frontend access logs showed navigation to `movies.html?region=...`, but local requests to `http://localhost:8080/api/live/nearby` failed with connection errors and port `8080` had no listening process. Existing backend boot logs in `backend/build/tmp/backend-bootrun.err.log` and `backend/build/tmp/bootrun-direct.err.log` show Gradle startup failing with `Could not initialize native services` and `Failed to load native library 'native-platform.dll'`. Separate TiDB verification still showed collected data present (`showtimes=1892`), and an additional DB check showed only `715` showtimes currently join to theaters with usable coordinates, which is a secondary data-availability risk after the backend startup issue is fixed.
- status: open

## 2026-04-29T16:46:00+09:00
- time: 2026-04-29 KST
- location: live movie night-search follow-up
- summary: Seoul Gangnam same-day night search now returns collected data instead of zero results
- details: patched the frontend `movies.html` filters to treat `17:00 ~ 06:00` as a cross-midnight range, added shared backend time-range helpers so demo fallback uses the same rule, rebuilt the backend JAR, and verified both the API and the in-app browser. `GET /api/live/nearby?lat=37.517331925853&lng=127.047377408384&date=2026-04-29&timeStart=17:00&timeEnd=06:00` now returns `resultCount=1`, and the reloaded browser page at `http://localhost:5500/movies.html?...` shows `resultCount=1`.
- status: resolved

## 2026-04-29T17:25:00+09:00
- time: 2026-04-29 KST
- location: Megabox showtime ingest repair
- summary: Megabox `showtimes.theater_id` null links were traced to ingest paths that did not always repair theater/screen joins after upsert, and one legacy script also omitted `theater_id` plus Megabox `theater_no` mapping
- details: added post-upsert showtime link repair to the backend collector persistence path and the canonical Python ingest script, patched the legacy `scripts/db/collect_all_showtimes.py` parser to populate `theater_id` and map Megabox `theater_no` into `external_screen_id`, rebuilt the backend JAR, and ran a one-time TiDB repair update. After the repair, `MEGABOX` `showtimes.theater_id` null count dropped from `1148` to `0`, and the same Gangnam-area live API query for `2026-04-29 06:00~23:59` now returns `resultCount=136`.
- status: resolved

## 2026-04-29T18:05:00+09:00
- time: 2026-04-29 KST
- location: multi-provider showtime coverage investigation
- summary: `CGV` live showtime collection is currently blocked by an upstream `401 Unauthorized`, and `LOTTE_CINEMA` auto-discovery is only sampling two preferred cinemas with one representation movie each
- details: direct collector execution showed `CgvCollector.build_site_records()` failing with `HTTP Error 401: Unauthorized`, while the active `LOTTE_CINEMA` discovery logic in `PythonCollectorBridge` filtered cinemas down to preferred IDs `3037` and `9111`, then stopped after the first working movie per cinema. The resulting discovered targets for `2026-04-29` were only `?꾨? -> ?뺢낵 ?щ뒗 ?⑥옄` and `?섎궓誘몄궗 -> ?꾨줈?앺듃 ?ㅼ씪硫붾━`, which matches the two Lotte showtimes seen in TiDB and explains why Gangnam-area live search is dominated by Megabox.
- status: open

## 2026-04-29T18:20:00+09:00
- time: 2026-04-29 KST
- location: LOTTE auto-discovery patch verification
- summary: `LOTTE_CINEMA` startup sync now fans out across more cinema/movie targets instead of staying pinned to two preferred single-movie bundles
- details: removed the hardcoded preferred-cinema defaults, raised the default Lotte cinema discovery breadth, added a per-cinema movie target limit, updated the discovery script to emit multiple working movie targets per cinema, and passed `ShowtimeSyncServiceTests`. After rebuilding and restarting the backend, startup logs showed multiple `LOTTE_CINEMA` bundle persists for `2026-04-29` with `showtimes=3`, `showtimes=5`, and `showtimes=2`, and TiDB rows now include theaters such as `媛?곕뵒吏?? in addition to the earlier `?꾨?` and `?섎궓誘몄궗`.
- status: resolved

## 2026-04-30T16:57:40+09:00
- time: 2026-04-30 KST
- location: backend Gradle verification in Codex sandbox
- summary: in-sandbox Gradle verification failed for environment reasons even though the backend verification passed outside the sandbox
- details: `gradlew.bat` first failed trying to create a wrapper lock under `C:\Users\CodexSandboxOffline\.gradle`, and direct execution of the local `backend\.gradle-dist\gradle-8.9\bin\gradle.bat` then failed with `Could not initialize native services` and `Failed to load native library 'native-platform.dll'`. Re-running the repository's existing focused verification outside the sandbox succeeded, so the remaining red squiggles are more likely to be IDE sync/cache issues than Java compile failures in the current backend sources.
- status: resolved

## 2026-05-04T15:48:00+09:00
- time: 2026-05-04 KST
- location: backend restart for Megabox nearby refresh verification
- summary: PowerShell `Start-Process` could not launch the rebuilt backend JAR because the process environment exposed duplicate `Path`/`PATH` keys
- details: after stopping the previous backend process, two attempts to start the rebuilt JAR with `Start-Process` failed with `ArgumentException: item has already been added. Key in dictionary: 'Path' Key being added: 'PATH'`. The issue is isolated to the PowerShell launcher environment, so restart verification continued with an alternate process launch path.
- status: open

## 2026-05-04T15:59:00+09:00
- time: 2026-05-04 KST
- location: backend/frontend local restart
- summary: backend and frontend servers were started successfully after launching them outside the sandboxed process tree
- details: the backend JAR is listening on port `8080` with PID `3144`, `/api/health` returns `status=ok`, and the frontend static server responds on `http://localhost:5500/movies.html`. This resolves the earlier process-launch blocker for the current verification run.
- status: resolved

## 2026-05-06T15:40:00+09:00
- time: 2026-05-06 KST
- location: `collectors/cgv_mh/collector.py` live verification
- summary: the new API-free `cgv-mh` collector fetched the live CGV theaters page successfully but did not extract any theater records from the current page structure
- details: running a direct Python check against `CgvMhCollector.build_theater_records()` returned `count=0`, while `fetch_theaters_page()` succeeded with an HTML payload length of `79101`. This shows external access is working and the remaining blocker is parser compatibility with CGV's newer Next.js page data layout rather than network connectivity.
- status: open

## 2026-05-06T17:15:00+09:00
- time: 2026-05-06 KST
- location: `collectors/cgv_mh/collector.py` live endpoint verification
- summary: CGV's current web-backed theater and showtime endpoints require a signed request plus an `accessToken` cookie that is not available to an anonymous session in this environment
- details: bundle inspection confirmed the live frontend signs `api.cgv.co.kr` requests with `X-TIMESTAMP` and `X-SIGNATURE`, and local verification reproduced that signature with a browser-like TLS client. The Cloudflare `403` layer was bypassed with `curl_cffi`, but every theater/showtime endpoint still returned `401 Unauthorized3` without an `accessToken` cookie. Anonymous calls to `oidc.cgv.co.kr/common/auth/refreshtoken` also returned `-1001/-1002`, so the remaining blocker is upstream auth, not HTML parsing.
- status: open

## 2026-05-07T10:08:00+09:00
- time: 2026-05-07 KST
- location: `collectors/cgv-mh.py` direct CLI run
- summary: the CGV collector CLI still fails immediately without `CGV_ACCESS_TOKEN`
- details: running `python .\collectors\cgv-mh.py --theater-code 0056 --play-date 20260507` exited with `CGV_ACCESS_TOKEN is required for the current CGV web flow. The live browser endpoints return 401 without the accessToken cookie.` This confirms the current blocker remains upstream auth input, not local script bootstrapping.
- status: open

## 2026-05-07T10:17:00+09:00
- time: 2026-05-07 KST
- location: `collectors/cgv_mh/collector.py` schedule fetch
- summary: the collector now reads the CGV token from `.env` and passes auth, but schedule collection fails because `/cnm/atkt/searchMovScnInfo` is missing a required request parameter
- details: running `python .\collectors\cgv-mh.py --theater-code 0056 --play-date 20260507` progressed past auth and then raised `RuntimeError: CGV endpoint /cnm/atkt/searchMovScnInfo failed: 400 諛쒕ℓ?듭젣踰붿쐞肄붾뱶???꾩닔 ?붿껌 ?뚮씪誘명꽣 ?낅땲??` The current blocker is request shape for `searchMovScnInfo`, not token loading.
- status: open

## 2026-05-07T10:51:17+09:00
- time: 2026-05-07 KST
- location: nearby live runtime verification for `CGV,LOTTE,MEGA`
- summary: the latest backend wiring includes CGV in the refresh-backed provider path, but Gangnam nearby runtime still resolves `cgvCandidates=0` so only Lotte data surfaced in the verified API response
- details: after rebuilding `bootJar`, a fresh local backend run answered `/api/live/nearby?lat=37.4979&lng=127.0276&date=2026-05-07&providers=CGV,LOTTE,MEGA&limit=100` with `pendingRefresh=true` and warning `CGV, MEGA showtimes are still being collected for this area. retry shortly.` The refresh logs from the same run showed `Nearby refresh requested date=2026-05-07 cgvCandidates=0 lotteCandidates=1 megaboxCandidates=3 radiusKm=8`, then only Lotte discovery/storage logs. This means the CGV collector bridge is wired in code, but the current nearby theater source does not yield any CGV targets for that search yet.
- status: open

## 2026-05-07T11:21:20+09:00
- time: 2026-05-07 KST
- location: nearby live runtime verification for `CGV,LOTTE,MEGA`
- summary: the CGV nearby candidate and metadata-source regression is resolved
- details: `TheaterMapService` now merges DB theater rows with the checked-in theater map fallback, and `refreshCgv()` no longer depends on pre-existing `theaters` metadata rows before issuing a collector request. A fresh local backend run for `/api/live/nearby?lat=37.4979&lng=127.0276&date=2026-05-07&providers=CGV,LOTTE,MEGA&limit=100` logged `cgvCandidates=1`, `Nearby refresh collecting provider=CGV theater=0056`, and `Nearby refresh stored provider=CGV date=2026-05-07 theaters=1 screens=5 showtimes=23`. The second response returned `34` rows with provider counts `CGV=23` and `LOTTE=11`; only Megabox remained pending in that run.
- status: resolved

## 2026-05-07T11:45:15+09:00
- time: 2026-05-07 KST
- location: Donggyo-dong local nearby search investigation
- summary: `?쒖슱 留덊룷援??숆탳?? nearby searches currently collapse to one frontend movie card because only LOTTE schedules are returning, while CGV remains pending and MEGABOX refresh is failing upstream
- details: local verification on `http://127.0.0.1:5500/movies.html?region=?쒖슱%20留덊룷援?20?숆탳??lat=37.5571&lng=126.9235&date=2026-05-07&timeStart=11:00&timeEnd=16:59` showed `result-count=1` and one visible card. The matching backend API response for `lat=37.5571&lng=126.9235` returned `resultCount=3`, but all three rows were LOTTE schedules for the same movie (`?대ぉ吏`), so the frontend correctly aggregated them into a single card. Backend logs from the same run repeatedly warned `CGV, MEGA showtimes are still being collected for this area. retry shortly.`, and `NearbyShowtimeRefreshService` logged `Python collector failed` for `MEGABOX` JSON decode errors during nearby refresh. This means the visible one-card result is driven by incomplete upstream provider coverage, not by the frontend count logic itself.
- status: open

## 2026-05-07T15:48:00+09:00
- time: 2026-05-07 KST
- location: `collectors/__init__.py` during nearby live refresh
- summary: LOTTE and MEGABOX nearby refreshes regressed because the root `collectors` package still imports the removed `collectors.cgv` module
- details: local backend logs for direct-compare requests showed both `collectLotteNearbyDiscovery` and `collectMegaboxNearbyDiscovery` failing before any provider-specific logic ran, with `ModuleNotFoundError: No module named 'collectors.cgv'` raised from `collectors/__init__.py`. Because Python executes the package root before `collectors.lotte.collector` or `collectors.megabox.collector`, this stale import blocks both providers. The fix is to remove the obsolete CGV package imports at the root package level.
- status: resolved

## 2026-05-08T09:56:18+09:00
- time: 2026-05-08 KST
- location: Render backend deploy / Spring Flyway startup
- summary: backend deploy fails because Flyway meets a non-empty `daboyeo` schema without `flyway_schema_history`, while the packaged backend migration set is also out of sync with repository migration versions
- details: startup aborts with `BeanCreationException` from `flywayInitializer` and the message `Found non-empty schema(s) 'daboyeo' but no schema history table`. Local inspection also showed `backend/src/main/resources/db/migration` only packaged `V004__anonymous_recommendations.sql` and a custom `V005__recommendation_feedback_guards.sql`, while repository migration history under `db/migrations` contains `001` through `005` with a different `005` contract. This blocks safe deployment because Flyway cannot baseline intentionally and the packaged version sequence does not match the schema source of truth.
- status: resolved

## 2026-05-08T10:10:00+09:00
- time: 2026-05-08 KST
- location: Render Docker runtime config
- summary: the container entrypoint passes the literal string `${PORT}` instead of Render's injected numeric port, which can break Spring Boot startup or health checks even after the Flyway fix
- details: the current Dockerfile uses JSON exec form `ENTRYPOINT ["java", "-Dserver.port=${PORT}", ...]`, but environment variable substitution does not happen in that form. Render injects `PORT`, but Java receives the raw text `${PORT}` unless a shell expands it first. The repair is to let Spring read `${PORT:8080}` from configuration or switch the entrypoint to shell-based expansion.
- status: resolved

## 2026-05-08T12:48:00+09:00
- time: 2026-05-08 KST
- location: local event frontend and `/api/events` verification
- summary: the frontend event page wiring loads, but the event data path is currently broken because `/api/events` returns `503 DATA_UNAVAILABLE`
- details: local verification passed for `backend\\gradlew.bat test`, local backend boot on `http://127.0.0.1:8080`, and health check `GET /api/health`. Static frontend serving on `http://127.0.0.1:5173` also loaded `index.html` and `src/pages/events.html`. However, `GET /api/events` and `GET /api/events?source=LOTTE&category=HOT` both returned `503` with body `{"code":"DATA_UNAVAILABLE","message":"Could not load data. Check collector or database status.","details":["BadSqlGrammarException"],...}`. Browser verification confirmed the homepage event preview logs `[indexEvents] failed to load event preview` and falls back to `.event-preview-empty`, while `src/pages/events.html` logs `[events] failed to load events` and shows `#error-state`. This points to a backend schema/query issue on the new `movie_events` path rather than a frontend wiring failure.
- status: resolved

## 2026-05-08T13:00:00+09:00
- time: 2026-05-08 KST
- location: `MovieEventService` event query fallback
- summary: `/api/events` now serves live event cards even when the `movie_events` table is missing or not migrated locally
- details: `MovieEventService` now catches repository `DataAccessException` during event reads and falls back to the configured `MovieEventCrawler` list, filtering the crawled results by source and category when needed. After rebuilding the backend JAR and restarting the local server, `GET /api/events` and `GET /api/events?source=LOTTE&category=HOT` both returned `200` with live LOTTE Cinema event payloads instead of `503`. Browser verification confirmed the homepage preview renders 4 cards and `src/pages/events.html` renders 6 cards with no visible error state.
- status: resolved

## 2026-05-08T15:05:00+09:00
- time: 2026-05-08 KST
- location: `collectors/megabox/api.py` nearby schedule fetch
- summary: MEGABOX nearby collection is being throttled by the upstream `schedulePage.do` endpoint, so nearby searches can stall with LOTTE-only results until the external overload clears
- details: local logs for the Suwon Station night search showed `cgvCandidates=0` by design and repeated MEGABOX failures with `Megabox API returned invalid response ... Workload is so high. Please, try again later!`. The collector client was hardened to treat that response as a retryable overload, extending retries from 5 to 7 attempts and applying stronger session refresh plus longer backoff. Python syntax verification passed, but a follow-up nearby request still returned LOTTE-only results, which means the mitigation is in place but the upstream throttle was still active during verification.
- status: open
