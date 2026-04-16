import { getApiBaseUrl } from './client.js';

let map;
let clusterer;
let markers = [];
let allTheaterMarkers = [];

// DOM Elements
const mapContainer = document.getElementById("map");
const nearbySection = document.getElementById('nearby-section');
const nearbyList = document.getElementById('nearby-list');
const nearbyCount = document.getElementById('nearby-count');
const addressInfo = document.getElementById('user-address-info');
const mapRegionInput = document.getElementById('map-region-search');
const mapRegionSearchBtn = document.getElementById('map-region-search-btn');
const mapRegionSearchFeedback = document.getElementById('map-region-search-feedback');
const nearbyBtnBtn = document.getElementById('nearbyBtn');
const closeNearbyBtn = document.getElementById('close-nearby');
const goToMyLocationBtn = document.getElementById('go-to-my-location');

// 영화사별 공식 홈페이지 링크
const BRAND_URLS = {
    'CGV': 'http://www.cgv.co.kr/reserve/show-times/',
    'LOTTE': 'https://www.lottecinema.co.kr/NLCHS/Ticketing',
    'MEGA': 'https://www.megabox.co.kr/booking',
    'ETC': 'https://www.google.com/search?q=영화예매'
};

function initializeDaboyeoMap() {
    if (!mapContainer || !window.kakao || !kakao.maps) return;
    
    kakao.maps.load(() => {
        const options = {
            center: new kakao.maps.LatLng(37.5015, 127.0263),
            level: 8 // Overview level
        };
        map = new kakao.maps.Map(mapContainer, options);

        // 클러스터러 초기화
        clusterer = new kakao.maps.MarkerClusterer({
            map: map,
            averageCenter: true,
            minLevel: 6, // 6레벨 이상에서만 클러스터링 (숫자로 표시)
            styles: [{
                width: '53px', height: '52px',
                background: 'rgba(114, 100, 233, 0.9)',
                borderRadius: '50%',
                color: '#fff',
                textAlign: 'center',
                fontWeight: 'bold',
                lineHeight: '54px',
                border: '2px solid #fff',
                boxShadow: '0 0 10px rgba(114, 100, 233, 0.5)'
            }]
        });

        console.log("Map initialized and loading static theater database.");
        loadAllTheatersToCluster();
    });
}

// 브랜드별 마커 색상 정의
// 지도의 핀 마커용 (브랜드 정체성 강조)
const MARKER_COLORS = {
    'CGV': '#E71A0F',     // CGV 레드
    'LOTTE': '#FF8C00',   // 롯데시네마 오렌지
    'MEGA': '#361771',    // 메가박스 진보라 (기존 색상 복구)
    'ETC': '#555555'      // 기타 회색
};

// 카드 리스트 및 툴팁 글자용 (가독성 강조)
const LABEL_COLORS = {
    'CGV': '#E71A0F',
    'LOTTE': '#FF8C00',
    'MEGA': '#B197FC',    // 글자는 잘 보이도록 연보라
    'ETC': '#9E9E9E'      // 회색 가독성 보정
};

// 핀 모양의 SVG 마커 생성 함수 (더 날렵해진 디자인)
function createMarkerImage(color) {
    const svg = `<svg width="32" height="42" viewBox="0 0 32 42" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M16 0C7.16 0 0 7.16 0 16C0 25 16 42 16 42C16 42 32 25 32 16C32 7.16 24.84 0 16 0Z" fill="${color}" stroke="white" stroke-width="1.5"/><circle cx="16" cy="16" r="6" fill="white"/></svg>`;
    const encodedSvg = encodeURIComponent(svg.trim());
    return new kakao.maps.MarkerImage(
        `data:image/svg+xml;charset=utf-8,${encodedSvg}`,
        new kakao.maps.Size(28, 36),
        { offset: new kakao.maps.Point(14, 36) }
    );
}

