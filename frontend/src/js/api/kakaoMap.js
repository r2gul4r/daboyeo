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

const LOCAL_NEARBY_RADIUS_METERS = 3000;
const SHOWTIME_AUTO_RETRY_DELAY_MS = 3000;
const SHOWTIME_AUTO_RETRY_LIMIT = 3;
const DEFAULT_FALLBACK_LOCATION = { lat: 37.5015, lng: 127.0263 };
const BRAND_SEARCHES = [
  { provider: 'CGV', keyword: 'CGV', namePattern: /^CGV(?:\s|$)/i },
  { provider: 'LOTTE', keyword: '\uB86F\uB370\uC2DC\uB124\uB9C8', namePattern: /^\uB86F\uB370\uC2DC\uB124\uB9C8(?:\s|$)/ },
  { provider: 'MEGA', keyword: '\uBA54\uAC00\uBC15\uC2A4', namePattern: /^\uBA54\uAC00\uBC15\uC2A4(?:\s|$)/ },
];
const EXCLUDED_PLACE_TERMS = [
  '\uB9E4\uC810',
  '\uCE74\uD398',
  '\uC8FC\uCC28\uC7A5',
  '\uAD7F\uC988',
  '\uC2A4\uD1A0\uC5B4',
  '\uD31D\uCF58',
  '\uC2A4\uB0B5',
];

const BRAND_URLS = {
  CGV: 'http://www.cgv.co.kr/reserve/show-times/',
  LOTTE: 'https://www.lottecinema.co.kr/NLCHS/Ticketing',
  MEGA: 'https://www.megabox.co.kr/booking',
  ETC: 'https://map.kakao.com/',
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

let activeNearbyRequestId = 0;
let mapPageInitialized = false;

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
      center: new kakao.maps.LatLng(DEFAULT_FALLBACK_LOCATION.lat, DEFAULT_FALLBACK_LOCATION.lng),
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

    kakao.maps.event.addListener(map, 'idle', () => {
      void loadAllTheatersToCluster();
    });

    void loadAllTheatersToCluster();
  });
}

function getPlacesService() {
  return new kakao.maps.services.Places(map);
}

function searchPlacesByKeyword(keyword, options) {
  return new Promise((resolve) => {
    getPlacesService().keywordSearch(
      keyword,
      (data, status) => {
        if (status === kakao.maps.services.Status.OK && Array.isArray(data)) {
          resolve(data);
          return;
        }
        resolve([]);
      },
      options,
    );
  });
}

function isExactBrandTheater(place, brand) {
  const name = String(place.place_name || '').trim();
  const categoryName = `${place.category_name || ''} ${place.category_group_name || ''}`.trim();
  if (!brand.namePattern.test(name)) {
    return false;
  }
  if (EXCLUDED_PLACE_TERMS.some((term) => name.includes(term))) {
    return false;
  }
  return place.category_group_code === 'CT1' || categoryName.includes('\uC601\uD654\uAD00');
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

function normalizeTheaterPlace(place, provider, originLat, originLng) {
  const lat = Number.parseFloat(place.y);
  const lng = Number.parseFloat(place.x);
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
    return null;
  }
  return {
    provider,
    name: String(place.place_name || '').trim(),
    lat,
    lng,
    address: String(place.road_address_name || place.address_name || '').trim(),
    placeUrl: String(place.place_url || '').trim(),
    distance: Number.isFinite(originLat) && Number.isFinite(originLng)
      ? getDistance(originLat, originLng, lat, lng)
      : undefined,
  };
}

function dedupeTheaters(theaters) {
  const deduped = new Map();
  theaters.forEach((theater) => {
    if (!theater) return;
    const key = `${theater.provider}::${theater.name}::${theater.lat.toFixed(6)}::${theater.lng.toFixed(6)}`;
    if (!deduped.has(key)) {
      deduped.set(key, theater);
    }
  });
  return Array.from(deduped.values());
}

function normalizeProviderLabel(provider) {
  const normalized = String(provider || '').trim().toUpperCase();
  if (normalized === 'LOTTE_CINEMA' || normalized === 'LOTTE') return 'LOTTE';
  if (normalized === 'MEGABOX' || normalized === 'MEGA') return 'MEGA';
  return normalized;
}

