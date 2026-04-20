
// ============================================
// 유틸리티 함수
// ============================================

/**
 * 시간 인덱스를 시간:분 형식으로 변환
 * @param {number} index - 30분 단위 인덱스 (0-47)
 * @returns {string} HH:MM 형식의 시간 문자열
 */
function indexToTime(index) {
  // 👉 마지막은 강제로 23:59
  if (index === 47) return "23:59";

  const totalMinutes = index * 30;

  const hour = Math.floor(totalMinutes / 60);
  const minute = totalMinutes % 60;

  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
}

/**
 * 콘솔에 이벤트 로그 출력
 * @param {string} eventType - 이벤트 유형
 * @param {object} data - 이벤트 데이터
 */
function logEvent(eventType, data) {
  const timestamp = new Date().toLocaleTimeString('ko-KR');
  // console.log(`[${timestamp}] ${eventType}:`, data);
}


// ============================================
// 날짜 입력 + 커스텀 캘린더 (최종)
// ============================================

// 요소 가져오기
const input = document.getElementById("dateInput");
const calendar = document.getElementById("calendar");
const datesContainer = document.getElementById("calendarDates");
const monthYear = document.getElementById("monthYear");

// 현재 기준 날짜
let currentDate = new Date();

// ============================================
// 1. 초기화 (오늘 날짜 세팅)
// ============================================
function initializeDateInput() {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");

  const todayString = `${year}-${month}-${day}`;

  input.value = todayString;
  input.min = todayString;

  logEvent("날짜 입력", { action: "초기화", date: todayString });
}

// ============================================
// 2. 캘린더 열기
// ============================================
input.addEventListener("click", () => {
  calendar.style.display = "block";
  renderCalendar();
});

// ============================================
// 3. 외부 클릭 시 닫기
// ============================================
document.addEventListener("click", (e) => {
  if (!e.target.closest(".form-group")) {
    calendar.style.display = "none";
  }
});

// ============================================
// 4. 캘린더 렌더링 (핵심)
// ============================================
function renderCalendar() {
  const year = currentDate.getFullYear();
  const month = currentDate.getMonth();

  // 오늘 기준 (시간 제거 필수)
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  monthYear.textContent = `${year}년 ${month + 1}월`;

  const firstDay = new Date(year, month, 1).getDay();
  const lastDate = new Date(year, month + 1, 0).getDate();

  datesContainer.innerHTML = "";

  // 앞쪽 빈칸
  for (let i = 0; i < firstDay; i++) {
    datesContainer.innerHTML += `<div></div>`;
  }

  // 날짜 생성
  for (let d = 1; d <= lastDate; d++) {
    const dateEl = document.createElement("div");
    dateEl.textContent = d;

    const thisDate = new Date(year, month, d);

    // 오늘 날짜 표시
    if (thisDate.getTime() === today.getTime()) {
      dateEl.classList.add("today");
    }

    // 과거 날짜 막기
    if (thisDate < today) {
      dateEl.classList.add("disabled");
    } else {
      dateEl.addEventListener("click", () => {
        const selectedDate = `${year}-${String(month + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`;

        input.value = selectedDate;
        calendar.style.display = "none";

        logEvent("날짜 입력", { action: "선택", date: selectedDate });
      });
    }

    datesContainer.appendChild(dateEl);
  }
}

// ============================================
// 5. 이전 / 다음 달 이동
// ============================================
document.getElementById("prevMonth").onclick = () => {
  const today = new Date();

  // 현재 달보다 이전으로 못 가게 제한
  if (
    currentDate.getFullYear() > today.getFullYear() ||
    (currentDate.getFullYear() === today.getFullYear() &&
      currentDate.getMonth() > today.getMonth())
  ) {
    currentDate.setMonth(currentDate.getMonth() - 1);
    renderCalendar();
  }
};

document.getElementById("nextMonth").onclick = () => {
  currentDate.setMonth(currentDate.getMonth() + 1);
  renderCalendar();
};

