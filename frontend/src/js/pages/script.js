import { REGIONS } from "../constants/regions.js";

(() => {
  const SEARCH_CONTEXT_KEY = "daboyeoSearchContext";
  const AI_PAGE_URL = "./src/pages/ai.html";
  const MOVIES_PAGE_URL = "./movies.html";
  const SEAT_MBTI_PAGE_URL = "./src/basic/seatRecommendMbti.html";

  let isInitialized = false;
  let currentDate = new Date();
  let currentPersonCount = 1;

  let selectedSido = "";
  let selectedGugun = "";
  let selectedDong = "";

  function saveSearchContext(searchContext) {
    sessionStorage.setItem(SEARCH_CONTEXT_KEY, JSON.stringify(searchContext));
  }

  function readSearchContext() {
    try {
      const raw = sessionStorage.getItem(SEARCH_CONTEXT_KEY);
      if (!raw) return null;
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }

  function initHomePage() {
    if (isInitialized) return;

    // Elements
    const sidoContainer = document.getElementById("sidoContainer");
    const gugunContainer = document.getElementById("gugunContainer");
    const dongContainer = document.getElementById("dongContainer");
    
    const sidoDisplay = document.getElementById("sidoDisplay");
    const gugunDisplay = document.getElementById("gugunDisplay");
    const dongDisplay = document.getElementById("dongDisplay");
    
    const sidoOptions = document.getElementById("sidoOptions");
    const gugunOptions = document.getElementById("gugunOptions");
    const dongOptions = document.getElementById("dongOptions");

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
    const directCompareBtn = document.getElementById("directCompareBtn");
    
    const seatFlowTriggers = document.querySelectorAll("[data-seat-flow]");

    if (!sidoContainer || !searchBtn) return;
    isInitialized = true;

    // --- Custom Select Logic ---
    function closeAllSelects() {
      document.querySelectorAll(".custom-select").forEach(el => el.classList.remove("active"));
    }

    function setupCustomSelect(container, display, optionsList, onSelect) {
      display.addEventListener("click", (e) => {
        e.stopPropagation();
        if (container.classList.contains("disabled")) return;
        const isActive = container.classList.contains("active");
        closeAllSelects();
        if (!isActive) container.classList.add("active");
      });

      optionsList.addEventListener("click", (e) => {
        const li = e.target.closest("li");
        if (!li) return;
        const value = li.dataset.value;
        const text = li.textContent;
        
        display.querySelector("span").textContent = text;
        optionsList.querySelectorAll("li").forEach(item => item.classList.remove("selected"));
        li.classList.add("selected");
        
        container.classList.remove("active");
        onSelect(value);
      });
    }

    function populateOptions(listEl, options) {
      listEl.innerHTML = "";
      options.forEach(opt => {
        const li = document.createElement("li");
        li.dataset.value = opt;
        li.textContent = opt;
        listEl.appendChild(li);
      });
    }

    setupCustomSelect(sidoContainer, sidoDisplay, sidoOptions, (val) => {
      selectedSido = val;
      selectedGugun = "";
      selectedDong = "";
      
      gugunDisplay.querySelector("span").textContent = "시/군/구 선택";
      dongDisplay.querySelector("span").textContent = "읍/면/동 선택";
      
      const guguns = Object.keys(REGIONS[val] || {});
      populateOptions(gugunOptions, guguns);
      gugunContainer.classList.remove("disabled");
      dongContainer.classList.add("disabled");
    });

    setupCustomSelect(gugunContainer, gugunDisplay, gugunOptions, (val) => {
      selectedGugun = val;
      selectedDong = "";
      dongDisplay.querySelector("span").textContent = "읍/면/동 선택";
      
      const dongs = REGIONS[selectedSido][val] || [];
      populateOptions(dongOptions, ["전체", ...dongs]);
      dongContainer.classList.remove("disabled");
    });

    setupCustomSelect(dongContainer, dongDisplay, dongOptions, (val) => {
      selectedDong = val;
    });

    // --- Calendar Logic ---
    function initializeDateInput() {
      if (dateInput.value) return;
      const today = new Date();
      dateInput.value = today.toISOString().split('T')[0];
    }

    function renderCalendar() {
      if (!datesContainer || !monthYear) return;
      const year = currentDate.getFullYear();
      const month = currentDate.getMonth();
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      monthYear.textContent = `${year}.${String(month + 1).padStart(2, "0")}`;
      datesContainer.innerHTML = "";

      const firstDay = new Date(year, month, 1).getDay();
      const lastDate = new Date(year, month + 1, 0).getDate();

      for (let i = 0; i < firstDay; i++) datesContainer.appendChild(document.createElement("div"));

      for (let day = 1; day <= lastDate; day++) {
        const dateCell = document.createElement("div");
        const thisDate = new Date(year, month, day);
        dateCell.textContent = String(day);

        if (thisDate.getTime() === today.getTime()) dateCell.classList.add("today");
        if (thisDate < today) {
          dateCell.classList.add("disabled");
        } else {
          dateCell.addEventListener("click", () => {
            dateInput.value = `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
            calendar.style.display = "none";
          });
        }
        datesContainer.appendChild(dateCell);
      }
    }

    // --- Context & Search Logic ---
    function buildSearchContext() {
      const region = `${selectedSido} ${selectedGugun} ${selectedDong === "전체" ? "" : selectedDong}`.trim();
      return {
        region: region || "전체",
        date: dateInput.value || "",
        timeRange: document.querySelector('input[name="timeRange"]:checked')?.value || "morning",
        personCount: currentPersonCount
      };
    }

    function restoreSearchContext() {
      const context = readSearchContext();
      if (!context) return;

      if (context.date) {
        dateInput.value = context.date;
        const restoredDate = new Date(context.date);
        if (!isNaN(restoredDate)) currentDate = restoredDate;
      }
      currentPersonCount = context.personCount || 1;
      if (personCountDisplay) personCountDisplay.textContent = currentPersonCount;
      
      const timeRadio = document.querySelector(`input[name="timeRange"][value="${context.timeRange}"]`);
      if (timeRadio) timeRadio.checked = true;
    }

    // Event Listeners
    populateOptions(sidoOptions, Object.keys(REGIONS));

    document.addEventListener("click", (e) => {
      if (!e.target.closest(".custom-select")) closeAllSelects();
      if (calendar && !e.target.closest(".form-group")) calendar.style.display = "none";
    });

    dateInput?.addEventListener("click", (e) => {
      e.stopPropagation();
      calendar.style.display = "block";
      renderCalendar();
    });

    prevMonthBtn?.addEventListener("click", (e) => {
      e.stopPropagation();
      currentDate.setMonth(currentDate.getMonth() - 1);
      renderCalendar();
    });

    nextMonthBtn?.addEventListener("click", (e) => {
      e.stopPropagation();
      currentDate.setMonth(currentDate.getMonth() + 1);
      renderCalendar();
    });

    increaseBtn?.addEventListener("click", (e) => {
      e.preventDefault();
      currentPersonCount++;
      if (personCountDisplay) personCountDisplay.textContent = currentPersonCount;
    });

    decreaseBtn?.addEventListener("click", (e) => {
      e.preventDefault();
      currentPersonCount = Math.max(1, currentPersonCount - 1);
      if (personCountDisplay) personCountDisplay.textContent = currentPersonCount;
    });

    searchBtn.addEventListener("click", () => {
      saveSearchContext(buildSearchContext());
      window.location.href = AI_PAGE_URL;
    });

    nearbyBtn.addEventListener("click", (e) => {
      e.preventDefault();
      
      if (!selectedSido || !selectedGugun) {
        alert("지역(시/도, 시/군/구)을 선택해주세요.");
        return;
      }

      const context = buildSearchContext();
      saveSearchContext(context);

      // --- Geocoding selected region ---
      const geocoder = new kakao.maps.services.Geocoder();
      const address = `${selectedSido} ${selectedGugun} ${selectedDong === "전체" ? "" : selectedDong}`.trim();
      
      geocoder.addressSearch(address, (result, status) => {
        if (status === kakao.maps.services.Status.OK) {
          const lat = result[0].y;
          const lng = result[0].x;
          window.location.href = `${MOVIES_PAGE_URL}?region=${encodeURIComponent(context.region)}&lat=${lat}&lng=${lng}`;
        } else {
          // Fallback if geocoding fails (e.g. very new address)
          window.location.href = `${MOVIES_PAGE_URL}?region=${encodeURIComponent(context.region)}`;
        }
      });
    });

    if (directCompareBtn) {
      directCompareBtn.addEventListener("click", () => {
        nearbyBtn.click();
      });
    }

    seatFlowTriggers.forEach(trigger => {
      trigger.addEventListener("click", () => {
        saveSearchContext(buildSearchContext());
        window.location.href = `${SEAT_MBTI_PAGE_URL}?flow=${encodeURIComponent(trigger.dataset.seatFlow || "mbti")}`;
      });
    });

    initializeDateInput();
    restoreSearchContext();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initHomePage, { once: true });
  } else {
    initHomePage();
  }
})();
