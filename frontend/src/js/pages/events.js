import { fetchMovieEvents } from "../api/client.js";

const state = {
  provider: "",
  category: "",
};

const elements = {
  grid: document.getElementById("event-grid"),
  empty: document.getElementById("empty-state"),
  error: document.getElementById("error-state"),
  provider: document.getElementById("provider-filter"),
  categoryTabs: document.getElementById("category-tabs"),
  retry: document.getElementById("retry-button"),
};

function formatDate(dateValue) {
  if (!dateValue) {
    return "상시 진행";
  }
  return String(dateValue).replaceAll("-", ".");
}

function getProviderBadgeClass(source) {
  return `badge-${String(source || "").trim().toLowerCase().replaceAll(/[^a-z0-9]+/g, "_")}`;
}

function getProviderLabel(event) {
  const raw = String(event.cinema || event.source || "").trim().toUpperCase();
  if (raw === "LOTTE_CINEMA" || raw === "LOTTE") {
    return "LOTTE";
  }
  if (raw === "MEGA") {
    return "MEGABOX";
  }
  return raw || "EVENT";
}

function hideStates() {
  elements.empty?.classList.add("hidden");
  elements.error?.classList.add("hidden");
}

function showEmpty() {
  elements.grid.innerHTML = "";
  elements.empty?.classList.remove("hidden");
  elements.error?.classList.add("hidden");
}

function showError() {
  elements.grid.innerHTML = "";
  elements.error?.classList.remove("hidden");
  elements.empty?.classList.add("hidden");
}

function createSkeletonCard() {
  const skeleton = document.createElement("article");
  skeleton.className = "event-card loading-skeleton";
  skeleton.innerHTML = `
    <div class="card-image-wrapper">
      <div class="skeleton-img"></div>
    </div>
    <div class="card-content">
      <div class="card-badges">
        <span class="skeleton-badge"></span>
        <span class="skeleton-badge"></span>
      </div>
      <div class="skeleton-text skeleton-title"></div>
      <div class="skeleton-text" style="width: 72%;"></div>
      <div class="card-footer">
        <div class="skeleton-text" style="width: 118px; margin-bottom: 0;"></div>
      </div>
    </div>
  `;
  return skeleton;
}

function renderLoading() {
  hideStates();
  elements.grid.innerHTML = "";
  for (let index = 0; index < 6; index += 1) {
    elements.grid.appendChild(createSkeletonCard());
  }
}

function createEventCard(event) {
  const card = document.createElement("article");
  card.className = "event-card";
  card.dataset.url = event.eventUrl || "";

  const providerLabel = getProviderLabel(event);
  const badgeClass = getProviderBadgeClass(providerLabel);
  const categoryLabel = event.category || "ALL";
  const dday = event.dday || event.dDay || "";

  card.innerHTML = `
    <div class="card-image-wrapper" style="pointer-events: none;">
      <img src="${event.imageUrl || "https://via.placeholder.com/400x225?text=No+Image"}"
           alt="${event.title || "이벤트 이미지"}"
           loading="lazy">
      ${dday ? `<span class="d-day-badge">${dday}</span>` : ""}
      <div class="card-overlay">
        <span>이벤트 보기 <i class="fas fa-external-link-alt"></i></span>
      </div>
    </div>
    <div class="card-content" style="pointer-events: none;">
      <div class="card-badges">
        <span class="badge ${badgeClass}">${providerLabel}</span>
        <span class="badge badge-category">${categoryLabel}</span>
      </div>
      <h3 class="card-title">${event.title || "제목 없음"}</h3>
      <div class="card-footer">
        <p class="card-date"><i class="far fa-calendar-alt"></i> ${formatDate(event.startDate)} ~ ${formatDate(event.endDate)}</p>
      </div>
    </div>
  `;

  card.addEventListener("click", () => {
    if (!card.dataset.url) {
      return;
    }
    window.open(card.dataset.url, "_blank", "noopener,noreferrer");
  });

  return card;
}

function renderEvents(events) {
  elements.grid.innerHTML = "";
  if (!Array.isArray(events) || events.length === 0) {
    showEmpty();
    return;
  }

  hideStates();
  const fragment = document.createDocumentFragment();
  for (const event of events) {
    fragment.appendChild(createEventCard(event));
  }
  elements.grid.appendChild(fragment);
}

async function loadEvents() {
  renderLoading();
  try {
    const events = await fetchMovieEvents({
      source: state.provider,
      category: state.category,
    });
    renderEvents(events);
  } catch (error) {
    console.error("[events] failed to load events", error);
    showError();
  }
}

function bindFilters() {
  elements.provider?.addEventListener("change", (event) => {
    state.provider = event.target.value;
    void loadEvents();
  });

  elements.categoryTabs?.addEventListener("click", (event) => {
    const button = event.target.closest(".tab-btn");
    if (!button) {
      return;
    }

    for (const tab of elements.categoryTabs.querySelectorAll(".tab-btn")) {
      tab.classList.remove("active");
    }
    button.classList.add("active");
    state.category = button.dataset.category || "";
    void loadEvents();
  });

  elements.retry?.addEventListener("click", () => {
    void loadEvents();
  });
}

document.addEventListener("DOMContentLoaded", () => {
  bindFilters();
  void loadEvents();
});
