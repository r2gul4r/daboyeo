(() => {
  const SEARCH_CONTEXT_KEY = "daboyeoSearchContext";
  const AI_PAGE_URL = "./src/pages/ai.html";
  const MOVIES_PAGE_URL = "./movies.html";
  const SEAT_MBTI_PAGE_URL = "./src/basic/seatRecommendMbti.html";
  const ALL_LABEL = "전체";

  let isInitialized = false;
  let currentDate = new Date();
  let currentPersonCount = 1;

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

    const regionInput = document.getElementById("selectedRegionInput");
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

    if (!regionInput || !dateInput || !searchBtn || !nearbyBtn) return;
    isInitialized = true;

    function initializeDateInput() {
      if (dateInput.value) return;
      const today = new Date();
      dateInput.value = today.toISOString().split("T")[0];
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

      for (let i = 0; i < firstDay; i += 1) {
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
            dateInput.value = `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
            calendar.style.display = "none";
          });
        }

        datesContainer.appendChild(dateCell);
      }
    }

    function buildSearchContext() {
      const region = String(regionInput.value || "").trim();
      return {
        region: region || ALL_LABEL,
        date: dateInput.value || "",
        timeRange: document.querySelector('input[name="timeRange"]:checked')?.value || "morning",
        personCount: currentPersonCount,
      };
    }

    function restoreSearchContext() {
      const context = readSearchContext();
      if (!context) return;

      if (context.region && context.region !== ALL_LABEL) {
        regionInput.value = context.region;
      }

      if (context.date) {
        dateInput.value = context.date;
        const restoredDate = new Date(context.date);
        if (!Number.isNaN(restoredDate.getTime())) {
          currentDate = restoredDate;
        }
      }

      currentPersonCount = context.personCount || 1;
      if (personCountDisplay) {
        personCountDisplay.textContent = currentPersonCount;
      }

      const timeRadio = document.querySelector(`input[name="timeRange"][value="${context.timeRange}"]`);
      if (timeRadio) {
        timeRadio.checked = true;
      }
    }

    function navigateToMovies() {
      const context = buildSearchContext();
      saveSearchContext(context);

      const address = String(regionInput.value || "").trim();
      if (!address || address === ALL_LABEL) {
        alert("지도에서 지역을 검색해서 주소를 먼저 입력해줘.");
        return;
      }

      const geocoder = new kakao.maps.services.Geocoder();
      geocoder.addressSearch(address, (result, status) => {
        if (status === kakao.maps.services.Status.OK && result.length > 0) {
          const lat = result[0].y;
          const lng = result[0].x;
          window.location.href = `${MOVIES_PAGE_URL}?region=${encodeURIComponent(context.region)}&lat=${lat}&lng=${lng}`;
          return;
        }

        window.location.href = `${MOVIES_PAGE_URL}?region=${encodeURIComponent(context.region)}`;
      });
    }

    document.addEventListener("click", (event) => {
      if (calendar && !event.target.closest(".form-group")) {
        calendar.style.display = "none";
      }
    });

    dateInput.addEventListener("click", (event) => {
      event.stopPropagation();
      calendar.style.display = "block";
      renderCalendar();
    });

    prevMonthBtn?.addEventListener("click", (event) => {
      event.stopPropagation();
      currentDate.setMonth(currentDate.getMonth() - 1);
      renderCalendar();
    });

    nextMonthBtn?.addEventListener("click", (event) => {
      event.stopPropagation();
      currentDate.setMonth(currentDate.getMonth() + 1);
      renderCalendar();
    });

    increaseBtn?.addEventListener("click", (event) => {
      event.preventDefault();
      currentPersonCount += 1;
      if (personCountDisplay) {
        personCountDisplay.textContent = currentPersonCount;
      }
    });

    decreaseBtn?.addEventListener("click", (event) => {
      event.preventDefault();
      currentPersonCount = Math.max(1, currentPersonCount - 1);
      if (personCountDisplay) {
        personCountDisplay.textContent = currentPersonCount;
      }
    });

    searchBtn.addEventListener("click", () => {
      saveSearchContext(buildSearchContext());
      window.location.href = AI_PAGE_URL;
    });

    nearbyBtn.addEventListener("click", (event) => {
      event.preventDefault();
      navigateToMovies();
    });

    if (directCompareBtn) {
      directCompareBtn.addEventListener("click", () => {
        navigateToMovies();
      });
    }

    seatFlowTriggers.forEach((trigger) => {
      trigger.addEventListener("click", () => {
        saveSearchContext(buildSearchContext());
        window.location.href = `${SEAT_MBTI_PAGE_URL}?flow=${encodeURIComponent(trigger.dataset.seatFlow || "mbti")}`;
      });
    });

    const sidoDisplay = document.getElementById("sidoValue");
    const sigunguDisplay = document.getElementById("sigunguValue");
    const guDisplay = document.getElementById("guValue");
    const dongDisplay = document.getElementById("dongValue");

    function updateSplitUI(data) {
      if (!sidoDisplay || !sigunguDisplay || !guDisplay || !dongDisplay) return;

      let sido = ALL_LABEL;
      let sigungu = ALL_LABEL;
      let gu = ALL_LABEL;
      let dong = ALL_LABEL;

      if (typeof data === "string") {
        const cleanText = data.trim();
        if (cleanText && cleanText !== ALL_LABEL) {
          const parts = cleanText.split(" ");
          sido = parts[0] || ALL_LABEL;
          sigungu = parts[1] || ALL_LABEL;
          // If we only have 3 parts, the 3rd part is usually Dong
          if (parts.length === 3) {
            dong = parts[2] || ALL_LABEL;
          } else if (parts.length >= 4) {
            gu = parts[2] || ALL_LABEL;
            dong = parts.slice(3).join(" ") || ALL_LABEL;
          }
        }
      } else if (data && typeof data === "object") {
        sido = data.sido || ALL_LABEL;
        
        // Handle "Suwon-si Paldal-gu" case in region_2depth_name
        const sigunguRaw = data.sigungu || "";
        if (sigunguRaw.includes(" ")) {
          const sParts = sigunguRaw.split(" ");
          sigungu = sParts[0];
          gu = sParts[1];
        } else {
          sigungu = sigunguRaw || ALL_LABEL;
        }
        
        dong = data.dong || ALL_LABEL;
      }

      sidoDisplay.textContent = sido;
      sigunguDisplay.textContent = sigungu;
      guDisplay.textContent = gu;
      dongDisplay.textContent = dong;
    }

    window.updateRegionFromMap = (regionData) => {
      let nextRegion = "";
      if (typeof regionData === "string") {
        nextRegion = regionData.trim();
      } else if (regionData && typeof regionData === "object") {
        nextRegion = String(regionData.full || "").trim();
      }

      if (!nextRegion) {
        console.warn("[RegionSync] Empty region text from map result");
        return;
      }

      regionInput.value = nextRegion;
      updateSplitUI(regionData);
      console.log(`[RegionSync] Updated region input: ${nextRegion}`);
    };

    const regionPartCards = document.querySelectorAll(".region-part-card");
    if (regionPartCards.length > 0 && locationBtn) {
      regionPartCards.forEach(card => {
        card.addEventListener("click", () => {
          locationBtn.click();
        });
      });
    }

    initializeDateInput();
    restoreSearchContext();
    updateSplitUI(regionInput.value);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initHomePage, { once: true });
  } else {
    initHomePage();
  }
})();
