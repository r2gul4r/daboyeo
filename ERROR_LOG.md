## 2026-04-28T00:00:00+09:00
- time: 2026-04-28 KST
- location: backend verification command
- summary: local `gradle test` could not run because `gradle` is not installed in the current environment PATH
- details: resolved by downloading a local Gradle 8.9 distribution, generating wrapper files, and running tests via `gradlew.bat` with `GRADLE_USER_HOME` pointed to `backend/.gradle-user-home`
- status: resolved

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
