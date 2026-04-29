import { initGoodsView, renderGoodsView } from './goods.js';
import { initEventsView, renderEventsView } from './events.js';

const DATA_PATHS = {
  goods: './goods.json',
  events: './events.json',
};

const MIN_LOADING_MS = 700;

const TEXT = {
  latestCollected: '\uCD5C\uC2E0 \uC218\uC9D1',
  noCollectionInfo: '\uC218\uC9D1 \uC815\uBCF4 \uC5C6\uC74C',
  partialLoadFailed: '\uC77C\uBD80 \uB370\uC774\uD130 \uB85C\uB4DC \uC2E4\uD328',
  loadFailed: '\uB370\uC774\uD130\uB97C \uBD88\uB7EC\uC62C \uC218 \uC5C6\uC74C',
  activeGoods: '\uC9C4\uD589\uC911 \uAD7F\uC988',
  upcomingEvents: '\uC608\uC815 \uC774\uBCA4\uD2B8',
  unknown: '\uC815\uBCF4 \uC5C6\uC74C',
  original: '\uC6D0\uBCF8 \uBCF4\uAE30',
};

const state = {
  goods: [],
  events: [],
  errors: {
    goods: null,
    events: null,
  },
};

const elements = {
  dataUpdated: document.querySelector('#data_updated'),
  tabs: Array.from(document.querySelectorAll('.tab_button')),
  panels: Array.from(document.querySelectorAll('.panel')),
  activeGoodsMetric: document.querySelector('#active_goods_metric'),
  upcomingEventsMetric: document.querySelector('#upcoming_events_metric'),
  popularEventsList: document.querySelector('#popular_events_list'),
  activeGoodsList: document.querySelector('#active_goods_list'),
};

document.addEventListener('DOMContentLoaded', initPage);

async function initPage() {
  bindTabs();
  initGoodsView({ onChange: () => renderGoodsView(state.goods, { error: state.errors.goods }) });
  initEventsView({ onChange: () => renderEventsView(state.events, { error: state.errors.events }) });

  await Promise.all([loadAllData(), wait(MIN_LOADING_MS)]);
  renderGoodsView(state.goods, { error: state.errors.goods });
  renderEventsView(state.events, { error: state.errors.events });
  renderHighlights();
  updateDataMeta([...state.goods, ...state.events]);
}

function bindTabs() {
  elements.tabs.forEach((tab) => {
    tab.addEventListener('click', () => {
      setActiveTab(tab.dataset.tabTarget);
    });
  });
}

function setActiveTab(targetId) {
  elements.tabs.forEach((tab) => {
    tab.classList.toggle('is_active', tab.dataset.tabTarget === targetId);
  });

  elements.panels.forEach((panel) => {
    panel.classList.toggle('is_active', panel.id === targetId);
  });
}

async function loadAllData() {
  const [goodsResult, eventsResult] = await Promise.allSettled([
    loadJson(DATA_PATHS.goods),
    loadJson(DATA_PATHS.events),
  ]);

  state.goods = getLoadedArray(goodsResult, 'goods');
  state.events = getLoadedArray(eventsResult, 'events');
}

async function loadJson(url) {
  const response = await fetch(url, { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`Failed to load ${url}: ${response.status}`);
  }

  const data = await response.json();
  return Array.isArray(data) ? data : [];
}

function getLoadedArray(result, key) {
  if (result.status === 'fulfilled') {
    state.errors[key] = null;
    return result.value;
  }

  console.warn(result.reason);
  state.errors[key] = result.reason;
  return [];
}