function buildShowtimeCountByTheater(results) {
  const counts = new Map();
  (Array.isArray(results) ? results : []).forEach((item) => {
    const provider = normalizeProviderLabel(item.provider || item.provider_code);
    const theaterName = String(item.theater_name || '').trim();
    if (!provider || !theaterName) return;
    const key = `${provider}::${theaterName}`;
    counts.set(key, (counts.get(key) || 0) + 1);
  });
  return counts;
}

function sleep(ms) {
  return new Promise((resolve) => {
    window.setTimeout(resolve, ms);
  });
}

async function fetchLiveShowtimePayload(lat, lng) {
  const apiBaseUrl = getApiBaseUrl();
  const response = await fetch(`${apiBaseUrl}/api/live/nearby?lat=${lat}&lng=${lng}`);
  if (!response.ok) {
    throw new Error('Backend live nearby response error');
  }
  const payload = await response.json();
  return {
    results: Array.isArray(payload.results) ? payload.results : [],
    pendingRefresh: Boolean(payload.search?.pendingRefresh),
    warning: String(payload.search?.warning || '').trim(),
  };
}

async function searchTheatersInCurrentBounds() {
  const results = await Promise.all(
    BRAND_SEARCHES.map(async (brand) => {
      const places = await searchPlacesByKeyword(brand.keyword, {
        useMapBounds: true,
        size: 15,
      });
      return places
        .filter((place) => isExactBrandTheater(place, brand))
        .map((place) => normalizeTheaterPlace(place, brand.provider));
    }),
  );
  return dedupeTheaters(results.flat());
}

async function searchTheatersByLocation(lat, lng, radiusMeters) {
  const results = await Promise.all(
    BRAND_SEARCHES.map(async (brand) => {
      const places = await searchPlacesByKeyword(brand.keyword, {
        location: new kakao.maps.LatLng(lat, lng),
        radius: radiusMeters,
        sort: kakao.maps.services.SortBy.DISTANCE,
        size: 15,
      });
      return places
        .filter((place) => isExactBrandTheater(place, brand))
        .map((place) => normalizeTheaterPlace(place, brand.provider, lat, lng));
    }),
  );
  return dedupeTheaters(results.flat())
    .filter((theater) => theater.distance === undefined || theater.distance <= radiusMeters)
    .sort((left, right) => (left.distance ?? Number.MAX_SAFE_INTEGER) - (right.distance ?? Number.MAX_SAFE_INTEGER))
    .slice(0, 10);
}

async function loadAllTheatersToCluster() {
  try {
    if (!map || !window.kakao?.maps?.services) return;

    const theaters = await searchTheatersInCurrentBounds();
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
        void showNearbyTheatersAt(theater.lat, theater.lng);
      });

      return marker;
    });

    if (clusterer) {
      clusterer.clear();
      clusterer.addMarkers(theaterMarkers);
    }
  } catch (error) {
    console.error('Failed to load Kakao Places theater markers:', error);
  }
}