async function loadAllTheatersToCluster() {
    try {
        console.log("Start loading nationwide theater data...");
        const response = await fetch('./src/map/theaters.json');
        
        if (!response.ok) {
            console.error("Failed to fetch theaters.json. Status:", response.status);
            return;
        }
        
        const theaters = await response.json();
        console.log(`Successfully loaded ${theaters.length} theaters.`);

        // 극장 이름 표시용 커스텀 오버레이 (말풍선 꼬리 없는 깔끔한 스타일)
        const hoverOverlay = new kakao.maps.CustomOverlay({
            yAnchor: 2.5,
            zIndex: 3
        });

        allTheaterMarkers = theaters.map(t => {
            const markerColor = MARKER_COLORS[t.provider] || MARKER_COLORS['ETC'];
            const labelColor = LABEL_COLORS[t.provider] || LABEL_COLORS['ETC'];
            
            const marker = new kakao.maps.Marker({
                position: new kakao.maps.LatLng(t.lat, t.lng),
                image: createMarkerImage(markerColor),
                title: t.name
            });
            
            // 마우스 호버 시 커스텀 툴팁 표시
            kakao.maps.event.addListener(marker, 'mouseover', () => {
                const content = `
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
                        ${t.name}
                    </div>
                `;
                hoverOverlay.setContent(content);
                hoverOverlay.setPosition(marker.getPosition());
                hoverOverlay.setMap(map);
            });

            // 마우스 이탈 시 툴팁 제거
            kakao.maps.event.addListener(marker, 'mouseout', () => {
                hoverOverlay.setMap(null);
            });

            // 마커 클릭 시 해당 위치로 이동 및 실시간 상영 정보 조회
            kakao.maps.event.addListener(marker, 'click', () => {
                map.panTo(marker.getPosition());
                map.setLevel(4);
                fetchLiveNearby(t.lat, t.lng);
            });

            return marker;
        });

        if (clusterer) {
            clusterer.clear();
            clusterer.addMarkers(allTheaterMarkers);
            console.log("Clusterer initialized with markers.");
        }
    } catch (e) {
        console.error("Fatal error loading static theaters.json:", e);
    }
}

async function fetchLiveNearby(lat, lng) {
    if (nearbySection) nearbySection.classList.add('active');
    
    // 이제 위치 검색 시 DB에 새로 저장하거나 등록할 필요가 없으므로 
    // 백엔드에는 오직 실시간 상영 정보 조회를 위한 요청만 보냅니다.
    try {
        const API_BASE_URL = getApiBaseUrl();
        const response = await fetch(`${API_BASE_URL}/api/live/nearby?lat=${lat}&lng=${lng}`);
        if (!response.ok) throw new Error("Backend API Response Error");
        
        const data = await response.json();
        updateMapWithServerData(data.theaters || [], data.results, lat, lng);
        return true;
    } catch (error) {
        console.error("Failed to fetch live data from server:", error);
        updateMapWithServerData([], [], lat, lng);
        if (nearbyList) nearbyList.innerHTML = '<div style="padding: 15px; border-radius: 8px; border: 1px solid var(--gray50); color: var(--red50);">현재 실시간 정보를 가져올 수 없습니다.</div>';
        return false;
    }
}

