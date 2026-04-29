const TEXT = {
  unknown: '\uC815\uBCF4 \uC5C6\uC74C',
  count: '\uAC74',
  movie: '\uC601\uD654',
  period: '\uAE30\uAC04',
  source: '\uCD9C\uCC98',
  original: '\uC6D0\uBCF8 \uBCF4\uAE30',
  collected: '\uC218\uC9D1',
  collectedUnknown: '\uC218\uC9D1\uC77C \uC815\uBCF4 \uC5C6\uC74C',
  active: '\uC9C4\uD589\uC911',
  ended: '\uC885\uB8CC',
  lotte: '\uB86F\uB370\uC2DC\uB124\uB9C8',
  megabox: '\uBA54\uAC00\uBC15\uC2A4',
};

const goodsState = {
  search: '',
  status: 'all',
};

let goodsElements = {};

export function initGoodsView({ onChange }) {
  goodsElements = {
    search: document.querySelector('#goods_search'),
    statusFilter: document.querySelector('#goods_status_filter'),
    loading: document.querySelector('#goods_loading'),
    error: document.querySelector('#goods_error'),
    empty: document.querySelector('#goods_empty'),
    list: document.querySelector('#goods_list'),
    count: document.querySelector('#goods_count'),
  };

  goodsElements.search.addEventListener('input', (event) => {
    goodsState.search = event.target.value;
    onChange();
  });

  goodsElements.statusFilter.addEventListener('change', (event) => {
    goodsState.status = event.target.value;
    onChange();
  });
}

export function renderGoodsView(goods, options = {}) {
  setGoodsLoading(false);
  goodsElements.error.classList.toggle('is_hidden', !options.error);
  goodsElements.list.innerHTML = '';

  if (options.error) {
    goodsElements.count.textContent = `0${TEXT.count}`;
    goodsElements.empty.classList.add('is_hidden');
    return;
  }

  const filteredGoods = filterGoods(goods, goodsState);
  const sortedGoods = sortGoodsByCollectedAt(filteredGoods);
  const groups = groupGoodsByMovie(sortedGoods);

  goodsElements.count.textContent = `${filteredGoods.length}${TEXT.count}`;
  goodsElements.empty.classList.toggle('is_hidden', filteredGoods.length > 0);

  groups.forEach((group) => {
    goodsElements.list.appendChild(createMovieGroup(group));
  });
}

function setGoodsLoading(isLoading) {
  goodsElements.loading.classList.toggle('is_hidden', !isLoading);
}

function filterGoods(goods, filters) {
  const keyword = normalizeText(filters.search);

  return goods.filter((item) => {
    const matchesStatus = filters.status === 'all' || item.status === filters.status;
    const searchableText = normalizeText(`${item.movie_name || ''} ${item.title || ''}`);
    const matchesSearch = !keyword || searchableText.includes(keyword);
    return matchesStatus && matchesSearch;
  });
}

function sortGoodsByCollectedAt(goods) {
  return [...goods].sort((left, right) => {
    return getTimeValue(right.collected_at) - getTimeValue(left.collected_at);
  });
}

function groupGoodsByMovie(goods) {
  const groupMap = new Map();

  goods.forEach((item) => {
    const groupName = item.movie_name || TEXT.unknown;
    if (!groupMap.has(groupName)) {
      groupMap.set(groupName, []);
    }
    groupMap.get(groupName).push(item);
  });

  return Array.from(groupMap.entries()).map(([movieName, items]) => ({
    movieName,
    items,
  }));
}

function createMovieGroup(group) {
  const groupElement = document.createElement('article');
  groupElement.className = 'movie_group';

  const cards = group.items.map(createGoodsCard).join('');
  groupElement.innerHTML = `
    <header class="movie_group_header">
      <h3 class="movie_group_title">${escapeHtml(group.movieName)}</h3>
      <span class="movie_group_count">${group.items.length}${TEXT.count}</span>
    </header>
    <div class="card_grid">${cards}</div>
  `;

  return groupElement;
}

function createGoodsCard(item) {
  return `
    <a class="info_card" href="${escapeAttribute(item.url)}" target="_blank" rel="noopener noreferrer">
      <div class="card_topline">
        <div class="badge_row">
          <span class="badge ${getTheaterBadgeClass(item.theater)}">${formatTheater(item.theater)}</span>
          <span class="badge ${getStatusBadgeClass(item.status)}">${formatStatus(item.status)}</span>
        </div>
      </div>
      <span class="movie_name">${escapeHtml(displayValue(item.movie_name))}</span>
      <h4 class="card_title">${escapeHtml(displayValue(item.title))}</h4>
      <ul class="detail_list">
        <li class="detail_item">
          <span class="detail_label">${TEXT.movie}</span>
          <span class="detail_value">${escapeHtml(displayValue(item.movie_name))}</span>
        </li>
        <li class="detail_item">
          <span class="detail_label">${TEXT.period}</span>
          <span class="detail_value">${escapeHtml(formatDateRange(item.start_date, item.end_date))}</span>
        </li>
        <li class="detail_item">
          <span class="detail_label">${TEXT.source}</span>
          <span class="detail_value">${escapeHtml(formatSourceHost(item.source_page_url))}</span>
        </li>
      </ul>
      <footer class="card_footer">
        <span class="collected_text">${escapeHtml(formatCollectedAt(item.collected_at))}</span>
        <span class="source_link">${TEXT.original}</span>
      </footer>
    </a>
  `;
}

function normalizeText(value) {
  return String(value || '').trim().toLowerCase();
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
    lotte: TEXT.lotte,
    megabox: TEXT.megabox,
  };
  return labels[value] || '-';
}

function formatStatus(value) {
  const labels = {
    active: TEXT.active,
    ended: TEXT.ended,
  };
  return labels[value] || '-';
}

function formatDate(value) {
  if (!value) return null;
  return String(value).slice(0, 10).replaceAll('-', '.');
}

function formatDateRange(startDate, endDate) {
  const start = formatDate(startDate);
  const end = formatDate(endDate);
  if (start && end && start !== end) return `${start} - ${end}`;
  if (start || end) return start || end;
  return '-';
}

function formatCollectedAt(value) {
  const date = formatDate(value);
  return date ? `${TEXT.collected} ${date}` : TEXT.collectedUnknown;
}

function formatSourceHost(value) {
  if (!value) return '-';
  try {
    return new URL(value).host;
  } catch (_error) {
    return '-';
  }
}

function getTheaterBadgeClass(value) {
  return `badge_${value || 'unknown'}`;
}

function getStatusBadgeClass(value) {
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
