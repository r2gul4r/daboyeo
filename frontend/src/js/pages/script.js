document.addEventListener('DOMContentLoaded', () => {
  // ============================================
  // 1. 전역 변수 및 요소 선택
  // ============================================

  // 날짜 관련
  const dateInput = document.getElementById("dateInput");
  const calendar = document.getElementById("calendar");
  const datesContainer = document.getElementById("calendarDates");
  const monthYear = document.getElementById("monthYear");
  const prevMonthBtn = document.getElementById("prevMonth");
  const nextMonthBtn = document.getElementById("nextMonth");

  // 인원 관련 (HTML ID: personCount, increaseBtn, decreaseBtn)
  const personCountDisplay = document.getElementById('personCount');
  const increaseBtn = document.getElementById('increaseBtn');
  const decreaseBtn = document.getElementById('decreaseBtn');

  // 검색 및 기타
  const searchBtn = document.getElementById('searchBtn');
  const nearbyBtn = document.getElementById('nearbyBtn');
  const regionInput = document.getElementById("regionInput");

  let currentDate = new Date(); // 달력 표시용 기준 날짜
  let currentPersonCount = 1;   // 선택된 인원 수 (기본값 1)

  // ============================================
  // 2. 날짜 선택 기능 (커스텀 캘린더)
  // ============================================

  /**
   * 날짜 입력창 초기화 (오늘 날짜 세팅)
   */
  function initializeDateInput() {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, "0");
    const day = String(today.getDate()).padStart(2, "0");
    const todayString = `${year}-${month}-${day}`;

    dateInput.value = todayString;
  }

  /**
   * 캘린더 렌더링
   */
  function renderCalendar() {
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    monthYear.textContent = `${year}년 ${month + 1}월`;

    const firstDay = new Date(year, month, 1).getDay();
    const lastDate = new Date(year, month + 1, 0).getDate();

    datesContainer.innerHTML = "";

    // 시작 요일 맞추기 위한 빈 칸 생성
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

      // 과거 날짜 비활성화
      if (thisDate < today) {
        dateEl.classList.add("disabled");
      } else {
        dateEl.addEventListener("click", () => {
          const selectedDate = `${year}-${String(month + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
          dateInput.value = selectedDate;
          calendar.style.display = "none";
        });
      }
      datesContainer.appendChild(dateEl);
    }
  }

  // 날짜 입력창 클릭 시 달력 표시
  dateInput.addEventListener("click", (e) => {
    e.stopPropagation();
    calendar.style.display = "block";
    renderCalendar();
  });

  // 이전 달 이동
  prevMonthBtn.addEventListener("click", (e) => {
    e.stopPropagation();
    const today = new Date();
    if (currentDate.getFullYear() > today.getFullYear() ||
       (currentDate.getFullYear() === today.getFullYear() && currentDate.getMonth() > today.getMonth())) {
      currentDate.setMonth(currentDate.getMonth() - 1);
      renderCalendar();
    }
  });

  // 다음 달 이동
  nextMonthBtn.addEventListener("click", (e) => {
    e.stopPropagation();
    currentDate.setMonth(currentDate.getMonth() + 1);
    renderCalendar();
  });

  // 외부 클릭 시 달력 닫기
  document.addEventListener("click", (e) => {
    if (!e.target.closest(".form-group")) {
      calendar.style.display = "none";
    }
  });

  // ============================================
  // 3. 인원 선택 기능 (수정됨)
  // ============================================

  /**
   * 인원 수 화면 업데이트
   */
  function updatePersonCount() {
    if (personCountDisplay) {
      personCountDisplay.textContent = currentPersonCount;
    }
  }

  // 인원 증가 버튼
  if (increaseBtn) {
    increaseBtn.addEventListener('click', (e) => {
      e.preventDefault(); // 기본 동작 방지
      currentPersonCount++;
      updatePersonCount();
    });
  }

  // 인원 감소 버튼 (1명 미만으로 내려가지 않도록 제한)
  if (decreaseBtn) {
    decreaseBtn.addEventListener('click', (e) => {
      e.preventDefault(); // 기본 동작 방지
      if (currentPersonCount > 1) {
        currentPersonCount--;
        updatePersonCount();
      } else {
        console.log("최소 인원은 1명입니다.");
      }
    });
  }

  // ============================================
  // 4. 검색 및 실행 기능
  // ============================================

  /**
   * 검색 데이터 수집 및 페이지 이동
   */
  function performSearch() {
    const timeRadio = document.querySelector('input[name="timeRange"]:checked');
    const selectedTimeRange = timeRadio ? timeRadio.value : 'morning';

    const searchData = {
      region: regionInput.value || '전체',
      date: dateInput.value,
      timeRange: selectedTimeRange,
      personCount: currentPersonCount
    };

    console.log('검색 실행:', searchData);
    alert(`검색을 시작합니다!\n지역: ${searchData.region}\n날짜: ${searchData.date}\n시간대: ${searchData.timeRange}\n인원: ${searchData.personCount}명`);
  }

  if (searchBtn) {
    searchBtn.addEventListener('click', performSearch);
  }

  if (nearbyBtn) {
    nearbyBtn.addEventListener('click', () => {
      console.log('내 위치 기반 주변 극장 검색 시작...');
    });
  }

  // ============================================
  // 5. 초기화 실행
  // ============================================
  initializeDateInput();
  updatePersonCount(); // 초기 인원 수 표시
});