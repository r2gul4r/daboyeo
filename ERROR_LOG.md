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
- details: rebuilt the backend with the new `TheaterLocationEnricher` patch and launched a temporary instance with `DABOYEO_SHOWTIME_STARTUP_ENABLED=true` and `DABOYEO_SHOWTIME_DATE_OFFSETS=0`. The app reached `Showtime sync starting ... offsets=[0]` and spawned a Python collector process, but after several minutes it produced no `stored`, `cleanup`, or `completed` logs, so the run was treated as stalled. To reflect the Java-side region inference immediately for existing data, a one-off TiDB update matched `LOTTE_CINEMA` theaters against `frontend/src/map/theaters.json`, filled usable theater addresses, set inferred `region_name`, and propagated that value into missing `showtimes.region_name`. The backfill updated `75` theater rows and `170` showtime rows; for today's Lotte showtimes the share with non-empty `region_name` improved from `0/288` to `239/288` (`서울=235`, `경기=4`, `UNKNOWN=49`).
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
- summary: the homepage `직접 비교하기` and `내 위치` button flows were repaired for the local runtime
- details: rewrote `frontend/src/js/pages/script.js` to initialize reliably even when the script runs after `DOMContentLoaded`, exposed `window.openMovieComparison` as a stable fallback target, and added an inline click fallback on the homepage button. Rewrote `frontend/src/js/api/kakaoMap.js` so the location button still opens the modal and shows a clear fallback message even when Kakao Maps/services does not initialize. Browser verification confirmed that `직접 비교하기` now navigates to `movies.html?...` and `내 위치` opens the modal with a diagnostic status message instead of silently doing nothing.
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
- summary: the `직접 비교하기` flow could not load collected data because the backend API was not actually listening on `localhost:8080`
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
- details: direct collector execution showed `CgvCollector.build_site_records()` failing with `HTTP Error 401: Unauthorized`, while the active `LOTTE_CINEMA` discovery logic in `PythonCollectorBridge` filtered cinemas down to preferred IDs `3037` and `9111`, then stopped after the first working movie per cinema. The resulting discovered targets for `2026-04-29` were only `위례 -> 왕과 사는 남자` and `하남미사 -> 프로젝트 헤일메리`, which matches the two Lotte showtimes seen in TiDB and explains why Gangnam-area live search is dominated by Megabox.
- status: open

## 2026-04-29T18:20:00+09:00
- time: 2026-04-29 KST
- location: LOTTE auto-discovery patch verification
- summary: `LOTTE_CINEMA` startup sync now fans out across more cinema/movie targets instead of staying pinned to two preferred single-movie bundles
- details: removed the hardcoded preferred-cinema defaults, raised the default Lotte cinema discovery breadth, added a per-cinema movie target limit, updated the discovery script to emit multiple working movie targets per cinema, and passed `ShowtimeSyncServiceTests`. After rebuilding and restarting the backend, startup logs showed multiple `LOTTE_CINEMA` bundle persists for `2026-04-29` with `showtimes=3`, `showtimes=5`, and `showtimes=2`, and TiDB rows now include theaters such as `가산디지털` in addition to the earlier `위례` and `하남미사`.
- status: resolved