// ============================================
// 6. 날짜 변경 이벤트
// ============================================
input.addEventListener("change", function () {
  logEvent("날짜 입력", { action: "변경", date: this.value });
});

// ============================================
// 실행
// ============================================
initializeDateInput();

// ============================================
// 4. 시간 선택 슬라이더 (핵심)
// ============================================

const sliderStart = document.getElementById('sliderStart');
const sliderEnd = document.getElementById('sliderEnd');
const sliderProgress = document.getElementById('sliderProgress');
const timeValue = document.getElementById('timeValue');
const sliderTrack = document.querySelector('.slider-track');

// 시간 슬라이더 상태
const timeSliderState = {
  startIndex: 24, // 12:00
  endIndex: 47,   // 👉 23:59로 표시됨
  isDragging: false,
  activeHandle: null
};

/**
 * 슬라이더 진행 상황 업데이트
 */
function updateSliderProgress() {
  const trackWidth = sliderTrack.offsetWidth;
  const startPercent = (timeSliderState.startIndex / 47) * 100;
  const endPercent = (timeSliderState.endIndex / 47) * 100;

  sliderProgress.style.left = startPercent + '%';
  sliderProgress.style.width = (endPercent - startPercent) + '%';

  const startTime = indexToTime(timeSliderState.startIndex);
  const endTime = indexToTime(timeSliderState.endIndex);
  timeValue.textContent = `${startTime} - ${endTime}`;

  logEvent('시간 슬라이더', {
    action: '업데이트',
    startTime: startTime,
    endTime: endTime,
    startIndex: timeSliderState.startIndex,
    endIndex: timeSliderState.endIndex
  });
}

/**
 * 슬라이더 핸들 위치 업데이트
 */
function updateHandlePosition() {
  const trackWidth = sliderTrack.offsetWidth;
  const startPercent = (timeSliderState.startIndex / 47) * 100;
  const endPercent = (timeSliderState.endIndex / 47) * 100;

  sliderStart.style.left = startPercent + '%';
  sliderEnd.style.left = endPercent + '%';
}

/**
 * 마우스 위치에서 인덱스 계산
 */
function getIndexFromMousePosition(event) {
  const rect = sliderTrack.getBoundingClientRect();
  const percent = (event.clientX - rect.left) / rect.width;

  let index = Math.round(percent * 47);

  // 👉 그냥 그대로 사용 (이미 30분 단위임)
  return Math.max(0, Math.min(47, index));
}

// 슬라이더 시작 핸들 이벤트
sliderStart.addEventListener('mousedown', function () {
  timeSliderState.isDragging = true;
  timeSliderState.activeHandle = 'start';
  sliderStart.style.cursor = 'grabbing';
  logEvent('시간 슬라이더', { action: '드래그 시작', handle: 'start' });
});

// 슬라이더 종료 핸들 이벤트
sliderEnd.addEventListener('mousedown', function () {
  timeSliderState.isDragging = true;
  timeSliderState.activeHandle = 'end';
  sliderEnd.style.cursor = 'grabbing';
  logEvent('시간 슬라이더', { action: '드래그 시작', handle: 'end' });
});

// 문서 전체 마우스 이동 이벤트
document.addEventListener('mousemove', function (event) {
  if (!timeSliderState.isDragging) return;

  const newIndex = getIndexFromMousePosition(event);

  if (timeSliderState.activeHandle === 'start') {
    if (newIndex <= timeSliderState.endIndex) {
      timeSliderState.startIndex = newIndex;
    }
  } else if (timeSliderState.activeHandle === 'end') {
    if (newIndex >= timeSliderState.startIndex) {
      timeSliderState.endIndex = newIndex;
    }
  }

  updateHandlePosition();
  updateSliderProgress();
});

