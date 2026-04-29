const fs = require('fs/promises');
const path = require('path');
const crypto = require('crypto');
const axios = require('axios');
const cheerio = require('cheerio');

const OUTPUT_DIR = path.resolve(__dirname, '..');
const GOODS_OUTPUT = path.join(OUTPUT_DIR, 'goods.json');
const EVENTS_OUTPUT = path.join(OUTPUT_DIR, 'events.json');

const REQUEST_DELAY_MS = {
  min: 1000,
  max: 3000,
};

const MAX_REQUESTS_PER_HOST = 6;

const REQUEST_HEADERS = {
  'User-Agent':
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 ' +
    '(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 ' +
    'daboyeo-public-crawler/0.1 non-commercial-portfolio',
  Accept: 'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8',
  'Accept-Language': 'ko-KR,ko;q=0.9,en-US;q=0.7,en;q=0.6',
};

const BLOCKED_URL_PATTERNS = [
  /login/i,
  /member/i,
  /mypage/i,
  /mycgv/i,
  /mymegabox/i,
  /auth/i,
  /reservation/i,
  /payment/i,
  /coupon/i,
];

const robotsCache = new Map();
const hostRequestCounts = new Map();

const TARGET_URLS = Object.freeze({
  cgv: Object.freeze([
    'https://m.cgv.co.kr/WebAPP/EventNotiV4/eventMain.aspx?iPage=1&mCode=001',
  ]),
  lotte: Object.freeze(['https://www.lottecinema.co.kr/NLCMW/Event']),
  megabox: Object.freeze(['https://www.megabox.co.kr/event']),
});

const ALLOWED_HOSTS = new Set(
  Object.values(TARGET_URLS)
    .flat()
    .map((url) => getHost(url)),
);

const TEXT = {
  goods: '\uAD7F\uC988',
  poster: '\uD3EC\uC2A4\uD130',
  originalTicket: '\uC624\uB9AC\uC9C0\uB110 \uD2F0\uCF13',
  specialTicket: '\uC2A4\uD398\uC15C \uD2F0\uCF13',
  filmMark: '\uD544\uB984\uB9C8\uD06C',
  artCard: '\uC544\uD2B8\uCE74\uB4DC',
  postcard: '\uC5FD\uC11C',
  sticker: '\uC2A4\uD2F0\uCEE4',
  keyring: '\uD0A4\uB9C1',
  badgeKo1: '\uBC43\uC9C0',
  badgeKo2: '\uBC30\uC9C0',
  giveaway: '\uC99D\uC815',
  prize: '\uACBD\uD488',
  audienceTalk: '\uAD00\uAC1D\uACFC\uC758 \uB300\uD654',
  cinemaTalk: '\uC2DC\uB124\uB9C8\uD1A1',
  liveTalk: '\uB77C\uC774\uBE0C\uD1A1',
  stageGreeting: '\uBB34\uB300\uC778\uC0AC',
  specialStageGreeting: '\uC2A4\uD398\uC15C \uBB34\uB300\uC778\uC0AC',
  done: '\uC885\uB8CC',
  closed: '\uB9C8\uAC10',
  soldOut: '\uC18C\uC9C4',
  pastEvent: '\uC9C0\uB09C \uC774\uBCA4\uD2B8',
  winnerAnnouncement: '\uB2F9\uCCA8\uC790 \uBC1C\uD45C',
  inProgress: '\uC9C4\uD589\uC911',
  place: '\uC7A5\uC18C',
  theater: '\uADF9\uC7A5',
  branch: '\uC9C0\uC810',
  lotteCinema: '\uB86F\uB370\uC2DC\uB124\uB9C8',
  megabox: '\uBA54\uAC00\uBC15\uC2A4',
};

const GOODS_KEYWORDS = [
  TEXT.goods,
  TEXT.poster,
  TEXT.originalTicket,
  TEXT.specialTicket,
  TEXT.filmMark,
  TEXT.artCard,
  TEXT.postcard,
  TEXT.sticker,
  TEXT.keyring,
  TEXT.badgeKo1,
  TEXT.badgeKo2,
  TEXT.giveaway,
  TEXT.prize,
];

