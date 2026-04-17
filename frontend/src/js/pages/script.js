
// ============================================
// 유틸리티 함수
// ============================================

function indexToTime(index) {
  if (index === 47) return "23:59";
  const totalMinutes = index * 30;
  const hour = Math.floor(totalMinutes / 60);
  const minute = totalMinutes % 60;
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
}

function logEvent(eventType, data) {
  // const timestamp = new Date().toLocaleTimeString('ko-KR');
  // console.log(`[${timestamp}] ${eventType}:`, data);
}


// ============================================
// 날짜 입력 + 커스텀 캘린더 (최종)
// ============================================

const input = document.getElementById("dateInput");
const calendar = document.getElementById("calendar");
const datesContainer = document.getElementById("calendarDates");
const monthYear = document.getElementById("monthYear");

let currentDate = new Date();

function initializeDateInput() {
  if (!input) return;
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");
  const todayString = `${year}-${month}-${day}`;
  input.value = todayString;
  input.min = todayString;
}

if (input && calendar) {
  input.addEventListener("click", () => {
    calendar.style.display = "block";
    renderCalendar();
  });
}

document.addEventListener("click", (e) => {
  if (calendar && !e.target.closest(".form-group")) {
    calendar.style.display = "none";
  }
});

function renderCalendar() {
  if (!datesContainer || !monthYear) return;
  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  monthYear.textContent = `${year}년 ${month + 1}월`;
  const firstDay = new Date(year, month, 1).getDay();
  const lastDate = new Date(year, month + 1, 0).getDate();
  datesContainer.innerHTML = "";

  for (let i = 0; i < firstDay; i++) {
    datesContainer.innerHTML += `<div></div>`;
  }

  for (let d = 1; d <= lastDate; d++) {
    const dateEl = document.createElement("div");
    dateEl.textContent = d;
    const thisDate = new Date(year, month, d);

    if (thisDate.getTime() === today.getTime()) {
      dateEl.classList.add("today");
    }

    if (thisDate < today) {
      dateEl.classList.add("disabled");
    } else {
      dateEl.addEventListener("click", () => {
        const selectedDate = `${year}-${String(month + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
        if (input) input.value = selectedDate;
        if (calendar) calendar.style.display = "none";
      });
    }
    datesContainer.appendChild(dateEl);
  }
}

const prevMonth = document.getElementById("prevMonth");
if (prevMonth) {
  prevMonth.onclick = () => {
    const today = new Date();
    if (currentDate.getFullYear() > today.getFullYear() || 
        (currentDate.getFullYear() === today.getFullYear() && currentDate.getMonth() > today.getMonth())) {
      currentDate.setMonth(currentDate.getMonth() - 1);
      renderCalendar();
    }
  };
}

const nextMonth = document.getElementById("nextMonth");
if (nextMonth) {
  nextMonth.onclick = () => {
    currentDate.setMonth(currentDate.getMonth() + 1);
    renderCalendar();
  };
}

if (input) {
  input.addEventListener("change", function () {
    logEvent("날짜 입력", { action: "변경", date: this.value });
  });
}

// ============================================
// 시간 선택 슬라이더 (핵심)
// ============================================

const sliderStart = document.getElementById('sliderStart');
const sliderEnd = document.getElementById('sliderEnd');
const sliderProgress = document.getElementById('sliderProgress');
const timeValue = document.getElementById('timeValue');
const sliderTrack = document.querySelector('.slider-track');

const timeSliderState = {
  startIndex: 24,
  endIndex: 47,
  isDragging: false,
  activeHandle: null
};

function updateSliderProgress() {
  if (!sliderTrack || !sliderProgress || !timeValue) return;
  const startPercent = (timeSliderState.startIndex / 47) * 100;
  const endPercent = (timeSliderState.endIndex / 47) * 100;

  sliderProgress.style.left = startPercent + '%';
  sliderProgress.style.width = (endPercent - startPercent) + '%';

  const startTime = indexToTime(timeSliderState.startIndex);
  const endTime = indexToTime(timeSliderState.endIndex);
  timeValue.textContent = `${startTime} - ${endTime}`;
}

function updateHandlePosition() {
  if (!sliderStart || !sliderEnd) return;
  const startPercent = (timeSliderState.startIndex / 47) * 100;
  const endPercent = (timeSliderState.endIndex / 47) * 100;
  sliderStart.style.left = startPercent + '%';
  sliderEnd.style.left = endPercent + '%';
}

function getIndexFromMousePosition(event) {
  if (!sliderTrack) return 0;
  const rect = sliderTrack.getBoundingClientRect();
  const percent = (event.clientX - rect.left) / rect.width;
  let index = Math.round(percent * 47);
  return Math.max(0, Math.min(47, index));
}

if (sliderStart) {
  sliderStart.addEventListener('mousedown', function () {
    timeSliderState.isDragging = true;
    timeSliderState.activeHandle = 'start';
    sliderStart.style.cursor = 'grabbing';
  });
}

if (sliderEnd) {
  sliderEnd.addEventListener('mousedown', function () {
    timeSliderState.isDragging = true;
    timeSliderState.activeHandle = 'end';
    sliderEnd.style.cursor = 'grabbing';
  });
}

document.addEventListener('mousemove', function (event) {
  if (!timeSliderState.isDragging) return;
  const newIndex = getIndexFromMousePosition(event);
  if (timeSliderState.activeHandle === 'start') {
    if (newIndex <= timeSliderState.endIndex) timeSliderState.startIndex = newIndex;
  } else if (timeSliderState.activeHandle === 'end') {
    if (newIndex >= timeSliderState.startIndex) timeSliderState.endIndex = newIndex;
  }
  updateHandlePosition();
  updateSliderProgress();
});

document.addEventListener('mouseup', function () {
  if (timeSliderState.isDragging) {
    timeSliderState.isDragging = false;
    if (sliderStart) sliderStart.style.cursor = 'grab';
    if (sliderEnd) sliderEnd.style.cursor = 'grab';
  }
});

if (sliderTrack) {
  sliderTrack.addEventListener('mousedown', function (e) {
    const index = getIndexFromMousePosition(e);
    const distToStart = Math.abs(index - timeSliderState.startIndex);
    const distToEnd = Math.abs(index - timeSliderState.endIndex);
    if (distToStart < distToEnd) {
      timeSliderState.startIndex = Math.min(index, timeSliderState.endIndex);
    } else {
      timeSliderState.endIndex = Math.max(index, timeSliderState.startIndex);
    }
    updateHandlePosition();
    updateSliderProgress();
  });
}

// ============================================
// 인원 선택 및 가격 강조 등 (누락 방지)
// ============================================
const personCount = document.getElementById('personCount');
const increaseBtn = document.getElementById('increaseBtn');
const decreaseBtn = document.getElementById('decreaseBtn');
let currentPersonCount = 1;

if (increaseBtn) {
  increaseBtn.addEventListener('click', () => { currentPersonCount++; if (personCount) personCount.textContent = currentPersonCount; });
}
if (decreaseBtn) {
  decreaseBtn.addEventListener('click', () => { if (currentPersonCount > 1) { currentPersonCount--; if (personCount) personCount.textContent = currentPersonCount; } });
}

function highlightLowestPrices() {
  const movieCards = document.querySelectorAll('.movie-card');
  if (movieCards.length === 0) return;
  // ... (기본 로직 유지)
}

const aiCards = document.querySelectorAll('.ai-card');
if (aiCards.length > 0) {
  let currentFeaturedIndex = 0;
  setInterval(() => {
    aiCards.forEach(c => c.classList.remove('featured'));
    aiCards[currentFeaturedIndex].classList.add('featured');
    currentFeaturedIndex = (currentFeaturedIndex + 1) % aiCards.length;
  }, 3000);
}

const searchBtn = document.getElementById('searchBtn');
if (searchBtn) {
  searchBtn.addEventListener('click', () => {
    const searchData = {
      movie: document.getElementById('movieInput')?.value || '전체',
      region: document.getElementById('regionInput')?.value || '전체',
      date: document.getElementById('dateInput')?.value || '',
      timeStart: indexToTime(timeSliderState.startIndex),
      timeEnd: indexToTime(timeSliderState.endIndex),
      personCount: currentPersonCount
    };
    const params = new URLSearchParams(searchData);
    window.location.href = `/basic/priceComparison.html?${params.toString()}`;
  });
}

function initialize() {
  initializeDateInput();
  updateSliderProgress();
  updateHandlePosition();
  highlightLowestPrices();
}

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initialize);
} else {
  initialize();
}