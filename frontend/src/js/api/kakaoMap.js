import { getApiBaseUrl } from './client.js';

let map;
let clusterer;
let markers = [];

const mapContainer = document.getElementById('map');
const nearbySection = document.getElementById('nearby-section');
const nearbyList = document.getElementById('nearby-list');
const nearbyCount = document.getElementById('nearby-count');
const addressInfo = document.getElementById('user-address-info');
const mapRegionInput = document.getElementById('map-region-search');
const mapRegionSearchBtn = document.getElementById('map-region-search-btn');
const mapRegionSearchFeedback = document.getElementById('map-region-search-feedback');
const locationBtn = document.getElementById('locationBtn');
const closeNearbyBtn = document.getElementById('close-nearby');
const goToMyLocationBtn = document.getElementById('go-to-my-location');

const BRAND_URLS = {
  CGV: 'http://www.cgv.co.kr/reserve/show-times/',
  LOTTE: 'https://www.lottecinema.co.kr/NLCHS/Ticketing',
  MEGA: 'https://www.megabox.co.kr/booking',
  ETC: 'https://www.google.com/search?q=영화예매',
};

const MARKER_COLORS = {
  CGV: '#E71A0F',
  LOTTE: '#FF8C00',
  MEGA: '#361771',
  ETC: '#555555',
};

const LABEL_COLORS = {
  CGV: '#E71A0F',
  LOTTE: '#FF8C00',
  MEGA: '#B197FC',
  ETC: '#9E9E9E',
};

function setTriggerBusy(isBusy) {
  if (locationBtn) {
    locationBtn.style.opacity = isBusy ? '0.5' : '1';
  }
}

function openNearbySection() {
  if (!nearbySection) return;

  nearbySection.classList.add('active');
  setTimeout(() => {
    nearbySection.scrollIntoView({ behavior: 'smooth', block: 'start' });
    if (map) {
      map.relayout();
    }
  }, 150);
}

function openNearbyTheaters() {
  openNearbySection();
  void handleGeo();
}

function createMarkerImage(color) {
  const svg = `<svg width="32" height="42" viewBox="0 0 32 42" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M16 0C7.16 0 0 7.16 0 16C0 25 16 42 16 42C16 42 32 25 32 16C32 7.16 24.84 0 16 0Z" fill="${color}" stroke="white" stroke-width="1.5"/><circle cx="16" cy="16" r="6" fill="white"/></svg>`;
  const encodedSvg = encodeURIComponent(svg.trim());
  return new kakao.maps.MarkerImage(
    `data:image/svg+xml;charset=utf-8,${encodedSvg}`,
    new kakao.maps.Size(28, 36),
    { offset: new kakao.maps.Point(14, 36) },
  );
}

function initializeDaboyeoMap() {
  if (!mapContainer || !window.kakao || !kakao.maps) return;

  kakao.maps.load(() => {
    map = new kakao.maps.Map(mapContainer, {
      center: new kakao.maps.LatLng(37.5015, 127.0263),
      level: 8,
    });

    clusterer = new kakao.maps.MarkerClusterer({
      map,
      averageCenter: true,
      minLevel: 6,
      styles: [{
        width: '53px',
        height: '52px',
        background: 'rgba(114, 100, 233, 0.9)',
        borderRadius: '50%',
        color: '#fff',
        textAlign: 'center',
        fontWeight: 'bold',
        lineHeight: '54px',
        border: '2px solid #fff',
        boxShadow: '0 0 10px rgba(114, 100, 233, 0.5)',
      }],
    });

    void loadAllTheatersToCluster();
  });
}

async function loadAllTheatersToCluster() {
  try {
    const response = await fetch('./src/map/theaters.json');
    if (!response.ok) return;

    const theaters = await response.json();
    const hoverOverlay = new kakao.maps.CustomOverlay({ yAnchor: 2.5, zIndex: 3 });
    const theaterMarkers = theaters.map((theater) => {
      const markerColor = MARKER_COLORS[theater.provider] || MARKER_COLORS.ETC;
      const labelColor = LABEL_COLORS[theater.provider] || LABEL_COLORS.ETC;
      const marker = new kakao.maps.Marker({
        position: new kakao.maps.LatLng(theater.lat, theater.lng),
        image: createMarkerImage(markerColor),
        title: theater.name,
      });

      kakao.maps.event.addListener(marker, 'mouseover', () => {
        hoverOverlay.setContent(`
          <div style="
            background: rgba(15, 15, 15, 0.95);
            color: #ffffff;
            padding: 6px 14px;
            border-radius: 20px;
            border: 1.5px solid ${labelColor};
            font-size: 12px;
            font-weight: 800;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
            white-space: nowrap;
            pointer-events: none;
          ">
            ${theater.name}
          </div>
        `);
        hoverOverlay.setPosition(marker.getPosition());
        hoverOverlay.setMap(map);
      });

      kakao.maps.event.addListener(marker, 'mouseout', () => {
        hoverOverlay.setMap(null);
      });

      kakao.maps.event.addListener(marker, 'click', () => {
        map.panTo(marker.getPosition());
        map.setLevel(4);
        void fetchLiveNearby(theater.lat, theater.lng);
      });

      return marker;
    });

    if (clusterer) {
      clusterer.clear();
      clusterer.addMarkers(theaterMarkers);
    }
  } catch (error) {
    console.error('Failed to load static theater database:', error);
  }
}

