const TEXT = {
  unknown: '\uC815\uBCF4 \uC5C6\uC74C',
  count: '\uAC74',
  date: '\uB0A0\uC9DC',
  movie: '\uC601\uD654',
  location: '\uC7A5\uC18C',
  original: '\uC6D0\uBCF8 \uBCF4\uAE30',
  collected: '\uC218\uC9D1',
  collectedUnknown: '\uC218\uC9D1\uC77C \uC815\uBCF4 \uC5C6\uC74C',
  lotte: '\uB86F\uB370\uC2DC\uB124\uB9C8',
  megabox: '\uBA54\uAC00\uBC15\uC2A4',
};

const eventsState = {
  theater: 'all',
  type: 'all',
};

let eventsElements = {};

export function initEventsView({ onChange }) {
  eventsElements = {
    theaterFilter: document.querySelector('#events_theater_filter'),
    typeFilter: document.querySelector('#events_type_filter'),
    loading: document.querySelector('#events_loading'),
    error: document.querySelector('#events_error'),
    empty: document.querySelector('#events_empty'),
    list: document.querySelector('#events_list'),
    count: document.querySelector('#events_count'),
  };

  eventsElements.theaterFilter.addEventListener('change', (event) => {
    eventsState.theater = event.target.value;
    onChange();
  });

  eventsElements.typeFilter.addEventListener('change', (event) => {
    eventsState.type = event.target.value;
    onChange();
  });
}

export function renderEventsView(events, options = {}) {
  setEventsLoading(false);
  eventsElements.error.classList.toggle('is_hidden', !options.error);
  eventsElements.list.innerHTML = '';

  if (options.error) {
    eventsElements.count.textContent = `0${TEXT.count}`;
    eventsElements.empty.classList.add('is_hidden');
    return;
  }

  const filteredEvents = filterEvents(events, eventsState);
  const sortedEvents = sortEventsByDateDesc(filteredEvents);

  eventsElements.count.textContent = `${filteredEvents.length}${TEXT.count}`;
  eventsElements.empty.classList.toggle('is_hidden', filteredEvents.length > 0);
  eventsElements.list.innerHTML = sortedEvents.map(createEventCard).join('');
}

function setEventsLoading(isLoading) {
  eventsElements.loading.classList.toggle('is_hidden', !isLoading);
}

function filterEvents(events, filters) {
  return events.filter((item) => {
    const matchesTheater = filters.theater === 'all' || item.theater === filters.theater;
    const matchesType = filters.type === 'all' || item.type === filters.type;
    return matchesTheater && matchesType;
  });
}

function sortEventsByDateDesc(events) {
  return [...events].sort((left, right) => {
    const leftTime = getDateSortValue(left.date);
    const rightTime = getDateSortValue(right.date);

    if (leftTime === null && rightTime === null) return 0;
    if (leftTime === null) return 1;
    if (rightTime === null) return -1;
    return rightTime - leftTime;
  });
}

function createEventCard(item) {
  return `
    <a class="info_card" href="${escapeAttribute(item.url)}" target="_blank" rel="noopener noreferrer">
      <div class="card_topline">
        <div class="badge_row">
          <span class="badge ${getTheaterBadgeClass(item.theater)}">${formatTheater(item.theater)}</span>
          <span class="badge">${escapeHtml(displayValue(item.type))}</span>
        </div>
      </div>
      <span class="movie_name">${escapeHtml(displayValue(item.movie_name))}</span>
      <h3 class="card_title">${escapeHtml(displayValue(item.title))}</h3>
      <ul class="detail_list">
        <li class="detail_item">
          <span class="detail_label">${TEXT.date}</span>
          <span class="detail_value">${escapeHtml(formatDisplayDate(item.date))}</span>
        </li>
        <li class="detail_item">
          <span class="detail_label">${TEXT.movie}</span>
          <span class="detail_value">${escapeHtml(displayValue(item.movie_name))}</span>
        </li>
        <li class="detail_item">
          <span class="detail_label">${TEXT.location}</span>
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

function getDateSortValue(value) {
  if (!value) return null;
  const time = new Date(value).getTime();
  return Number.isNaN(time) ? null : time;
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

function formatDisplayDate(value) {
  if (!value) return '-';
  const [datePart, timePart] = String(value).split('T');
  const date = datePart ? datePart.replaceAll('-', '.') : '-';
  const time = timePart ? ` ${timePart.slice(0, 5)}` : '';
  return `${date}${time}`;
}

function formatCollectedAt(value) {
  if (!value) return TEXT.collectedUnknown;
  const date = String(value).slice(0, 10).replaceAll('-', '.');
  return `${TEXT.collected} ${date}`;
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