// 문서 전체 마우스 업 이벤트
document.addEventListener('mouseup', function () {
  if (timeSliderState.isDragging) {
    timeSliderState.isDragging = false;
    sliderStart.style.cursor = 'grab';
    sliderEnd.style.cursor = 'grab';
    logEvent('시간 슬라이더', { action: '드래그 종료' });
  }
});

// 트랙 클릭 시 가까운 핸들 이동
sliderTrack.addEventListener('mousedown', function (e) {
  const rect = sliderTrack.getBoundingClientRect();
  const percent = (e.clientX - rect.left) / rect.width;
  const index = Math.max(0, Math.min(47, Math.round(percent * 47)));

  // 👉 더 가까운 핸들 선택
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

// ============================================
// 5. 인원 선택 기능
// ============================================

const personCount = document.getElementById('personCount');
const increaseBtn = document.getElementById('increaseBtn');
const decreaseBtn = document.getElementById('decreaseBtn');

let currentPersonCount = 1;

/**
 * 인원 수 업데이트
 */
function updatePersonCount() {
  personCount.textContent = currentPersonCount;
  logEvent('인원 선택', { action: '변경', count: currentPersonCount });
}

increaseBtn.addEventListener('click', function () {
  currentPersonCount++;
  updatePersonCount();
});

decreaseBtn.addEventListener('click', function () {
  if (currentPersonCount > 1) {
    currentPersonCount--;
    updatePersonCount();
  }
});

// ============================================
// 6. 가격 비교 카드 강조
// ============================================

/**
 * 각 영화 카드의 최저가 항목 강조
 */
function highlightLowestPrices() {
  const movieCards = document.querySelectorAll('.movie-card');

  movieCards.forEach(card => {
    const priceItems = card.querySelectorAll('.price-item');
    let lowestPrice = Infinity;
    let lowestItem = null;

    // 최저가 찾기
    priceItems.forEach(item => {
      const priceText = item.querySelector('.price-amount').textContent;
      const price = parseInt(priceText.replace(/[^0-9]/g, ''));

      if (price < lowestPrice) {
        lowestPrice = price;
        lowestItem = item;
      }
    });

    // 기존 강조 제거
    priceItems.forEach(item => {
      item.classList.remove('lowest-price');
    });

    // 최저가 항목에 강조 추가
    if (lowestItem) {
      lowestItem.classList.add('lowest-price');
      logEvent('가격 비교', {
        action: '최저가 강조',
        movieTitle: card.querySelector('.movie-title').textContent,
        lowestPrice: lowestPrice
      });
    }
  });
}

// ============================================
// 7. AI 추천 카드 애니메이션
// ============================================

const aiGrid = document.getElementById('aiGrid');
const aiCards = document.querySelectorAll('.ai-card');
let currentFeaturedIndex = 0;

/**
 * AI 카드 강조 업데이트
 */
function updateAICardFeatured() {
  aiCards.forEach((card, index) => {
    card.classList.remove('featured');
  });

  aiCards[currentFeaturedIndex].classList.add('featured');

  logEvent('AI 추천 카드', {
    action: '강조 이동',
    currentIndex: currentFeaturedIndex,
    cardTitle: aiCards[currentFeaturedIndex].querySelector('.ai-card-title').textContent
  });

  currentFeaturedIndex = (currentFeaturedIndex + 1) % aiCards.length;
}

// AI 카드 애니메이션 시작 (3초 간격)
setInterval(updateAICardFeatured, 3000);

// ============================================
// 8. 검색 버튼 및 기타 이벤트
// ============================================

const searchBtn = document.getElementById('searchBtn');
const nearbyBtn = document.getElementById('nearbyBtn');
const movieInput = document.getElementById('movieInput');
const regionInput = document.getElementById('regionInput');

/**
 * 검색 실행
 */
function performSearch() {
  const searchData = {
    movie: movieInput.value || '전체',
    region: regionInput.value || '전체',
    date: dateInput.value,
    timeStart: indexToTime(timeSliderState.startIndex),
    timeEnd: indexToTime(timeSliderState.endIndex),
    personCount: currentPersonCount
  };

  logEvent('검색', {
    action: '실행',
    ...searchData
  });

  console.log('검색 조건:', searchData);

  const params = new URLSearchParams(searchData);
  window.location.href = `/basic/priceComparison.html?${params.toString()}`;
}

searchBtn.addEventListener('click', performSearch);

movieInput.addEventListener('keypress', function (event) {
  if (event.key === 'Enter') {
    performSearch();
  }
});

nearbyBtn.addEventListener('click', function () {
  logEvent('근처 극장', { action: '클릭' });
  console.log('근처 극장 검색 시작...');
});

// ============================================
// 초기화
// ============================================

function initialize() {
  initializeDateInput();
  updateSliderProgress();
  updateHandlePosition();
  highlightLowestPrices();

  logEvent('페이지', { action: '초기화 완료' });
  console.log('='.repeat(50));
  console.log('다보여 - 영화 최저가 찾기 페이지 로드 완료');
  console.log('='.repeat(50));
}

// DOM 로드 완료 후 초기화
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initialize);
} else {
  initialize();
}





