# goods_events

Local Node.js crawler for public CGV, Lotte Cinema, and Megabox event pages.
It writes goods data to `goods.json` and GV/stage-greeting data to `events.json`.

## Run

```powershell
npm install
npm start
```

Static public pages are fetched with `axios` and parsed with `cheerio`. Browser
automation is intentionally not used because it can trigger many secondary requests
for scripts, images, and tracking resources.

## Compliance Guardrails

- Checks each origin's `robots.txt` before fetching crawl targets.
- Skips login, member, reservation, payment, coupon, and auth-like URLs.
- Waits a randomized 1-3 seconds before requests.
- Stops after `MAX_REQUESTS_PER_HOST` requests per host in one run.
- Fetches only the explicitly configured event-list URLs in `TARGET_URLS`.
- Does not follow candidate links for detail-page enrichment.
- Stores only summary fields and source URLs. Images and full detail text are not saved.
- Intended only for non-commercial personal portfolio use.

## Output

- `goods.json`: `id`, `theater`, `title`, `movie_name`, `status`, `start_date`, `end_date`, `url`, `source_page_url`, `collected_at`
- `events.json`: `id`, `theater`, `type`, `title`, `movie_name`, `date`, `location`, `url`, `source_page_url`, `collected_at`

Unknown optional values are stored as `null`, not empty strings. `status` is normalized
to `active` or `ended`.

The crawler is public-page only. It does not use login state, cookies, tokens, or
private APIs. If `robots.txt` cannot be fetched, that origin is skipped.