function wait(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function renderHighlights() {
  const activeGoods = state.goods
    .filter((item) => item.status === 'active')
    .sort((left, right) => getTimeValue(right.collected_at) - getTimeValue(left.collected_at));
  const upcomingEvents = state.events
    .filter((item) => getTimeValue(item.date) >= Date.now())
    .sort((left, right) => getTimeValue(left.date) - getTimeValue(right.date));

  elements.activeGoodsMetric.textContent = String(activeGoods.length);
  elements.upcomingEventsMetric.textContent = String(upcomingEvents.length);
  elements.popularEventsList.innerHTML = upcomingEvents.slice(0, 3).map(createHighlightCard).join('');
  elements.activeGoodsList.innerHTML = activeGoods.slice(0, 4).map(createPosterCard).join('');

  if (!elements.popularEventsList.innerHTML) {
    elements.popularEventsList.innerHTML = createEmptyMiniCard(TEXT.upcomingEvents);
  }
  if (!elements.activeGoodsList.innerHTML) {
    elements.activeGoodsList.innerHTML = createEmptyMiniCard(TEXT.activeGoods);
  }
}

function createHighlightCard(item) {
  return `
    <a class="highlight_card" href="${escapeAttribute(item.url)}" target="_blank" rel="noopener noreferrer">
      <div class="badge_row">
        <span class="badge ${getTheaterBadgeClass(item.theater)}">${formatTheater(item.theater)}</span>
        <span class="badge">${escapeHtml(displayValue(item.type))}</span>
      </div>
      <span class="movie_name">${escapeHtml(displayValue(item.movie_name))}</span>
      <h3 class="card_title">${escapeHtml(displayValue(item.title))}</h3>
      <ul class="detail_list">
        <li class="detail_item">
          <span class="detail_label">\uB0A0\uC9DC</span>
          <span class="detail_value">${escapeHtml(formatDisplayDate(item.date))}</span>
        </li>
        <li class="detail_item">
          <span class="detail_label">\uC7A5\uC18C</span>
          <span class="detail_value">${escapeHtml(displayValue(item.location))}</span>
        </li>
      </ul>
      <footer class="card_footer">
        <span class="collected_text">${escapeHtml(formatCollectedAt(item.collected_at))}</span>
        <span class="source_link">${TEXT.original}</span>
      </footer>
    </a>
  `;
}

function createPosterCard(item) {
  return `
    <a class="poster_card" href="${escapeAttribute(item.url)}" target="_blank" rel="noopener noreferrer">
      <div class="badge_row">
        <span class="badge ${getTheaterBadgeClass(item.theater)}">${formatTheater(item.theater)}</span>
        <span class="badge badge_active">\uC9C4\uD589\uC911</span>
      </div>
      <span class="movie_name">${escapeHtml(displayValue(item.movie_name))}</span>
      <h3 class="card_title">${escapeHtml(displayValue(item.title))}</h3>
      <ul class="detail_list">
        <li class="detail_item">
          <span class="detail_label">\uAE30\uAC04</span>
          <span class="detail_value">${escapeHtml(formatDateRange(item.start_date, item.end_date))}</span>
        </li>
      </ul>
      <footer class="card_footer">
        <span class="collected_text">${escapeHtml(formatCollectedAt(item.collected_at))}</span>
        <span class="source_link">${TEXT.original}</span>
      </footer>
    </a>
  `;
}

function createEmptyMiniCard(label) {
  return `
    <div class="highlight_card">
      <span class="movie_name">${escapeHtml(label)}</span>
      <h3 class="card_title">${escapeHtml(getEmptyHighlightText())}</h3>
    </div>
  `;
}

function updateDataMeta(records) {
  const hasError = state.errors.goods || state.errors.events;
  const latest = records
    .map((record) => record.collected_at)
    .filter(Boolean)
    .sort((left, right) => new Date(right) - new Date(left))[0];

  if (hasError && records.length === 0) {
    elements.dataUpdated.textContent = TEXT.loadFailed;
    return;
  }

  elements.dataUpdated.textContent = hasError
    ? TEXT.partialLoadFailed
    : latest
    ? `${TEXT.latestCollected} ${formatDateTime(latest)}`
    : TEXT.noCollectionInfo;
}

function getEmptyHighlightText() {
  if (state.errors.goods || state.errors.events) return TEXT.loadFailed;
  return TEXT.noCollectionInfo;
}

function getTimeValue(value) {
  const time = new Date(value || 0).getTime();
  return Number.isNaN(time) ? 0 : time;
}

function displayValue(value) {
  return value === null || value === undefined || value === '' ? TEXT.unknown : String(value);
}

function formatTheater(value) {
  const labels = {
    cgv: 'CGV',
    lotte: '\uB86F\uB370\uC2DC\uB124\uB9C8',
    megabox: '\uBA54\uAC00\uBC15\uC2A4',
  };
  return labels[value] || '-';
}

function formatDisplayDate(value) {
  if (!value) return '-';
  const [datePart, timePart] = String(value).split('T');
  const date = datePart ? datePart.replaceAll('-', '.') : '-';
  const time = timePart ? ` ${timePart.slice(0, 5)}` : '';
  return `${date}${time}`;
}

function formatDateRange(startDate, endDate) {
  const start = formatDate(startDate);
  const end = formatDate(endDate);
  if (start && end && start !== end) return `${start} - ${end}`;
  if (start || end) return start || end;
  return '-';
}

function formatDate(value) {
  if (!value) return null;
  return String(value).slice(0, 10).replaceAll('-', '.');
}

function formatCollectedAt(value) {
  const date = formatDate(value);
  return date ? `${TEXT.latestCollected} ${date}` : TEXT.noCollectionInfo;
}

function formatDateTime(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return TEXT.unknown;

  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}.${month}.${day} ${hour}:${minute}`;
}

function getTheaterBadgeClass(value) {
  return `badge_${value || 'unknown'}`;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

function escapeAttribute(value) {
  return escapeHtml(value || '#');
}