function updateMapWithServerData(theaters, showtimeResults, userLat, userLng) {
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
        <div class="user-label">\uD604\uC7AC \uC704\uCE58</div>
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
      addressInfo.innerText = `\uD604\uC7AC \uC704\uCE58: ${address}`;
    }
  });

  if (nearbyList) {
    nearbyList.innerHTML = '';
  }

  const showtimeCountByTheater = buildShowtimeCountByTheater(showtimeResults);
  const sortedTheaters = (Array.isArray(theaters) ? [...theaters] : [])
    .filter((theater) => Number.isFinite(theater.lat) && Number.isFinite(theater.lng))
    .sort((a, b) => (a.distance ?? Number.MAX_SAFE_INTEGER) - (b.distance ?? Number.MAX_SAFE_INTEGER));

  if (sortedTheaters.length === 0 && nearbyList) {
    nearbyList.innerHTML = '<div style="padding: 15px; border-radius: 8px; border: 1px solid var(--gray50);">\uC8FC\uBCC0\uC5D0 \uD45C\uC2DC\uD560 \uC601\uD654\uAD00\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.</div>';
  }

  sortedTheaters.forEach((theater) => {
    if (!nearbyList) return;

    const brandColor = LABEL_COLORS[theater.provider] || LABEL_COLORS.ETC;
    const distanceText = theater.distance ? `${(theater.distance / 1000).toFixed(1)}km` : '';
    const theaterKey = `${normalizeProviderLabel(theater.provider)}::${String(theater.name || '').trim()}`;
    const showtimeCount = showtimeCountByTheater.get(theaterKey) || 0;
    const showtimeLabel = showtimeCount > 0
      ? `\uC0C1\uC601 \uC815\uBCF4 ${showtimeCount}\uAC74`
      : '\uC0C1\uC601 \uC815\uBCF4 \uC5C6\uC74C';

    const card = document.createElement('div');
    card.className = 'theater-card';
    card.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom: 8px;">
        <span style="font-weight: bold; color: ${brandColor};">${theater.provider}</span>
        <div class="distance-badge">${distanceText}</div>
      </div>
      <h3>${theater.name}</h3>
      <p style="margin-top: 6px; color: var(--gray100); font-size: 0.9rem;">${theater.address || '\uCE74\uCE74\uC624\uB9F5 \uC601\uD654\uAD00 \uAC80\uC0C9 \uACB0\uACFC'}</p>
      <p style="margin-top: 4px; color: var(--gray100); font-size: 0.9rem;">${showtimeLabel}</p>
      <div style="margin-top: 12px;">
        <a href="${theater.placeUrl || BRAND_URLS[theater.provider] || BRAND_URLS.ETC}" target="_blank" class="booking-link" onclick="event.stopPropagation();">
          \uCE74\uCE74\uC624\uB9F5\uC5D0\uC11C \uBCF4\uAE30<i class="fas fa-external-link-alt" style="font-size: 10px; margin-left: 4px;"></i>
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

function isSupersededNearbyRequest(requestId) {
  return requestId !== activeNearbyRequestId;
}

async function runNearbyExploration(lat, lng) {
  const requestId = ++activeNearbyRequestId;
  const nearbyPromise = findNearbyLocal(lat, lng);
  let showtimePayload;

  try {
    showtimePayload = await fetchLiveShowtimePayload(lat, lng);
  } catch (error) {
    console.error('Failed to fetch live showtime data:', error);
    showtimePayload = { results: [], pendingRefresh: false, warning: '' };
  }

  const nearby = await nearbyPromise;
  if (isSupersededNearbyRequest(requestId)) {
    return;
  }

  updateMapWithServerData(nearby, showtimePayload.results, lat, lng);

  for (let attempt = 0; attempt < SHOWTIME_AUTO_RETRY_LIMIT; attempt += 1) {
    if (!showtimePayload.pendingRefresh || isSupersededNearbyRequest(requestId)) {
      return;
    }

    await sleep(SHOWTIME_AUTO_RETRY_DELAY_MS);
    if (isSupersededNearbyRequest(requestId)) {
      return;
    }

    try {
      showtimePayload = await fetchLiveShowtimePayload(lat, lng);
    } catch (error) {
      console.error('Failed to auto refresh live showtime data:', error);
      return;
    }

    if (isSupersededNearbyRequest(requestId)) {
      return;
    }

    updateMapWithServerData(nearby, showtimePayload.results, lat, lng);
  }
}

async function findNearbyLocal(lat, lng) {
  try {
    return await searchTheatersByLocation(lat, lng, LOCAL_NEARBY_RADIUS_METERS);
  } catch (error) {
    console.error('Kakao Places nearby search failed:', error);
    return [];
  }
}

async function showNearbyTheatersAt(lat, lng) {
  openNearbySection();
  await runNearbyExploration(lat, lng);
}

async function handleGeo() {
  setTriggerBusy(true);

  if (!navigator.geolocation) {
    setTriggerBusy(false);
    alert('\uC774 \uBE0C\uB77C\uC6B0\uC800\uC5D0\uC11C\uB294 \uC704\uCE58 \uC815\uBCF4\uB97C \uC9C0\uC6D0\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.');
    return;
  }

  navigator.geolocation.getCurrentPosition(
    async (position) => {
      const userLat = position.coords.latitude;
      const userLng = position.coords.longitude;
      await runNearbyExploration(userLat, userLng);
      setTriggerBusy(false);
    },
    async () => {
      alert('\uC704\uCE58 \uC815\uBCF4\uB97C \uAC00\uC838\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4. \uAC15\uB0A8 \uC9C0\uC5ED\uC744 \uAE30\uC900\uC73C\uB85C \uC601\uD654\uAD00\uC744 \uD45C\uC2DC\uD569\uB2C8\uB2E4.');
      await runNearbyExploration(DEFAULT_FALLBACK_LOCATION.lat, DEFAULT_FALLBACK_LOCATION.lng);
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
          lat: Number.parseFloat(result[0].y),
          lng: Number.parseFloat(result[0].x),
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
          lat: Number.parseFloat(data[0].y),
          lng: Number.parseFloat(data[0].x),
          label: data[0].place_name,
        });
        return;
      }
      resolve(null);
    });
  });
  if (keywordResult) return keywordResult;

  throw new Error('\uC704\uCE58\uB97C \uCC3E\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.');
}

