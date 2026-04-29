document.addEventListener("DOMContentLoaded", () => {
  const SEARCH_CONTEXT_KEY = "daboyeoSearchContext";
  const AI_PAGE_URL = "./src/pages/daboyeoAi.html";
  const DIRECT_COMPARE_PAGE_URL = "./src/pages/movies.html";
  const SEAT_MBTI_PAGE_URL = "./src/pages/seatRecommendMbti.html";
  const TIME_RANGE_PARAMS = {
    morning: { timeStart: "06:00", timeEnd: "10:59" },
    brunch: { timeStart: "11:00", timeEnd: "16:59" },
    night: { timeStart: "17:00", timeEnd: "23:59" },
  };

  const dateInput = document.getElementById("dateInput");
  const calendar = document.getElementById("calendar");
  const datesContainer = document.getElementById("calendarDates");
  const monthYear = document.getElementById("monthYear");
  const prevMonthBtn = document.getElementById("prevMonth");
  const nextMonthBtn = document.getElementById("nextMonth");
  const personCountDisplay = document.getElementById("personCount");
  const increaseBtn = document.getElementById("increaseBtn");
  const decreaseBtn = document.getElementById("decreaseBtn");
  const searchBtn = document.getElementById("searchBtn");
  const nearbyBtn = document.getElementById("nearbyBtn");
  const regionInput = document.getElementById("regionInput");
  const seatFlowTriggers = document.querySelectorAll("[data-seat-flow]");

  let currentDate = new Date();
  let currentPersonCount = 1;

  function initializeDateInput() {
    if (!dateInput || dateInput.value) {
      return;
    }

    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, "0");
    const day = String(today.getDate()).padStart(2, "0");
    dateInput.value = `${year}-${month}-${day}`;
  }

  function renderCalendar() {
    if (!datesContainer || !monthYear) {
      return;
    }

    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    monthYear.textContent = `${year}.${String(month + 1).padStart(2, "0")}`;
    datesContainer.innerHTML = "";

    const firstDay = new Date(year, month, 1).getDay();
    const lastDate = new Date(year, month + 1, 0).getDate();

    for (let index = 0; index < firstDay; index += 1) {
      datesContainer.appendChild(document.createElement("div"));
    }

    for (let day = 1; day <= lastDate; day += 1) {
      const dateCell = document.createElement("div");
      const thisDate = new Date(year, month, day);
      dateCell.textContent = String(day);

      if (thisDate.getTime() === today.getTime()) {
        dateCell.classList.add("today");
      }

      if (thisDate < today) {
        dateCell.classList.add("disabled");
      } else {
        dateCell.addEventListener("click", () => {
          const selectedDate = `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
          dateInput.value = selectedDate;
          if (calendar) {
            calendar.style.display = "none";
          }
        });
      }

      datesContainer.appendChild(dateCell);
    }
  }

  function updatePersonCount() {
    if (personCountDisplay) {
      personCountDisplay.textContent = String(currentPersonCount);
    }
  }

  function selectedTimeRange() {
    return document.querySelector('input[name="timeRange"]:checked')?.value || "morning";
  }

  function normalizeRegion(value) {
    const trimmed = value?.trim() || "";
    return trimmed || "전체";
  }

  function buildSearchContext() {
    return {
      region: normalizeRegion(regionInput?.value),
      date: dateInput?.value || "",
      timeRange: selectedTimeRange(),
      personCount: currentPersonCount,
    };
  }

  function saveSearchContext(searchContext) {
    sessionStorage.setItem(SEARCH_CONTEXT_KEY, JSON.stringify(searchContext));
  }

  function readSearchContext() {
    try {
      const raw = sessionStorage.getItem(SEARCH_CONTEXT_KEY);
      if (!raw) {
        return null;
      }

      const parsed = JSON.parse(raw);
      if (!parsed || typeof parsed !== "object") {
        return null;
      }

      const personCount = Number(parsed.personCount);
      return {
        region: normalizeRegion(parsed.region),
        date: typeof parsed.date === "string" ? parsed.date : "",
        timeRange: typeof parsed.timeRange === "string" ? parsed.timeRange : "morning",
        personCount: Number.isFinite(personCount) && personCount > 0 ? Math.floor(personCount) : 1,
      };
    } catch {
      return null;
    }
  }

  function restoreSearchContext() {
    const context = readSearchContext();
    if (!context) {
      return;
    }

    if (regionInput) {
      regionInput.value = context.region === "전체" ? "" : context.region;
    }

    if (dateInput && context.date) {
      dateInput.value = context.date;
      const restoredDate = new Date(`${context.date}T00:00:00`);
      if (!Number.isNaN(restoredDate.getTime())) {
        currentDate = restoredDate;
      }
    }

    currentPersonCount = context.personCount;
    updatePersonCount();

    const timeRadio = document.querySelector(`input[name="timeRange"][value="${context.timeRange}"]`);
    if (timeRadio) {
      timeRadio.checked = true;
    }
  }

  function performSearch() {
    saveSearchContext(buildSearchContext());
    window.location.href = AI_PAGE_URL;
  }

  function buildDirectCompareUrl(searchContext) {
    const timeRange = TIME_RANGE_PARAMS[searchContext.timeRange] || TIME_RANGE_PARAMS.morning;
    const params = new URLSearchParams({
      region: searchContext.region,
      date: searchContext.date,
      timeStart: timeRange.timeStart,
      timeEnd: timeRange.timeEnd,
      personCount: String(searchContext.personCount),
    });

    return `${DIRECT_COMPARE_PAGE_URL}?${params.toString()}`;
  }

  function openDirectCompare(event) {
    event?.preventDefault();
    const searchContext = buildSearchContext();
    saveSearchContext(searchContext);
    window.location.href = buildDirectCompareUrl(searchContext);
  }

  function openSeatFlow(flow) {
    saveSearchContext(buildSearchContext());
    window.location.href = `${SEAT_MBTI_PAGE_URL}?flow=${encodeURIComponent(flow || "mbti")}`;
  }

  if (dateInput && calendar) {
    dateInput.addEventListener("click", (event) => {
      event.stopPropagation();
      calendar.style.display = "block";
      renderCalendar();
    });
  }

  if (prevMonthBtn) {
    prevMonthBtn.addEventListener("click", (event) => {
      event.stopPropagation();
      const today = new Date();
      if (
        currentDate.getFullYear() > today.getFullYear()
        || (currentDate.getFullYear() === today.getFullYear() && currentDate.getMonth() > today.getMonth())
      ) {
        currentDate.setMonth(currentDate.getMonth() - 1);
        renderCalendar();
      }
    });
  }

  if (nextMonthBtn) {
    nextMonthBtn.addEventListener("click", (event) => {
      event.stopPropagation();
      currentDate.setMonth(currentDate.getMonth() + 1);
      renderCalendar();
    });
  }

  if (calendar) {
    document.addEventListener("click", (event) => {
      if (!event.target.closest(".form-group")) {
        calendar.style.display = "none";
      }
    });
  }

  if (increaseBtn) {
    increaseBtn.addEventListener("click", (event) => {
      event.preventDefault();
      currentPersonCount += 1;
      updatePersonCount();
    });
  }

  if (decreaseBtn) {
    decreaseBtn.addEventListener("click", (event) => {
      event.preventDefault();
      currentPersonCount = Math.max(1, currentPersonCount - 1);
      updatePersonCount();
    });
  }

  if (searchBtn) {
    searchBtn.addEventListener("click", performSearch);
  }

  if (nearbyBtn) {
    nearbyBtn.addEventListener("click", openDirectCompare);
  }

  seatFlowTriggers.forEach((trigger) => {
    const open = () => openSeatFlow(trigger.dataset.seatFlow);

    trigger.addEventListener("click", open);
    trigger.addEventListener("keydown", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        open();
      }
    });
  });

  initializeDateInput();
  restoreSearchContext();
  updatePersonCount();
});