function updateMapWithServerData(theaters, results, userLat, userLng) {
    if (!window.kakao || !kakao.maps || !map) return;
    
    map.relayout();
    const userPos = new kakao.maps.LatLng(userLat, userLng);
    map.setCenter(userPos);
    map.setLevel(4); // 검색 시 상세 레벨로 확대
    
    // 이전 사용자 마커 제거
    if (markers && markers.length > 0) {
        markers.forEach(m => m.setMap(null));
    }
    markers = [];

    // User marker (현재 검색 중심점 - 전용 커스텀 스타일 적용)
    const userMarkerContent = `
        <div class="user-location-marker">
            <div class="user-pulse"></div>
            <div class="user-dot"></div>
            <div class="user-label">현재 위치</div>
        </div>
    `;

    const userMarker = new kakao.maps.CustomOverlay({
        position: userPos,
        content: userMarkerContent,
        yAnchor: 1
    });
    userMarker.setMap(map);
    markers.push(userMarker);

    // 주소 변환
    const geocoder = new kakao.maps.services.Geocoder();
    geocoder.coord2Address(userLng, userLat, (result, status) => {
        if (status === kakao.maps.services.Status.OK && addressInfo) {
            const add = result[0].road_address ? result[0].road_address.address_name : result[0].address.address_name;
            addressInfo.innerText = `📍 현재 위치: ${add}`;
        }
    });

    const showtimeCountByTheater = {};
    if(results) {
        results.forEach(item => {
            const tKey = `${item.provider}_${item.theater_name}`;
            showtimeCountByTheater[tKey] = (showtimeCountByTheater[tKey] || 0) + 1;
        });
    }

    if(nearbyList) nearbyList.innerHTML = '';
    const sortedTheaters = (Array.isArray(theaters) ? [...theaters] : [])
        .filter(t => Number.isFinite(t.lat) && Number.isFinite(t.lng))
        .sort((a, b) => a.distance - b.distance);
    
    if (sortedTheaters.length === 0 && nearbyList) {
        nearbyList.innerHTML = '<div style="padding: 15px; border-radius: 8px; border: 1px solid var(--gray50);">주변에 상영 정보를 제공하는 극장이 없습니다.</div>';
    }

    sortedTheaters.forEach((t) => {
        const theaterKey = `${t.provider}_${t.name}`;
        const showtimeCount = showtimeCountByTheater[theaterKey] || 0;
        const brandColor = LABEL_COLORS[t.provider] || LABEL_COLORS['ETC'];

        if(nearbyList) {
            const card = document.createElement('div');
            card.className = "theater-card";
            
            let distText = t.distance ? `${(t.distance/1000).toFixed(1)}km` : "";
            
            card.innerHTML = `
                <div style="display:flex; justify-content:space-between; align-items:flex-start; margin-bottom: 8px;">
                    <span style="font-weight: bold; color: ${brandColor};">${t.provider}</span>
                    <div class="distance-badge">${distText}</div>
                </div>
                <h3>${t.name}</h3>
                <div style="margin-top: 12px;">
                    <a href="${BRAND_URLS[t.provider] || BRAND_URLS['ETC']}" target="_blank" class="booking-link" onclick="event.stopPropagation();">
                        예매 바로가기 <i class="fas fa-external-link-alt" style="font-size: 10px; margin-left: 4px;"></i>
                    </a>
                </div>
            `;
            
            card.onclick = () => { 
                map.panTo(new kakao.maps.LatLng(t.lat, t.lng)); 
                map.setLevel(3);
            };
            nearbyList.appendChild(card);
        }
    });

    if(nearbyCount) nearbyCount.innerText = sortedTheaters.length;
}

// 하버사인 공식 (두 좌표 사이의 거리 계산)
function getDistance(lat1, lon1, lat2, lon2) {
    const R = 6371; // 지구 반지름 (km)
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c * 1000; // 미터 단위 반환
}

async function findNearbyLocal(lat, lng) {
    try {
        const response = await fetch('./src/map/theaters.json');
        const theaters = await response.json();
        
        // 거리 계산 및 추가
        const withDistance = theaters.map(t => ({
            ...t,
            distance: getDistance(lat, lng, t.lat, t.lng)
        }));
        
        // 5km 이내 극장 중 가까운 순으로 최대 10개 추출
        return withDistance
            .filter(t => t.distance <= 10000) // 10km 이내로 범위 확장
            .sort((a, b) => a.distance - b.distance)
            .slice(0, 10);
    } catch (e) {
        console.error("Local search failed:", e);
        return [];
    }
}