const GV_KEYWORDS = ['GV', TEXT.audienceTalk, TEXT.cinemaTalk, TEXT.liveTalk];
const STAGE_GREETING_KEYWORDS = [TEXT.stageGreeting, TEXT.specialStageGreeting];
const EVENT_KEYWORDS = [...GV_KEYWORDS, ...STAGE_GREETING_KEYWORDS];

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function randomDelayMs() {
  const { min, max } = REQUEST_DELAY_MS;
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

async function delayBetweenRequests() {
  await sleep(randomDelayMs());
}

function getOrigin(url) {
  return new URL(url).origin;
}

function getHost(url) {
  return new URL(url).host;
}

function isBlockedByLoginPattern(url) {
  return BLOCKED_URL_PATTERNS.some((pattern) => pattern.test(url));
}

function isAllowedConfiguredHost(url) {
  return ALLOWED_HOSTS.has(getHost(url));
}

function isAllowedPublicUrl(url) {
  return isAllowedConfiguredHost(url) && !isBlockedByLoginPattern(url);
}

function assertRateLimit(url) {
  const host = getHost(url);
  const count = hostRequestCounts.get(host) || 0;
  if (count >= MAX_REQUESTS_PER_HOST) {
    throw new Error(`rate limit reached for ${host}`);
  }
  hostRequestCounts.set(host, count + 1);
}

function normalizeSpace(value) {
  return String(value || '')
    .replace(/\s+/g, ' ')
    .trim();
}

function absoluteUrl(href, baseUrl) {
  if (!href) return baseUrl;
  try {
    return new URL(href, baseUrl).toString();
  } catch (_error) {
    return baseUrl;
  }
}

function includesAny(text, keywords) {
  const source = normalizeSpace(text).toLowerCase();
  return keywords.some((keyword) => source.includes(keyword.toLowerCase()));
}

function nullable(value) {
  const normalized = normalizeSpace(value);
  return normalized || null;
}

function createRecordId(parts) {
  const hash = crypto
    .createHash('sha1')
    .update(parts.map((part) => normalizeSpace(part)).join('|'))
    .digest('hex')
    .slice(0, 12);
  return hash;
}

function inferEventType(text) {
  if (includesAny(text, GV_KEYWORDS)) return 'GV';
  if (includesAny(text, STAGE_GREETING_KEYWORDS)) return TEXT.stageGreeting;
  return '';
}

function inferStatus(text) {
  const source = normalizeSpace(text);
  const donePattern = new RegExp(
    `(${TEXT.done}|${TEXT.closed}|${TEXT.soldOut}|` +
      `${TEXT.pastEvent.replace(' ', '\\s*')}|${TEXT.winnerAnnouncement.replace(' ', '\\s*')})`,
  );
  if (donePattern.test(source)) {
    return 'ended';
  }
  return 'active';
}

function inferMovieName(title) {
  const source = normalizeSpace(title);
  const bracketMatch = source.match(/\[([^\]]{1,80})\]/);
  if (bracketMatch) return normalizeSpace(bracketMatch[1]);

  const quoteMatch = source.match(/[<\u3008\u300E\u300C](.*?)[>\u3009\u300F\u300D]/);
  if (quoteMatch) return normalizeSpace(quoteMatch[1]);

  return null;
}

function inferDateTime(text) {
  const source = normalizeSpace(text);
  const fullDate = source.match(/(20\d{2})[.\-/\uB144\s]+(\d{1,2})[.\-/\uC6D4\s]+(\d{1,2})/);
  const time = inferTime(source);

  if (fullDate) {
    return joinDateTime(formatDate(fullDate[1], fullDate[2], fullDate[3]), time);
  }

  const shortDate = source.match(/(\d{1,2})[.\-/\uC6D4\s]+(\d{1,2})\s*(?:\uC77C)?/);
  if (shortDate) {
    return joinDateTime(formatDate(new Date().getFullYear(), shortDate[1], shortDate[2]), time);
  }

  return null;
}

function formatDate(year, month, day) {
  return [
    String(year).padStart(4, '0'),
    String(month).padStart(2, '0'),
    String(day).padStart(2, '0'),
  ].join('-');
}