async function handleRegionSearch() {
  const query = mapRegionInput ? mapRegionInput.value.trim() : '';
  if (!query) return;

  if (mapRegionSearchFeedback) {
    mapRegionSearchFeedback.innerText = '\uAC80\uC0C9 \uC911...';
    mapRegionSearchFeedback.style.color = 'var(--gray100)';
  }

  try {
    const found = await searchLocationByQuery(query);
    if (map) {
      const moveLatLon = new kakao.maps.LatLng(found.lat, found.lng);
      map.setCenter(moveLatLon);
      map.setLevel(4);
    }

    const geocoder = new kakao.maps.services.Geocoder();
    const geocodePromise = new Promise((resolve) => {
      geocoder.coord2Address(found.lng, found.lat, (result, status) => {
        if (status === kakao.maps.services.Status.OK && result[0].address) {
          const addr = result[0].address;
          const fullRegionText = addr.address_name
            || `${addr.region_1depth_name || ''} ${addr.region_2depth_name || ''} ${addr.region_3depth_name || ''}`.trim();

          if (mapRegionInput) {
            mapRegionInput.value = `${addr.region_1depth_name} ${addr.region_2depth_name} ${addr.region_3depth_name}`.trim();
          }

          if (window.updateRegionFromMap) {
            window.updateRegionFromMap({
              full: fullRegionText,
              sido: addr.region_1depth_name,
              sigungu: addr.region_2depth_name,
              dong: addr.region_3depth_name,
            });
          }
          resolve(true);
          return;
        }
        if (window.updateRegionFromMap) {
          window.updateRegionFromMap({
            full: found.label,
            sido: found.label.split(' ')[0],
            sigungu: found.label.split(' ')[1],
            dong: found.label.split(' ')[2],
          });
        }
        resolve(false);
      });
    });

    await geocodePromise;
    await runNearbyExploration(found.lat, found.lng);
    openNearbySection();

    if (mapRegionSearchFeedback) {
      mapRegionSearchFeedback.innerText = `"${found.label}" \uC9C0\uC5ED \uC601\uD654\uAD00 \uD0D0\uC0C9 \uACB0\uACFC\uB97C \uD45C\uC2DC\uD588\uC2B5\uB2C8\uB2E4.`;
      mapRegionSearchFeedback.style.color = 'var(--purple50)';
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

function initializeMapPage() {
  if (mapPageInitialized) {
    return;
  }
  mapPageInitialized = true;
  initializeDaboyeoMap();
  window.openNearbyTheaters = openNearbyTheaters;
  window.handleMapRegionSearch = () => {
    void handleRegionSearch();
  };
}

if (document.readyState === 'complete') {
  initializeMapPage();
} else {
  window.addEventListener('load', initializeMapPage, { once: true });
}
