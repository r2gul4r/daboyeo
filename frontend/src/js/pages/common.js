// ============================================
// Common UI bootstrap
// ============================================

(() => {
  let isInitialized = false;

  function emitLogEvent(eventType, data) {
    if (typeof window.logEvent === "function") {
      window.logEvent(eventType, data);
    }
  }

  const theaterData = {
    cgv: {
      서울: ["강남", "강변", "건대입구", "구로", "대학로", "동대문", "등촌", "명동", "목동", "미아", "상봉", "성신여대입구", "송파", "수유", "여의도", "영등포", "왕십리", "용산아이파크몰", "중계", "천호", "청담씨네시티", "피카디리1958", "하계", "홍대"],
      경기: ["고양행신", "광교", "구리", "기흥", "김포", "동백", "동수원", "동탄", "범계", "부천", "부천역", "분당", "서현", "수원", "수원역", "안산", "오리", "용인", "의정부", "이천", "일산", "죽전", "판교", "평촌", "평택", "하남미사", "화정"],
      인천: ["계양", "부평", "송도", "인천", "인천가정", "인천도화", "주안역", "청라"],
    },
    megabox: {
      서울: ["강남", "강동", "군자", "동대문", "마곡", "목동", "상봉", "성수", "신촌", "이수", "창동", "코엑스", "홍대", "화곡"],
      경기: ["고양스타필드", "광명AK플라자", "구리", "남양주", "동탄", "미사강변", "분당", "수원스타필드", "안성스타필드", "영통", "의정부민락", "킨텍스", "하남스타필드"],
      인천: ["검단", "송도", "영종", "인천논현", "청라지젤"],
    },
    lotte: {
      서울: ["가든파이브", "가산디지털", "건대입구", "김포공항", "노원", "도곡", "서울대입구", "신림", "영등포", "용산", "월드타워", "은평", "청량리", "합정"],
      경기: ["광교", "광명", "구리", "기흥", "동탄", "병점", "부천역", "성남중앙", "수원", "안산", "안양일번가", "오산", "용인기흥", "의정부민락", "일산", "주엽", "파주운정", "평촌"],
      인천: ["부평", "부평역사", "송도", "인천아시아드", "청라"],
    },
  };

  function renderTheaters(grid, brand) {
    const data = theaterData[brand];
    if (!grid || !data) {
      return;
    }

    let html = "";

    for (const region in data) {
      html += `
        <div class="region-row">
          <div class="region-name">${region}</div>
          <div class="theater-list">
            ${data[region].map((name) => `<span class="theater-item">${name}</span>`).join("")}
          </div>
        </div>
      `;
    }

    grid.innerHTML = html;
  }

  function initCommonUi() {
    if (isInitialized) {
      return;
    }

    const fixedHeader = document.getElementById("fixedHeader");
    const headerSearchBtn = document.getElementById("headerSearchBtn");
    const scrollToTopBtn = document.getElementById("scrollToTop");
    const theaterSearchBtn = document.getElementById("theaterSearchBtn");
    const overlay = document.getElementById("theaterOverlay");
    const closeBtn = document.getElementById("closeOverlay");
    const grid = document.getElementById("theaterGrid");
    const tabItems = Array.from(document.querySelectorAll(".overlay-tab-item"));
    const dropdown = document.querySelector(".custom-dropdown");
    const selected = dropdown?.querySelector(".selected");
    const items = Array.from(dropdown?.querySelectorAll(".dropdown-list li") || []);

    if (!fixedHeader && !scrollToTopBtn && !theaterSearchBtn && !dropdown) {
      return;
    }

    isInitialized = true;

    const handleScroll = () => {
      const scrollTop = window.scrollY;

      if (fixedHeader) {
        const shouldShowHeader = scrollTop > 100;

        if (shouldShowHeader && !fixedHeader.classList.contains("visible")) {
          fixedHeader.classList.add("visible");
          document.body.classList.add("header-visible");
          emitLogEvent("고정 헤더", { action: "표시", scrollPosition: scrollTop });
        } else if (!shouldShowHeader && fixedHeader.classList.contains("visible")) {
          fixedHeader.classList.remove("visible");
          document.body.classList.remove("header-visible");
          emitLogEvent("고정 헤더", { action: "숨김", scrollPosition: scrollTop });
        }
      }

      if (scrollToTopBtn) {
        if (scrollTop > 300 && !scrollToTopBtn.classList.contains("visible")) {
          scrollToTopBtn.classList.add("visible");
          emitLogEvent("맨 위로 버튼", { action: "표시", scrollPosition: scrollTop });
        } else if (scrollTop <= 300 && scrollToTopBtn.classList.contains("visible")) {
          scrollToTopBtn.classList.remove("visible");
          emitLogEvent("맨 위로 버튼", { action: "숨김", scrollPosition: scrollTop });
        }
      }
    };

    window.addEventListener("scroll", handleScroll, { passive: true });
    handleScroll();

    headerSearchBtn?.addEventListener("click", () => {
      emitLogEvent("헤더 검색 버튼", { action: "클릭" });

      const movieInput = document.getElementById("movieInput");
      const searchCard = document.querySelector(".search-card");

      if (movieInput && searchCard) {
        movieInput.focus();
        searchCard.scrollIntoView({ behavior: "smooth" });
        return;
      }

      const homeUrl = headerSearchBtn.dataset.homeUrl;
      if (homeUrl) {
        window.location.href = homeUrl;
      }
    });

    scrollToTopBtn?.addEventListener("click", () => {
      emitLogEvent("맨 위로 버튼", { action: "클릭" });
      window.scrollTo({ top: 0, behavior: "smooth" });
    });

    theaterSearchBtn?.addEventListener("click", (event) => {
      if (!overlay) {
        return;
      }

      event.stopPropagation();
      overlay.classList.toggle("active");

      if (overlay.classList.contains("active")) {
        const activeTab = document.querySelector(".overlay-tab-item.active");
        if (activeTab) {
          renderTheaters(grid, activeTab.dataset.brand);
        }
      }
    });

    closeBtn?.addEventListener("click", () => {
      overlay?.classList.remove("active");
    });

    tabItems.forEach((tab) => {
      tab.addEventListener("click", () => {
        tabItems.forEach((item) => item.classList.remove("active"));
        tab.classList.add("active");
        renderTheaters(grid, tab.dataset.brand);
      });
    });

    selected?.addEventListener("click", (event) => {
      event.stopPropagation();
      dropdown.classList.toggle("active");
    });

    items.forEach((item) => {
      item.addEventListener("click", () => {
        const url = item.dataset.url;

        if (selected) {
          selected.textContent = item.textContent;
        }

        dropdown?.classList.remove("active");

        if (!url) {
          return;
        }

        if (url.startsWith("http")) {
          window.open(url, "_blank", "noopener");
          return;
        }

        window.location.href = url;
      });
    });

    document.addEventListener("click", (event) => {
      if (dropdown && !dropdown.contains(event.target)) {
        dropdown.classList.remove("active");
      }

      if (overlay?.classList.contains("active") && !overlay.contains(event.target) && event.target !== theaterSearchBtn) {
        overlay.classList.remove("active");
      }
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initCommonUi, { once: true });
  } else {
    initCommonUi();
  }

  document.addEventListener("daboyeo:components-loaded", initCommonUi);
  window.initCommonUi = initCommonUi;
})();