const regionInput2 = document.getElementById("regionInput");
const regionDropdown = document.getElementById("regionDropdown");

// Kakao Places 객체 생성
const ps = new kakao.maps.services.Places();

/**
 * 지역 검색 실행
 */
function searchRegion(keyword) {
  if (!keyword) {
    regionDropdown.style.display = "none";
    return;
  }

  ps.keywordSearch(keyword, function (data, status) {
    if (status === kakao.maps.services.Status.OK) {
      renderDropdown(data);
    } else {
      regionDropdown.style.display = "none";
    }
  });
}
















    // ============================================
    // 유틸리티 함수
    // ============================================

    /**
     * 시간 인덱스를 시간:분 형식으로 변환
     * @param {number} index - 30분 단위 인덱스 (0-47)
     * @returns {string} HH:MM 형식의 시간 문자열
     */
    function indexToTime(index) {
      // 👉 마지막은 강제로 23:59
      if (index === 47) return "23:59";

      const totalMinutes = index * 30;

      const hour = Math.floor(totalMinutes / 60);
      const minute = totalMinutes % 60;

      return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
    }

    /**
     * 콘솔에 이벤트 로그 출력
     * @param {string} eventType - 이벤트 유형
     * @param {object} data - 이벤트 데이터
     */
    function logEvent(eventType, data) {
      const timestamp = new Date().toLocaleTimeString('ko-KR');
      // console.log(`[${timestamp}] ${eventType}:`, data);
    }


    // ============================================
    // 날짜 입력 + 커스텀 캘린더 (최종)
    // ============================================

    // 요소 가져오기
    const input = document.getElementById("dateInput");
    const calendar = document.getElementById("calendar");
    const datesContainer = document.getElementById("calendarDates");
    const monthYear = document.getElementById("monthYear");

    // 현재 기준 날짜
    let currentDate = new Date();

    // ============================================
    // 1. 초기화 (오늘 날짜 세팅)
    // ============================================
    function initializeDateInput() {
      const today = new Date();
      const year = today.getFullYear();
      const month = String(today.getMonth() + 1).padStart(2, "0");
      const day = String(today.getDate()).padStart(2, "0");

      const todayString = `${year}-${month}-${day}`;

      input.value = todayString;
      input.min = todayString;

      logEvent("날짜 입력", { action: "초기화", date: todayString });
    }

    // ============================================
    // 2. 캘린더 열기
    // ============================================
    input.addEventListener("click", () => {
      calendar.style.display = "block";
      renderCalendar();
    });

    // ============================================
    // 3. 외부 클릭 시 닫기
    // ============================================
    document.addEventListener("click", (e) => {
      if (!e.target.closest(".form-group")) {
        calendar.style.display = "none";
      }
    });

    // ============================================
    // 4. 캘린더 렌더링 (핵심)
    // ============================================
    function renderCalendar() {
      const year = currentDate.getFullYear();
      const month = currentDate.getMonth();

      // 오늘 기준 (시간 제거 필수)
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      monthYear.textContent = `${year}년 ${month + 1}월`;

      const firstDay = new Date(year, month, 1).getDay();
      const lastDate = new Date(year, month + 1, 0).getDate();

      datesContainer.innerHTML = "";

      // 앞쪽 빈칸
      for (let i = 0; i < firstDay; i++) {
        datesContainer.innerHTML += `<div></div>`;
      }

      // 날짜 생성
      for (let d = 1; d <= lastDate; d++) {
        const dateEl = document.createElement("div");
        dateEl.textContent = d;

        const thisDate = new Date(year, month, d);

        // 오늘 날짜 표시
        if (thisDate.getTime() === today.getTime()) {
          dateEl.classList.add("today");
        }

        // 과거 날짜 막기
        if (thisDate < today) {
          dateEl.classList.add("disabled");
        } else {
          dateEl.addEventListener("click", () => {
            const selectedDate = `${year}-${String(month + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`;

            input.value = selectedDate;
            calendar.style.display = "none";

            logEvent("날짜 입력", { action: "선택", date: selectedDate });
          });
        }

        datesContainer.appendChild(dateEl);
      }
    }

    // ============================================
    // 5. 이전 / 다음 달 이동
    // ============================================
    document.getElementById("prevMonth").onclick = () => {
      const today = new Date();

      // 현재 달보다 이전으로 못 가게 제한
      if (
        currentDate.getFullYear() > today.getFullYear() ||
        (currentDate.getFullYear() === today.getFullYear() &&
          currentDate.getMonth() > today.getMonth())
      ) {
        currentDate.setMonth(currentDate.getMonth() - 1);
        renderCalendar();
      }
    };

    document.getElementById("nextMonth").onclick = () => {
      currentDate.setMonth(currentDate.getMonth() + 1);
      renderCalendar();
    };

    // ============================================
    // 6. 날짜 변경 이벤트
    // ============================================
    input.addEventListener("change", function () {
      logEvent("날짜 입력", { action: "변경", date: this.value });
    });

    // ============================================
    // 실행
    // ============================================
    initializeDateInput();

    // ============================================
    // 4. 시간 선택 슬라이더 (핵심)
    // ============================================

    const sliderStart = document.getElementById('sliderStart');
    const sliderEnd = document.getElementById('sliderEnd');
    const sliderProgress = document.getElementById('sliderProgress');
    const timeValue = document.getElementById('timeValue');
    const sliderTrack = document.querySelector('.slider-track');

    // 시간 슬라이더 상태
    const timeSliderState = {
      startIndex: 24, // 12:00
      endIndex: 47,   // 👉 23:59로 표시됨
      isDragging: false,
      activeHandle: null
    };

    /**
     * 슬라이더 진행 상황 업데이트
     */
    function updateSliderProgress() {
      const trackWidth = sliderTrack.offsetWidth;
      const startPercent = (timeSliderState.startIndex / 47) * 100;
      const endPercent = (timeSliderState.endIndex / 47) * 100;

      sliderProgress.style.left = startPercent + '%';
      sliderProgress.style.width = (endPercent - startPercent) + '%';

      const startTime = indexToTime(timeSliderState.startIndex);
      const endTime = indexToTime(timeSliderState.endIndex);
      timeValue.textContent = `${startTime} - ${endTime}`;

      logEvent('시간 슬라이더', {
        action: '업데이트',
        startTime: startTime,
        endTime: endTime,
        startIndex: timeSliderState.startIndex,
        endIndex: timeSliderState.endIndex
      });
    }

    /**
     * 슬라이더 핸들 위치 업데이트
     */
    function updateHandlePosition() {
      const trackWidth = sliderTrack.offsetWidth;
      const startPercent = (timeSliderState.startIndex / 47) * 100;
      const endPercent = (timeSliderState.endIndex / 47) * 100;

      sliderStart.style.left = startPercent + '%';
      sliderEnd.style.left = endPercent + '%';
    }

    /**
     * 마우스 위치에서 인덱스 계산
     */
    function getIndexFromMousePosition(event) {
      const rect = sliderTrack.getBoundingClientRect();
      const percent = (event.clientX - rect.left) / rect.width;

      let index = Math.round(percent * 47);

      // 👉 그냥 그대로 사용 (이미 30분 단위임)
      return Math.max(0, Math.min(47, index));
    }

    // 슬라이더 시작 핸들 이벤트
    sliderStart.addEventListener('mousedown', function () {
      timeSliderState.isDragging = true;
      timeSliderState.activeHandle = 'start';
      sliderStart.style.cursor = 'grabbing';
      logEvent('시간 슬라이더', { action: '드래그 시작', handle: 'start' });
    });

    // 슬라이더 종료 핸들 이벤트
    sliderEnd.addEventListener('mousedown', function () {
      timeSliderState.isDragging = true;
      timeSliderState.activeHandle = 'end';
      sliderEnd.style.cursor = 'grabbing';
      logEvent('시간 슬라이더', { action: '드래그 시작', handle: 'end' });
    });

    // 문서 전체 마우스 이동 이벤트
    document.addEventListener('mousemove', function (event) {
      if (!timeSliderState.isDragging) return;

      const newIndex = getIndexFromMousePosition(event);

      if (timeSliderState.activeHandle === 'start') {
        if (newIndex <= timeSliderState.endIndex) {
          timeSliderState.startIndex = newIndex;
        }
      } else if (timeSliderState.activeHandle === 'end') {
        if (newIndex >= timeSliderState.startIndex) {
          timeSliderState.endIndex = newIndex;
        }
      }

      updateHandlePosition();
      updateSliderProgress();
    });

    // 문서 전체 마우스 업 이벤트
    document.addEventListener('mouseup', function () {
      if (timeSliderState.isDragging) {
        timeSliderState.isDragging = false;
        sliderStart.style.cursor = 'grab';
        sliderEnd.style.cursor = 'grab';
        logEvent('시간 슬라이더', { action: '드래그 종료' });
      }
    });

    // 트랙 클릭 시 가까운 핸들 이동
    sliderTrack.addEventListener('mousedown', function (e) {
      const rect = sliderTrack.getBoundingClientRect();
      const percent = (e.clientX - rect.left) / rect.width;
      const index = Math.max(0, Math.min(47, Math.round(percent * 47)));

      // 👉 더 가까운 핸들 선택
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

    // ============================================
    // 5. 인원 선택 기능
    // ============================================

    const personCount = document.getElementById('personCount');
    const increaseBtn = document.getElementById('increaseBtn');
    const decreaseBtn = document.getElementById('decreaseBtn');

    let currentPersonCount = 1;

    /**
     * 인원 수 업데이트
     */
    function updatePersonCount() {
      personCount.textContent = currentPersonCount;
      logEvent('인원 선택', { action: '변경', count: currentPersonCount });
    }

    increaseBtn.addEventListener('click', function () {
      currentPersonCount++;
      updatePersonCount();
    });

    decreaseBtn.addEventListener('click', function () {
      if (currentPersonCount > 1) {
        currentPersonCount--;
        updatePersonCount();
      }
    });

    // ============================================
    // 6. 가격 비교 카드 강조
    // ============================================

    /**
     * 각 영화 카드의 최저가 항목 강조
     */
    function highlightLowestPrices() {
      const movieCards = document.querySelectorAll('.movie-card');

      movieCards.forEach(card => {
        const priceItems = card.querySelectorAll('.price-item');
        let lowestPrice = Infinity;
        let lowestItem = null;

        // 최저가 찾기
        priceItems.forEach(item => {
          const priceText = item.querySelector('.price-amount').textContent;
          const price = parseInt(priceText.replace(/[^0-9]/g, ''));

          if (price < lowestPrice) {
            lowestPrice = price;
            lowestItem = item;
          }
        });

        // 기존 강조 제거
        priceItems.forEach(item => {
          item.classList.remove('lowest-price');
        });

        // 최저가 항목에 강조 추가
        if (lowestItem) {
          lowestItem.classList.add('lowest-price');
          logEvent('가격 비교', {
            action: '최저가 강조',
            movieTitle: card.querySelector('.movie-title').textContent,
            lowestPrice: lowestPrice
          });
        }
      });
    }

    // ============================================
    // 7. AI 추천 카드 애니메이션
    // ============================================

    const aiGrid = document.getElementById('aiGrid');
    const aiCards = document.querySelectorAll('.ai-card');
    let currentFeaturedIndex = 0;

    /**
     * AI 카드 강조 업데이트
     */
    function updateAICardFeatured() {
      aiCards.forEach((card, index) => {
        card.classList.remove('featured');
      });

      aiCards[currentFeaturedIndex].classList.add('featured');

      logEvent('AI 추천 카드', {
        action: '강조 이동',
        currentIndex: currentFeaturedIndex,
        cardTitle: aiCards[currentFeaturedIndex].querySelector('.ai-card-title').textContent
      });

      currentFeaturedIndex = (currentFeaturedIndex + 1) % aiCards.length;
    }

    // AI 카드 애니메이션 시작 (3초 간격)
    setInterval(updateAICardFeatured, 3000);

    // ============================================
    // 8. 검색 버튼 및 기타 이벤트
    // ============================================

    const searchBtn = document.getElementById('searchBtn');
    const nearbyBtn = document.getElementById('nearbyBtn');
    const movieInput = document.getElementById('movieInput');
    const regionInput = document.getElementById('regionInput');

    /**
     * 검색 실행
     */
    function performSearch() {
      const searchData = {
        movie: movieInput.value || '전체',
        region: regionInput.value || '전체',
        date: dateInput.value,
        timeStart: indexToTime(timeSliderState.startIndex),
        timeEnd: indexToTime(timeSliderState.endIndex),
        personCount: currentPersonCount
      };

      logEvent('검색', {
        action: '실행',
        ...searchData
      });

      console.log('검색 조건:', searchData);

      const params = new URLSearchParams(searchData);
      window.location.href = `/basic/priceComparison.html?${params.toString()}`;
    }

    searchBtn.addEventListener('click', performSearch);

    movieInput.addEventListener('keypress', function (event) {
      if (event.key === 'Enter') {
        performSearch();
      }
    });

    nearbyBtn.addEventListener('click', function () {
      logEvent('근처 극장', { action: '클릭' });
      console.log('근처 극장 검색 시작...');
    });

    // ============================================
    // 초기화
    // ============================================

    function initialize() {
      initializeDateInput();
      updateSliderProgress();
      updateHandlePosition();
      highlightLowestPrices();

      logEvent('페이지', { action: '초기화 완료' });
      console.log('='.repeat(50));
      console.log('다보여 - 영화 최저가 찾기 페이지 로드 완료');
      console.log('='.repeat(50));
    }

    // DOM 로드 완료 후 초기화
    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', initialize);
    } else {
      initialize();
    }





    const regionInput2 = document.getElementById("regionInput");
    const regionDropdown = document.getElementById("regionDropdown");

    // Kakao Places 객체 생성
    const ps = new kakao.maps.services.Places();

    /**
     * 지역 검색 실행
     */
    function searchRegion(keyword) {
      if (!keyword) {
        regionDropdown.style.display = "none";
        return;
      }

      ps.keywordSearch(keyword, function (data, status) {
        if (status === kakao.maps.services.Status.OK) {
          renderDropdown(data);
        } else {
          regionDropdown.style.display = "none";
        }
      });
    }