async function fetchLiveNearby(lat, lng) {
  openNearbySection();

  try {
    const apiBaseUrl = getApiBaseUrl();
    const response = await fetch(`${apiBaseUrl}/api/live/nearby?lat=${lat}&lng=${lng}`);
    if (!response.ok) {
      throw new Error('Backend API response error');
    }

    const data = await response.json();
    updateMapWithServerData(data.theaters || [], data.results || [], lat, lng);
  } catch (error) {
    console.error('Failed to fetch live data from server:', error);
    updateMapWithServerData([], [], lat, lng);
    if (nearbyList) {
      nearbyList.innerHTML = '<div style="padding: 15px; border-radius: 8px; border: 1px solid var(--gray50); color: var(--red50);">현재 실시간 정보를 가져올 수 없습니다.</div>';
    }
  }
}

function updateMapWithServerData(theaters, results, userLat, userLng) {
  if (!window.kakao || !kakao.maps || !map) return;

  map.relayout();
  const userPos = new kakao.maps.LatLng(userLat, userLng);
  map.setCenter(userPos);
  map.setLevel(4);

  if (markers.length > 0) {
    markers.forEach((marker) => marker.setMap(null));
  }
  markers = [];

  const userMarker = new kakao.maps.CustomOverlay({
    position: userPos,
    content: `
      <div class="user-location-marker">
        <div class="user-pulse"></div>
        <div class="user-dot"></div>
        <div class="user-label">현재 위치</div>
      </div>
    `,
    yAnchor: 1,
  });
  userMarker.setMap(map);
  markers.push(userMarker);

  const geocoder = new kakao.maps.services.Geocoder();
  geocoder.coord2Address(userLng, userLat, (result, status) => {
    if (status === kakao.maps.services.Status.OK && addressInfo) {
      const address = result[0].road_address ? result[0].road_address.address_name : result[0].address.address_name;
      addressInfo.innerText = `현재 위치: ${address}`;
    }
  });

  const showtimeCountByTheater = {};
  results.forEach((item) => {
    const key = `${item.provider}_${item.theater_name}`;
    showtimeCountByTheater[key] = (showtimeCountByTheater[key] || 0) + 1;
  });

  if (nearbyList) {
    nearbyList.innerHTML = '';
  }

  const sortedTheaters = (Array.isArray(theaters) ? [...theaters] : [])
    .filter((theater) => Number.isFinite(theater.lat) && Number.isFinite(theater.lng))
    .sort((a, b) => (a.distance ?? Number.MAX_SAFE_INTEGER) - (b.distance ?? Number.MAX_SAFE_INTEGER));

  if (sortedTheaters.length === 0 && nearbyList) {
    nearbyList.innerHTML = '<div style="padding: 15px; border-radius: 8px; border: 1px solid var(--gray50);">주변에 상영 정보를 제공하는 극장이 없습니다.</div>';
  }

  sortedTheaters.forEach((theater) => {
    if (!nearbyList) return;

    const theaterKey = `${theater.provider}_${theater.name}`;
    const brandColor = LABEL_COLORS[theater.provider] || LABEL_COLORS.ETC;
    const distanceText = theater.distance ? `${(theater.distance / 1000).toFixed(1)}km` : '';
    const showtimeCount = showtimeCountByTheater[theaterKey] || 0;

    const card = document.createElement('div');
    card.className = 'theater-card';
    card.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom: 8px;">
        <span style="font-weight: bold; color: ${brandColor};">${theater.provider}</span>
        <div class="distance-badge">${distanceText}</div>
      </div>
      <h3>${theater.name}</h3>
      <p style="margin-top: 6px; color: var(--gray100); font-size: 0.9rem;">실시간 상영 ${showtimeCount}건</p>
      <div style="margin-top: 12px;">
        <a href="${BRAND_URLS[theater.provider] || BRAND_URLS.ETC}" target="_blank" class="booking-link" onclick="event.stopPropagation();">
          예매 바로가기 <i class="fas fa-external-link-alt" style="font-size: 10px; margin-left: 4px;"></i>
        </a>
      </div>
    `;

    card.onclick = () => {
      map.panTo(new kakao.maps.LatLng(theater.lat, theater.lng));
      map.setLevel(3);
    };

    nearbyList.appendChild(card);
  });

  if (nearbyCount) {
    nearbyCount.innerText = String(sortedTheaters.length);
  }
}

function getDistance(lat1, lon1, lat2, lon2) {
  const radiusKm = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
    + Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180)
    * Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return radiusKm * c * 1000;
}

async function findNearbyLocal(lat, lng) {
  try {
    const response = await fetch('./src/map/theaters.json');
    if (!response.ok) return [];

    const theaters = await response.json();
    return theaters
      .map((theater) => ({
        ...theater,
        distance: getDistance(lat, lng, theater.lat, theater.lng),
      }))
      .filter((theater) => theater.distance <= 10000)
      .sort((a, b) => a.distance - b.distance)
      .slice(0, 10);
  } catch (error) {
    console.error('Local search failed:', error);
    return [];
  }
}

async function handleGeo() {
  setTriggerBusy(true);

  if (!navigator.geolocation) {
    setTriggerBusy(false);
    alert('이 브라우저에서는 위치 정보를 지원하지 않습니다.');
    return;
  }

  navigator.geolocation.getCurrentPosition(
    async (position) => {
      const userLat = position.coords.latitude;
      const userLng = position.coords.longitude;
      const nearby = await findNearbyLocal(userLat, userLng);
      updateMapWithServerData(nearby, [], userLat, userLng);
      setTriggerBusy(false);
    },
    async () => {
      alert('위치 정보를 가져오지 못했습니다. 강남 지역을 기준으로 검색합니다.');
      await fetchLiveNearby(37.5015, 127.0263);
      setTriggerBusy(false);
    },
    {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 0,
    },
  );
}

async function searchLocationByQuery(query) {
  if (!window.kakao || !kakao.maps.services) {
    throw new Error('SDK not ready');
  }

  const geocoder = new kakao.maps.services.Geocoder();
  const addressResult = await new Promise((resolve) => {
    geocoder.addressSearch(query, (result, status) => {
      if (status === kakao.maps.services.Status.OK && result.length > 0) {
        resolve({
          lat: parseFloat(result[0].y),
          lng: parseFloat(result[0].x),
          label: result[0].address_name,
        });
        return;
      }
      resolve(null);
    });
  });
  if (addressResult) return addressResult;

  const places = new kakao.maps.services.Places();
  const keywordResult = await new Promise((resolve) => {
    places.keywordSearch(query, (data, status) => {
      if (status === kakao.maps.services.Status.OK && data.length > 0) {
        resolve({
          lat: parseFloat(data[0].y),
          lng: parseFloat(data[0].x),
          label: data[0].place_name,
        });
        return;
      }
      resolve(null);
    });
  });
  if (keywordResult) return keywordResult;

  throw new Error('위치를 찾을 수 없습니다.');
}

async function handleRegionSearch() {
  const query = mapRegionInput ? mapRegionInput.value.trim() : '';
  if (!query) return;

  if (mapRegionSearchFeedback) {
    mapRegionSearchFeedback.innerText = '검색 중...';
    mapRegionSearchFeedback.style.color = 'var(--gray100)';
  }

  try {
    const found = await searchLocationByQuery(query);
    if (mapRegionSearchFeedback) {
      mapRegionSearchFeedback.innerText = `"${found.label}" 주변 극장을 조회합니다.`;
    }

    const regionInput = document.getElementById('regionInput');
    if (regionInput) {
      regionInput.value = found.label;
    }

    if (nearbySection) {
      nearbySection.classList.remove('active');
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  } catch (error) {
    if (mapRegionSearchFeedback) {
      mapRegionSearchFeedback.innerText = error.message;
      mapRegionSearchFeedback.style.color = 'var(--red50)';
    }
  }
}

if (locationBtn) {
  locationBtn.addEventListener('click', (event) => {
    event.preventDefault();
    openNearbyTheaters();
  });
}

if (mapRegionSearchBtn) {
  mapRegionSearchBtn.addEventListener('click', handleRegionSearch);
}

if (mapRegionInput) {
  mapRegionInput.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
      void handleRegionSearch();
    }
  });
}

if (closeNearbyBtn) {
  closeNearbyBtn.addEventListener('click', () => {
    if (nearbySection) {
      nearbySection.classList.remove('active');
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  });
}

if (goToMyLocationBtn) {
  goToMyLocationBtn.addEventListener('click', () => {
    openNearbyTheaters();
  });
}

window.addEventListener('load', () => {
  initializeDaboyeoMap();
  window.openNearbyTheaters = openNearbyTheaters;
});