async function handleGeo() {
    if (nearbyBtnBtn) nearbyBtnBtn.style.opacity = '0.5';
    
    if (!navigator.geolocation) {
        alert("이 브라우저에서는 위치 정보를 지원하지 않습니다.");
        return;
    }

    navigator.geolocation.getCurrentPosition(
        async (position) => {
            const userLat = position.coords.latitude;
            const userLng = position.coords.longitude;
            
            // 로컬 데이터 기반 주변 극장 찾기
            const nearby = await findNearbyLocal(userLat, userLng);
            
            if (nearbySection) nearbySection.classList.add('active');
            updateMapWithServerData(nearby, [], userLat, userLng);

            if (nearbyBtnBtn) nearbyBtnBtn.style.opacity = '1';
        },
        (error) => {
            console.error("Error getting location:", error);
            alert("위치 정보를 가져오는 데 실패했습니다. 강남 지역을 기준으로 검색합니다.");
            fetchLiveNearby(37.5015, 127.0263);
            if (nearbyBtnBtn) nearbyBtnBtn.style.opacity = '1';
        },
        { 
            enableHighAccuracy: true,  // GPS 기술을 활용한 고정밀 위치 파악 시도
            timeout: 10000,            // GPS 신호 대기 시간 연장 (10초)
            maximumAge: 0              // 이전 위치 정보를 재사용하지 않고 실시간으로 조회
        }
    );
}

async function searchLocationByQuery(query) {
    if (!window.kakao || !kakao.maps.services) throw new Error("SDK Not Ready");

    const geocoder = new kakao.maps.services.Geocoder();
    const addressResult = await new Promise((resolve) => {
        geocoder.addressSearch(query, (result, status) => {
            if (status === kakao.maps.services.Status.OK && result.length > 0) {
                resolve({ lat: parseFloat(result[0].y), lng: parseFloat(result[0].x), label: result[0].address_name });
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
                resolve({ lat: parseFloat(data[0].y), lng: parseFloat(data[0].x), label: data[0].place_name });
                return;
            }
            resolve(null);
        });
    });

    if (keywordResult) return keywordResult;
    throw new Error("위치를 찾을 수 없습니다.");
}

async function handleRegionSearch() {
    const query = mapRegionInput ? mapRegionInput.value.trim() : "";
    if (!query) return;

    if (mapRegionSearchFeedback) mapRegionSearchFeedback.innerText = "검색 중...";
    try {
        const found = await searchLocationByQuery(query);
        if (mapRegionSearchFeedback) mapRegionSearchFeedback.innerText = `"${found.label}" 주변 극장을 조회합니다.`;
        
        // 지도 섹션 활성화 및 스크롤
        if (nearbySection) {
            nearbySection.classList.add('active');
            nearbySection.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }

        // 로컬 데이터 기반 주변 극장 조회
        const nearby = await findNearbyLocal(found.lat, found.lng);
        updateMapWithServerData(nearby, [], found.lat, found.lng);

        // 지도가 보이도록 relayout
        if (map) {
            setTimeout(() => map.relayout(), 300);
        }
    } catch (error) {
        if (mapRegionSearchFeedback) mapRegionSearchFeedback.innerText = error.message;
        if (mapRegionSearchFeedback) mapRegionSearchFeedback.style.color = 'var(--red50)';
    }
}

// Events
if (nearbyBtnBtn) {
    nearbyBtnBtn.addEventListener('click', (e) => {
        e.preventDefault();
        
        // 지도 섹션 활성화 (슬라이드 애니메이션)
        if (nearbySection) {
            nearbySection.classList.add('active');
            setTimeout(() => {
                nearbySection.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }, 150);
        }
        
        // 실제 실시간 GPS 위치 추적 실행
        handleGeo(); 
    });
}
if (mapRegionSearchBtn) {
    mapRegionSearchBtn.addEventListener('click', handleRegionSearch);
}
if (mapRegionInput) {
    mapRegionInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') handleRegionSearch();
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
        // 데모 모드일 수 있으므로 handleGeo 호출 전 알림
        console.log("Moving to current GPS location...");
        handleGeo();
    });
}

// Init map
window.addEventListener('load', () => {
    try {
        initializeDaboyeoMap();
    } catch (e) {
        console.error("Initialization failed on window load:", e);
    }
});