function inferTime(text) {
  const source = normalizeSpace(text);
  const timeMatch = source.match(/(?:^|[^0-9])(\d{1,2})[:\uC2DC]\s*(\d{2})?/);
  if (!timeMatch) return null;

  const hour = Number(timeMatch[1]);
  const minute = timeMatch[2] ? Number(timeMatch[2]) : 0;
  if (hour > 23 || minute > 59) return null;

  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}:00`;
}

function joinDateTime(date, time) {
  if (!date) return null;
  return time ? `${date}T${time}` : date;
}

function inferDateRange(text) {
  const source = normalizeSpace(text);
  const fullRange = source.match(
    /(20\d{2})[.\-/\uB144\s]+(\d{1,2})[.\-/\uC6D4\s]+(\d{1,2})\s*(?:~|-|\u2013|\u2014|\uBD80\uD130)\s*(?:(20\d{2})[.\-/\uB144\s]+)?(\d{1,2})[.\-/\uC6D4\s]+(\d{1,2})/,
  );

  if (fullRange) {
    const startYear = fullRange[1];
    const endYear = fullRange[4] || startYear;
    return {
      start_date: formatDate(startYear, fullRange[2], fullRange[3]),
      end_date: formatDate(endYear, fullRange[5], fullRange[6]),
    };
  }

  const singleDate = inferDateTime(source);
  const dateOnly = singleDate ? singleDate.slice(0, 10) : null;
  return {
    start_date: dateOnly,
    end_date: dateOnly,
  };
}

function inferLocation(text) {
  const source = normalizeSpace(text);
  const labelPattern = new RegExp(
    `(?:${TEXT.place}|${TEXT.theater}|${TEXT.branch})\\s*[:\uFF1A]\\s*([^./|]{2,80})`,
  );
  const labelMatch = source.match(labelPattern);
  if (labelMatch) return normalizeSpace(labelMatch[1]);

  const theaterPattern = new RegExp(
    `(CGV[\\uAC00-\\uD7A3A-Za-z0-9()\\s-]{1,24}|` +
      `${TEXT.lotteCinema}[\\uAC00-\\uD7A3A-Za-z0-9()\\s-]{1,24}|` +
      `${TEXT.megabox}[\\uAC00-\\uD7A3A-Za-z0-9()\\s-]{1,24})`,
  );
  const theaterMatch = source.match(theaterPattern);
  if (theaterMatch) return normalizeSpace(theaterMatch[1]);

  return null;
}

function buildGoodsRecord(theater, title, sourceText, url, sourcePageUrl, collectedAt) {
  const normalizedTitle = normalizeSpace(title);
  const movieName = inferMovieName(title) || inferMovieName(sourceText);
  const dateRange = inferDateRange(sourceText);

  return {
    id: createRecordId(['goods', theater, normalizedTitle, movieName, url]),
    theater,
    title: normalizedTitle,
    movie_name: nullable(movieName),
    status: inferStatus(sourceText),
    start_date: dateRange.start_date,
    end_date: dateRange.end_date,
    url,
    source_page_url: sourcePageUrl,
    collected_at: collectedAt,
  };
}

function buildEventRecord(theater, type, title, sourceText, url, sourcePageUrl, collectedAt) {
  const normalizedTitle = normalizeSpace(title);
  const movieName = inferMovieName(title) || inferMovieName(sourceText);
  const date = inferDateTime(sourceText);
  const location = inferLocation(sourceText);

  return {
    id: createRecordId(['event', theater, type, normalizedTitle, movieName, date, location, url]),
    theater,
    type,
    title: normalizedTitle,
    movie_name: nullable(movieName),
    date,
    location: nullable(location),
    url,
    source_page_url: sourcePageUrl,
    collected_at: collectedAt,
  };
}

function dedupeRecords(records, keyFields) {
  const seen = new Set();
  return records.filter((record) => {
    const key = keyFields.map((field) => normalizeSpace(record[field])).join('|');
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

async function fetchHtml(url, options = {}) {
  if (!isAllowedPublicUrl(url)) {
    throw new Error(`URL outside configured public crawl scope skipped: ${url}`);
  }
  if (!(await isAllowedByRobots(url))) {
    throw new Error(`blocked by robots.txt: ${url}`);
  }
  assertRateLimit(url);
  await delayBetweenRequests();

  const response = await axios.get(url, {
    headers: REQUEST_HEADERS,
    timeout: options.timeout || 15000,
    maxRedirects: 5,
    validateStatus: (status) => status >= 200 && status < 400,
  });

  const finalUrl = response.request?.res?.responseUrl || url;
  if (!isAllowedPublicUrl(finalUrl)) {
    throw new Error(`redirected outside configured public crawl scope: ${finalUrl}`);
  }
  if (!(await isAllowedByRobots(finalUrl))) {
    throw new Error(`redirect target blocked by robots.txt: ${finalUrl}`);
  }

  return {
    url: finalUrl,
    html: response.data,
  };
}

async function fetchRobotsText(origin) {
  if (robotsCache.has(origin)) return robotsCache.get(origin);

  const robotsUrl = `${origin}/robots.txt`;
  try {
    await delayBetweenRequests();
    const response = await axios.get(robotsUrl, {
      headers: REQUEST_HEADERS,
      timeout: 10000,
      maxRedirects: 3,
      validateStatus: (status) => status >= 200 && status < 500,
    });

    const text = response.status >= 200 && response.status < 300 ? String(response.data || '') : '';
    robotsCache.set(origin, text);
    return text;
  } catch (error) {
    console.warn(`[warn] robots.txt fetch failed: ${robotsUrl} (${error.message})`);
    robotsCache.set(origin, null);
    return null;
  }
}

async function isAllowedByRobots(url) {
  const parsed = new URL(url);
  const robotsText = await fetchRobotsText(getOrigin(url));
  if (robotsText === null) return false;
  if (!robotsText.trim()) return true;

  const rules = parseRobotsRules(robotsText, REQUEST_HEADERS['User-Agent']);
  const pathWithSearch = `${parsed.pathname}${parsed.search}`;
  const matchedRule = findLongestRobotsRule(rules, pathWithSearch);
  return !matchedRule || matchedRule.type !== 'disallow';
}

function parseRobotsRules(robotsText, userAgent) {
  const lines = robotsText.split(/\r?\n/);
  const ownToken = userAgent.split(/\s+/)[0].toLowerCase();
  const groups = [];
  let currentGroup = null;
  let lastFieldWasUserAgent = false;

  lines.forEach((line) => {
    const cleanLine = line.replace(/#.*/, '').trim();
    if (!cleanLine) return;

    const separatorIndex = cleanLine.indexOf(':');
    if (separatorIndex === -1) return;

    const field = cleanLine.slice(0, separatorIndex).trim().toLowerCase();
    const value = cleanLine.slice(separatorIndex + 1).trim();

    if (field === 'user-agent') {
      if (currentGroup && lastFieldWasUserAgent && currentGroup.rules.length === 0) {
        currentGroup.agents.push(value.toLowerCase());
      } else {
        currentGroup = { agents: [value.toLowerCase()], rules: [] };
        groups.push(currentGroup);
      }
      lastFieldWasUserAgent = true;
      return;
    }

    if (!currentGroup) return;

    if (field === 'allow' || field === 'disallow') {
      currentGroup.rules.push({ type: field, path: value });
    }

    lastFieldWasUserAgent = false;
  });

  return groups
    .filter((group) => group.agents.includes('*') || group.agents.includes(ownToken))
    .flatMap((group) => group.rules)
    .filter((rule) => rule.path !== '');
}

function findLongestRobotsRule(rules, pathWithSearch) {
  return rules
    .filter((rule) => robotsPathMatches(rule.path, pathWithSearch))
    .sort((left, right) => right.path.length - left.path.length)[0];
}

function robotsPathMatches(rulePath, pathWithSearch) {
  const anchored = rulePath.endsWith('$');
  const ruleBody = anchored ? rulePath.slice(0, -1) : rulePath;
  const escaped = ruleBody.replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '.*');
  return new RegExp(`^${escaped}${anchored ? '$' : ''}`).test(pathWithSearch);
}

async function fetchPublicPage(url, options = {}) {
  try {
    return await fetchHtml(url, options);
  } catch (error) {
    console.warn(`[warn] fetch failed: ${url} (${error.message})`);
    return null;
  }
}

function extractCandidates(html, pageUrl) {
  const $ = cheerio.load(html);
  const candidates = [];

  $('a').each((_index, element) => {
    const $link = $(element);
    const href = $link.attr('href');
    const title =
      normalizeSpace($link.attr('title')) ||
      normalizeSpace($link.find('img').attr('alt')) ||
      normalizeSpace($link.text());

    if (!title || title.length < 2) return;

    const containerText = normalizeSpace(
      $link.closest('li, article, div, tr, section').text() || title,
    );

    candidates.push({
      title,
      text: containerText,
      url: absoluteUrl(href, pageUrl),
      source_page_url: pageUrl,
    });
  });

  $('[onclick]').each((_index, element) => {
    const $element = $(element);
    const title =
      normalizeSpace($element.attr('title')) ||
      normalizeSpace($element.find('img').attr('alt')) ||
      normalizeSpace($element.text());
    const onclick = $element.attr('onclick') || '';
    const hrefMatch = onclick.match(/['"]([^'"]*(?:event|Event|evt|detail|Detail)[^'"]*)['"]/);

    if (!title || title.length < 2) return;

    candidates.push({
      title,
      text: normalizeSpace($element.closest('li, article, div, tr, section').text() || title),
      url: absoluteUrl(hrefMatch?.[1], pageUrl),
      source_page_url: pageUrl,
    });
  });

  return dedupeRecords(candidates, ['title', 'url']).filter((candidate) =>
    isAllowedPublicUrl(candidate.url),
  );
}

function parseRecordsFromCandidates(theater, candidates, collectedAt) {
  const goods = [];
  const events = [];

  candidates.forEach((candidate) => {
    const sourceText = normalizeSpace(`${candidate.title} ${candidate.text}`);

    if (includesAny(sourceText, GOODS_KEYWORDS)) {
      goods.push(
        buildGoodsRecord(
          theater,
          candidate.title,
          sourceText,
          candidate.url,
          candidate.source_page_url,
          collectedAt,
        ),
      );
    }

    if (includesAny(sourceText, EVENT_KEYWORDS)) {
      const type = inferEventType(sourceText);
      if (type) {
        events.push(
          buildEventRecord(
            theater,
            type,
            candidate.title,
            sourceText,
            candidate.url,
            candidate.source_page_url,
            collectedAt,
          ),
        );
      }
    }
  });

  return { goods, events };
}

async function crawlTheater(theater, urls) {
  const allCandidates = [];
  const collectedAt = new Date().toISOString();

  for (const url of urls) {
    const page = await fetchPublicPage(url);
    if (!page) continue;
    allCandidates.push(...extractCandidates(page.html, page.url));
  }

  const parsed = parseRecordsFromCandidates(theater, allCandidates, collectedAt);

  return {
    goods: parsed.goods,
    events: parsed.events,
  };
}

async function cgvCrawler() {
  return crawlTheater('cgv', TARGET_URLS.cgv);
}

async function lotteCrawler() {
  return crawlTheater('lotte', TARGET_URLS.lotte);
}

async function megaboxCrawler() {
  return crawlTheater('megabox', TARGET_URLS.megabox);
}

async function runCrawler() {
  const crawlerTasks = [
    ['cgv', cgvCrawler],
    ['lotte', lotteCrawler],
    ['megabox', megaboxCrawler],
  ];

  const goods = [];
  const events = [];

  for (const [theater, crawler] of crawlerTasks) {
    try {
      console.log(`[info] crawling ${theater}`);
      const result = await crawler();
      goods.push(...result.goods);
      events.push(...result.events);
      console.log(
        `[info] ${theater} done: goods=${result.goods.length}, events=${result.events.length}`,
      );
    } catch (error) {
      console.warn(`[warn] ${theater} crawler failed: ${error.message}`);
    }
  }

  const uniqueGoods = dedupeRecords(goods, ['id']);
  const uniqueEvents = dedupeRecords(events, [
    'id',
  ]);

  await fs.writeFile(GOODS_OUTPUT, `${JSON.stringify(uniqueGoods, null, 2)}\n`, 'utf8');
  await fs.writeFile(EVENTS_OUTPUT, `${JSON.stringify(uniqueEvents, null, 2)}\n`, 'utf8');

  console.log(`[info] wrote ${path.relative(process.cwd(), GOODS_OUTPUT)} (${uniqueGoods.length})`);
  console.log(`[info] wrote ${path.relative(process.cwd(), EVENTS_OUTPUT)} (${uniqueEvents.length})`);
}

if (require.main === module) {
  runCrawler().catch((error) => {
    console.error(`[error] crawler failed: ${error.stack || error.message}`);
    process.exitCode = 1;
  });
}

module.exports = {
  TARGET_URLS,
  cgvCrawler,
  lotteCrawler,
  megaboxCrawler,
  runCrawler,
  parseRecordsFromCandidates,
  dedupeRecords,
  inferDateRange,
  inferDateTime,
};
