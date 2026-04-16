(() => {
  const SEARCH_RADIUS = 5000;
  const DEFAULT_POSITION = { lat: 37.5665, lng: 126.9780 };
  const BRAND_CONFIG = {
    CGV: { className: "brand-cgv", color: "#ff4c4c", shortName: "C" },
    롯데시네마: { className: "brand-lotte", color: "#3f8cff", shortName: "L" },
    메가박스: { className: "brand-mega", color: "#1ac878", shortName: "M" },
  };

  const state = {
    map: null,
    placesService: null,
    infoWindow: null,
    userMarker: null,
    userOverlay: null,
    radiusCircle: null,
    placeMarkers: [],
    placeOverlays: [],
    allPlaces: [],
    selectedBrand: "all",
    currentPosition: null,
    highlightedPlaceId: null,
  };

  const elements = {};

  function getBrand(placeName = "") {
    if (placeName.includes("CGV")) return "CGV";
    if (placeName.includes("롯데시네마")) return "롯데시네마";
    if (placeName.includes("메가박스")) return "메가박스";
    return null;
  }

  function createMarkerSvg(color, iconText) {
    return `
      <svg xmlns="http://www.w3.org/2000/svg" width="44" height="54" viewBox="0 0 44 54">
        <path d="M22 2C11.5 2 3 10.5 3 21c0 14.7 19 31 19 31s19-16.3 19-31C41 10.5 32.5 2 22 2z" fill="${color}" stroke="${color}" stroke-width="2"/>
        <circle cx="22" cy="21" r="11" fill="white" opacity="0.96"/>
        <text x="22" y="25" text-anchor="middle" font-size="11" font-family="Arial, sans-serif" font-weight="700" fill="${color}">${iconText}</text>
      </svg>
    `.trim();
  }

  function createUserMarkerSvg() {
    return `
      <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 28 28">
        <circle cx="14" cy="14" r="10" fill="#ffffff" fill-opacity="0.96" stroke="#6f63f6" stroke-width="3"/>
        <circle cx="14" cy="14" r="4" fill="#6f63f6"/>
      </svg>
    `.trim();
  }

  function markerImageFromSvg(svg, width, height) {
    const encoded = `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`;
    return new kakao.maps.MarkerImage(encoded, new kakao.maps.Size(width, height), {
      offset: new kakao.maps.Point(width / 2, height),
    });
  }

  function formatDistance(distanceInMeters) {
    const meters = Number(distanceInMeters) || 0;
    return meters < 1000 ? `${meters}m` : `${(meters / 1000).toFixed(1)}km`;
  }

  function escapeHtml(value) {
    return String(value)
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#39;");
  }

  function setLoading(isLoading, message) {
    if (!elements.mapLoading) return;

    elements.mapLoading.classList.toggle("is-hidden", !isLoading);

    const text = elements.mapLoading.querySelector("span");
    if (text && message) {
      text.textContent = message;
    }
  }

  function setLocationMessage(message) {
    if (elements.locationAccuracy) {
      elements.locationAccuracy.textContent = message;
    }
  }

  function setLocationNote(message) {
    if (elements.locationNote) {
      elements.locationNote.textContent = message;
    }
  }

  function setEmptyState(visible, message) {
    if (!elements.emptyState) return;

    elements.emptyState.classList.toggle("is-hidden", !visible);

    if (message) {
      elements.emptyState.textContent = message;
    }
  }

  function updateCount(count) {
    if (elements.countNum) {
      elements.countNum.textContent = String(count);
    }
  }

  function clearPlaceLayers() {
    state.placeMarkers.forEach((marker) => marker.setMap(null));
    state.placeOverlays.forEach((overlay) => overlay.setMap(null));
    state.placeMarkers = [];
    state.placeOverlays = [];
    state.highlightedPlaceId = null;

    if (state.infoWindow) {
      state.infoWindow.close();
    }
  }

  function clearSearchResults() {
    clearPlaceLayers();
    state.allPlaces = [];

    if (elements.placesList) {
      elements.placesList.innerHTML = "";
    }

    updateCount(0);
    setEmptyState(false);
  }

  function getVisiblePlaces() {
    if (state.selectedBrand === "all") {
      return state.allPlaces;
    }

    return state.allPlaces.filter((place) => place.brand === state.selectedBrand);
  }

  function highlightPlace(placeId) {
    state.highlightedPlaceId = placeId;

    document.querySelectorAll(".place-card").forEach((card) => {
      card.classList.toggle("is-active", card.dataset.placeId === placeId);
    });
  }

  function moveToPlace(place) {
    const latLng = new kakao.maps.LatLng(place.lat, place.lng);
    state.map.panTo(latLng);
    highlightPlace(place.id);
  }

  function renderList(places) {
    if (!elements.placesList) return;

    elements.placesList.innerHTML = "";

    if (places.length === 0) {
      setEmptyState(true, "선택한 브랜드의 영화관이 현재 반경 5km 내에 없습니다.");
      return;
    }

    setEmptyState(false);

    const fragment = document.createDocumentFragment();

    places.forEach((place) => {
      const config = BRAND_CONFIG[place.brand];
      const item = document.createElement("article");

      item.className = "place-card";
      item.dataset.placeId = place.id;
      item.innerHTML = `
        <span class="place-brand ${config.className}">${escapeHtml(place.brand)}</span>
        <div class="place-content">
          <div class="place-name-row">
            <strong class="place-name">${escapeHtml(place.name)}</strong>
            <span class="place-distance">${escapeHtml(place.distanceText)}</span>
          </div>
          <p class="place-address">${escapeHtml(place.address)}</p>
        </div>
        <div class="place-actions">
          <a class="btn-booking" href="${escapeHtml(place.placeUrl)}" target="_blank" rel="noopener noreferrer">예매하기</a>
        </div>
      `;

      item.addEventListener("mouseenter", () => highlightPlace(place.id));
      item.addEventListener("focusin", () => highlightPlace(place.id));
      item.addEventListener("mouseleave", () => highlightPlace(state.highlightedPlaceId));
      item.addEventListener("click", (event) => {
        if (event.target.closest(".btn-booking")) {
          return;
        }

        moveToPlace(place);
      });

      fragment.appendChild(item);
    });

    elements.placesList.appendChild(fragment);
  }

  function renderMapPlaces(places) {
    clearPlaceLayers();

    const bounds = new kakao.maps.LatLngBounds();

    if (state.currentPosition) {
      bounds.extend(state.currentPosition);
    }

    places.forEach((place) => {
      const config = BRAND_CONFIG[place.brand];
      const position = new kakao.maps.LatLng(place.lat, place.lng);

      const marker = new kakao.maps.Marker({
        map: state.map,
        position,
        title: place.name,
        image: markerImageFromSvg(createMarkerSvg(config.color, config.shortName), 44, 54),
      });

      const overlay = new kakao.maps.CustomOverlay({
        position,
        yAnchor: 1.8,
        xAnchor: 0.5,
        clickable: true,
        content: `<div class="map-label ${config.className}">${escapeHtml(place.name)}</div>`,
      });

      overlay.setMap(state.map);

      kakao.maps.event.addListener(marker, "click", () => {
        state.infoWindow.setContent(`
          <div style="padding:10px 12px;min-width:180px;color:#111;line-height:1.5;">
            <strong style="display:block;margin-bottom:4px;">${escapeHtml(place.name)}</strong>
            <div style="font-size:12px;color:#555;">${escapeHtml(place.address)}</div>
            <div style="font-size:12px;font-weight:700;color:${config.color};margin-top:6px;">${escapeHtml(place.distanceText)}</div>
          </div>
        `);
        state.infoWindow.open(state.map, marker);
        moveToPlace(place);
      });

      kakao.maps.event.addListener(marker, "mouseover", () => highlightPlace(place.id));

      state.placeMarkers.push(marker);
      state.placeOverlays.push(overlay);
      bounds.extend(position);
    });

    if (!bounds.isEmpty()) {
      state.map.setBounds(bounds);
    }
  }

  function renderPlaces() {
    const visiblePlaces = getVisiblePlaces();
    updateCount(visiblePlaces.length);
    renderMapPlaces(visiblePlaces);
    renderList(visiblePlaces);
  }

  function updateFilterButtons() {
    if (!elements.brandFilters) return;

    elements.brandFilters.querySelectorAll(".filter-btn").forEach((button) => {
      button.classList.toggle("active", button.dataset.brand === state.selectedBrand);
    });
  }

  function setFilter(brand) {
    state.selectedBrand = brand;
    updateFilterButtons();
    renderPlaces();
  }

  function drawUserLocation(position, accuracy) {
    if (state.userMarker) state.userMarker.setMap(null);
    if (state.userOverlay) state.userOverlay.setMap(null);
    if (state.radiusCircle) state.radiusCircle.setMap(null);

    state.userMarker = new kakao.maps.Marker({
      position,
      map: state.map,
      image: markerImageFromSvg(createUserMarkerSvg(), 28, 28),
      zIndex: 20,
    });

    state.userOverlay = new kakao.maps.CustomOverlay({
      position,
      yAnchor: 2.1,
      xAnchor: 0.5,
      content: '<div class="my-location-badge">내 위치</div>',
    });
    state.userOverlay.setMap(state.map);

    state.radiusCircle = new kakao.maps.Circle({
      center: position,
      radius: SEARCH_RADIUS,
      strokeWeight: 2,
      strokeColor: "#7c6cf0",
      strokeOpacity: 0.8,
      strokeStyle: "solid",
      fillColor: "#7c6cf0",
      fillOpacity: 0.15,
    });
    state.radiusCircle.setMap(state.map);

    const accuracyText = accuracy ? `${Math.round(accuracy)}m` : "측정 불가";
    setLocationMessage(`현재 위치 기준 반경 5km를 탐색 중입니다. 위치 정확도는 약 ${accuracyText}입니다.`);
  }

  function normalizePlaces(data) {
    const uniquePlaces = new Map();

    data.forEach((place, index) => {
      const brand = getBrand(place.place_name);
      if (!brand) return;

      const normalized = {
        id: `${brand}-${place.id || index}-${place.x}-${place.y}`,
        brand,
        name: place.place_name,
        address: place.road_address_name || place.address_name || "주소 정보 없음",
        distance: Number(place.distance) || 0,
        distanceText: formatDistance(place.distance),
        placeUrl: place.place_url || "#",
        lat: Number(place.y),
        lng: Number(place.x),
      };

      const dedupeKey = `${normalized.brand}:${normalized.name}:${normalized.address}`;
      const existing = uniquePlaces.get(dedupeKey);

      if (!existing || normalized.distance < existing.distance) {
        uniquePlaces.set(dedupeKey, normalized);
      }
    });

    return Array.from(uniquePlaces.values()).sort((a, b) => a.distance - b.distance);
  }

  function searchNearbyTheaters(position) {
    clearSearchResults();
    setLoading(true, "주변 영화관을 찾는 중입니다...");

    state.placesService.keywordSearch("영화관", (data, status) => {
      setLoading(false);

      if (status !== kakao.maps.services.Status.OK) {
        setEmptyState(true, "주변 영화관을 찾지 못했습니다. 잠시 후 다시 시도해 주세요.");
        setLocationNote("위치 정보나 카카오 장소 검색 결과가 일시적으로 비어 있을 수 있어요. 버튼을 다시 눌러 새로고침해 보세요.");
        return;
      }

      state.allPlaces = normalizePlaces(data);

      if (state.allPlaces.length === 0) {
        setEmptyState(true, "반경 5km 내에 CGV, 롯데시네마, 메가박스가 검색되지 않았습니다.");
        setLocationNote("주변에 표시 가능한 멀티플렉스 영화관이 없는 위치일 수 있어요.");
        return;
      }

      setLocationNote(`검색 결과 ${state.allPlaces.length}개를 지도와 리스트에 동기화했습니다.`);
      renderPlaces();
    }, {
      location: position,
      radius: SEARCH_RADIUS,
      sort: kakao.maps.services.SortBy.DISTANCE,
      size: 15,
    });
  }

  function requestCurrentLocation(options = {}) {
    if (!navigator.geolocation) {
      const fallback = new kakao.maps.LatLng(DEFAULT_POSITION.lat, DEFAULT_POSITION.lng);
      state.currentPosition = fallback;
      state.map.setCenter(fallback);
      drawUserLocation(fallback, 0);
      searchNearbyTheaters(fallback);
      setLocationMessage("이 브라우저에서는 위치 기능을 지원하지 않아 서울 시청 기준으로 보여드리고 있어요.");
      setLocationNote("브라우저 위치 기능을 허용하면 더 정확한 주변 영화관을 확인할 수 있습니다.");
      return;
    }

    elements.relocateButton.disabled = true;
    setLoading(true, options.loadingMessage || "현재 위치를 확인하는 중입니다...");

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const latLng = new kakao.maps.LatLng(position.coords.latitude, position.coords.longitude);
        state.currentPosition = latLng;
        state.map.setCenter(latLng);
        drawUserLocation(latLng, position.coords.accuracy);
        searchNearbyTheaters(latLng);
        elements.relocateButton.disabled = false;
      },
      (error) => {
        const fallback = new kakao.maps.LatLng(DEFAULT_POSITION.lat, DEFAULT_POSITION.lng);
        state.currentPosition = fallback;
        state.map.setCenter(fallback);
        drawUserLocation(fallback, 0);
        searchNearbyTheaters(fallback);
        elements.relocateButton.disabled = false;

        const message = error.code === error.PERMISSION_DENIED
          ? "위치 권한이 없어 서울 시청 기준으로 보여드리고 있어요."
          : "현재 위치를 정확히 가져오지 못해 기본 위치로 보여드리고 있어요.";

        setLocationMessage(message);
        setLocationNote("브라우저 위치 권한을 허용한 뒤 다시 누르면 더 정확한 결과를 볼 수 있습니다.");
      },
      {
        enableHighAccuracy: true,
        timeout: 12000,
        maximumAge: 0,
      },
    );
  }

  function bindEvents() {
    elements.relocateButton.addEventListener("click", () => {
      requestCurrentLocation({ loadingMessage: "현재 위치를 다시 확인하는 중입니다..." });
    });

    elements.brandFilters.addEventListener("click", (event) => {
      const button = event.target.closest(".filter-btn");
      if (!button) return;
      setFilter(button.dataset.brand);
    });
  }

  function initElements() {
    elements.map = document.getElementById("map");
    elements.mapLoading = document.getElementById("mapLoading");
    elements.relocateButton = document.getElementById("relocateButton");
    elements.brandFilters = document.getElementById("brandFilters");
    elements.countNum = document.getElementById("countNum");
    elements.emptyState = document.getElementById("emptyState");
    elements.placesList = document.getElementById("placesList");
    elements.locationAccuracy = document.getElementById("locationAccuracy");
    elements.locationNote = document.getElementById("locationNote");
  }

  function initMapPage() {
    initElements();

    if (!elements.map) return;

    if (!window.kakao || !kakao.maps || !kakao.maps.services) {
      setLoading(true, "카카오맵 SDK를 불러오지 못했습니다.");
      setLocationNote("카카오맵 로딩에 실패했습니다. 새로고침 후 다시 시도해 주세요.");
      return;
    }

    state.map = new kakao.maps.Map(elements.map, {
      center: new kakao.maps.LatLng(DEFAULT_POSITION.lat, DEFAULT_POSITION.lng),
      level: 4,
    });
    state.placesService = new kakao.maps.services.Places();
    state.infoWindow = new kakao.maps.InfoWindow({ zIndex: 30 });

    bindEvents();
    requestCurrentLocation({ loadingMessage: "현재 위치를 확인하는 중입니다..." });
  }

  document.addEventListener("DOMContentLoaded", initMapPage);
})();
