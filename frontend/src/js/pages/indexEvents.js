import { fetchMovieEvents } from "../api/client.js";

const grid = document.getElementById("main-event-grid");

function createSkeletonCard() {
  const card = document.createElement("article");
  card.className = "event-card loading-skeleton";
  card.innerHTML = `
    <div class="card-image-wrapper">
      <div class="skeleton-img"></div>
    </div>
    <div class="card-content">
      <div class="card-badges">
        <span class="skeleton-badge"></span>
        <span class="skeleton-badge"></span>
      </div>
      <div class="skeleton-text skeleton-title"></div>
      <div class="skeleton-text" style="width: 70%;"></div>
      <div class="card-footer">
        <div class="skeleton-text" style="width: 108px; margin-bottom: 0;"></div>
      </div>
    </div>
  `;
  return card;
}

function formatDate(dateValue) {
  if (!dateValue) {
    return "상시 진행";
  }
  return String(dateValue).replaceAll("-", ".");
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

function getProviderBadgeClass(source) {
  return `badge-${String(source || "").trim().toLowerCase().replaceAll(/[^a-z0-9]+/g, "_")}`;
}

function renderLoading() {
  if (!grid) {
    return;
  }
  grid.innerHTML = "";
  for (let index = 0; index < 4; index += 1) {
    grid.appendChild(createSkeletonCard());
  }
}

function renderEmpty() {
  if (!grid) {
    return;
  }
  grid.innerHTML = `
    <div class="event-preview-empty">
      <p>지금 표시할 이벤트가 없어.</p>
      <a href="./src/pages/events.html" class="more-link">이벤트 페이지로 가기</a>
    </div>
  `;
}

function createEventCard(event) {
  const card = document.createElement("article");
  card.className = "event-card";
  const providerLabel = getProviderLabel(event);
  const badgeClass = getProviderBadgeClass(providerLabel);
  const dday = event.dday || event.dDay || "";

  card.innerHTML = `
    <div class="card-image-wrapper" style="pointer-events: none;">
      <img src="${event.imageUrl || "https://via.placeholder.com/400x225?text=No+Image"}"
           alt="${event.title || "이벤트 이미지"}"
           loading="lazy">
      ${dday ? `<span class="d-day-badge">${dday}</span>` : ""}
      <div class="card-overlay">
        <span>자세히 보기 <i class="fas fa-arrow-right"></i></span>
      </div>
    </div>
    <div class="card-content" style="pointer-events: none;">
      <div class="card-badges">
        <span class="badge ${badgeClass}">${providerLabel}</span>
        <span class="badge badge-category">${event.category || "ALL"}</span>
      </div>
      <h3 class="card-title">${event.title || "제목 없음"}</h3>
      <div class="card-footer">
        <p class="card-date"><i class="far fa-calendar-alt"></i> ${formatDate(event.startDate)} ~ ${formatDate(event.endDate)}</p>
      </div>
    </div>
  `;

  card.addEventListener("click", () => {
    window.location.href = "./src/pages/events.html";
  });

  return card;
}

async function loadPreview() {
  if (!grid) {
    return;
  }

  renderLoading();

  try {
    const events = await fetchMovieEvents();
    if (!Array.isArray(events) || events.length === 0) {
      renderEmpty();
      return;
    }

    grid.innerHTML = "";
    for (const event of events.slice(0, 4)) {
      grid.appendChild(createEventCard(event));
    }
  } catch (error) {
    console.error("[indexEvents] failed to load event preview", error);
    renderEmpty();
  }
}

document.addEventListener("DOMContentLoaded", () => {
  void loadPreview();
});
