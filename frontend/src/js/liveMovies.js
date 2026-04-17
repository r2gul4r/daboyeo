/**
 * liveMovies.js - URL 파라미터 연동 및 가상 데이터 처리 버전
 */

const movieGrid = document.getElementById('movie-grid');
const API_BASE_URL = 'http://localhost:8000/api';

// --- 가상 데이터 및 기본값 설정 (Virtual Data Engine) ---
const MOCK_DEFAULTS = {
    lat: 37.4979,              // 강남역 위도
    lng: 127.0276,             // 강남역 경도
    regionName: "서울 강남구",  // 표시용 지역명
    date: new Date().toISOString().split('T')[0], // 오늘 날짜 (YYYY-MM-DD)
    timeStart: "06:00",
    timeEnd: "23:59"
};

// 현재 활성화된 검색 조건 (URL에서 읽거나 기본값 사용)
let currentSearchConfig = { ...MOCK_DEFAULTS };

let allRawSchedules = []; 
let currentSelectedMovie = null;
let currentTab = 'ALL';

// 1. URL에서 파라미터 읽어오기
function initSearchParams() {
    const params = new URLSearchParams(window.location.search);
    
    // 파라미터가 있으면 덮어쓰기, 없으면 기본값 유지
    if (params.has('lat')) currentSearchConfig.lat = parseFloat(params.get('lat'));
    if (params.has('lng')) currentSearchConfig.lng = parseFloat(params.get('lng'));
    if (params.has('region')) currentSearchConfig.regionName = params.get('region');
    if (params.has('date')) currentSearchConfig.date = params.get('date');
    if (params.has('timeStart')) currentSearchConfig.timeStart = params.get('timeStart');
    if (params.has('timeEnd')) currentSearchConfig.timeEnd = params.get('timeEnd');

    console.log("Current Search Config:", currentSearchConfig);
    updateSearchInfoUI();
}

// 2. 상단에 어떤 검색 결과인지 보여주는 UI 업데이트
function updateSearchInfoUI() {
    // movies.html에 관련 ID 요소가 있다면 텍스트 변경
    const infoArea = document.getElementById('search-info-text');
    if (infoArea) {
        infoArea.innerHTML = `
            <strong>${currentSearchConfig.regionName}</strong> 주변, 
            <strong>${currentSearchConfig.date}</strong> 기준 
            (시간: ${currentSearchConfig.timeStart} ~ ${currentSearchConfig.timeEnd})
        `;
    }
}

async function loadLiveMovies() {
    if (!movieGrid) return;
    
    movieGrid.innerHTML = `
        <div class="loading-spinner">
            <i class="fas fa-circle-notch fa-spin"></i>
            <p>실시간 정보를 불러오고 있습니다...</p>
        </div>
    `;

    try {
        // 백엔드 API 호출 시 현재 설정된 좌표 전송
        const { lat, lng } = currentSearchConfig;
        const response = await fetch(`${API_BASE_URL}/live/nearby?lat=${lat}&lng=${lng}`);
        const data = await response.json();
        
        if (!data || !data.results) {
            movieGrid.innerHTML = '<p class="loading-spinner">데이터 수집에 실패했습니다.</p>';
            return;
        }

        allRawSchedules = data.results.map(item => ({
            ...item,
            normalized: {
                title: item.movie_name || item.movie_nm || "제목 없음",
                provider: (item.provider === 'LOTTE_CINEMA' ? 'LOTTE' : item.provider) || "ETC",
                format: item.format_name || item.screen_name || item.screen_division_name || "2D",
                theater: item.theater_name || item.cinema_name || "불명",
                rating: item.age_rating || item.age_rating_name || "ALL",
                total_seats: item.total_seat_count || 100,
                available_seats: item.available_seat_count || item.remaining_seat_count || 0,
                start_time: item.start_time || "00:00"
            }
        }));

        applyFilters();

    } catch (error) {
        console.error("API Error:", error);
        movieGrid.innerHTML = '<p class="loading-spinner">백엔드 서버 연결 오류</p>';
    }
}

function applyFilters() {
    const getActiveValues = (id) => {
        const container = document.getElementById(id);
        if (!container) return ['all'];
        const chips = container.querySelectorAll('.filter-chip.active');
        return Array.from(chips).map(c => c.getAttribute('data-value'));
    };

    const providerFilters = getActiveValues('check-provider');
    const theaterFilters = getActiveValues('check-theater');
    const seatTypeFilters = getActiveValues('check-seat-type');
    const seatFilters = getActiveValues('check-seats');
    const searchFilterValue = document.getElementById('filter-search') ? document.getElementById('filter-search').value.trim().toLowerCase() : '';

    const filtered = allRawSchedules.filter(item => {
        const norm = item.normalized;
        const matchProvider = providerFilters.includes('all') || providerFilters.includes(norm.provider);
        const matchTheater = theaterFilters.includes('all') || theaterFilters.some(f => norm.format.toUpperCase().includes(f.toUpperCase()));
        const matchSeatType = seatTypeFilters.includes('all') || seatTypeFilters.some(f => norm.format.toUpperCase().includes(f.toUpperCase()));
        const matchSearch = searchFilterValue === '' || norm.title.toLowerCase().includes(searchFilterValue);
        
        // 3. 시간대 필터링 추가 (현재 설정된 시간대 내부인지 확인)
        const itemTime = norm.start_time; // "HH:MM"
        const matchTime = itemTime >= currentSearchConfig.timeStart && itemTime <= currentSearchConfig.timeEnd;

        // 4. 좌석 비율 필터링
        const seatRatio = (norm.available_seats / norm.total_seats) * 100;
        let matchSeats = seatFilters.includes('all');
        if (seatFilters.includes('spacious')) matchSeats = matchSeats || seatRatio >= 50;
        if (seatFilters.includes('comfortable')) matchSeats = matchSeats || seatRatio >= 30;
        if (seatFilters.includes('closing')) matchSeats = matchSeats || (seatRatio < 10 && norm.available_seats > 0);
        
        return matchProvider && matchTheater && matchSeatType && matchSeats && matchSearch && matchTime;
    });

    const aggregated = filtered.reduce((acc, current) => {
        const title = current.normalized.title;
        if (!acc[title]) {
            acc[title] = {
                title: title,
                providers: new Set([current.normalized.provider]),
                rating: current.normalized.rating,
                max_available: current.normalized.available_seats,
                total_matches: 1
            };
        } else {
            acc[title].providers.add(current.normalized.provider);
            acc[title].max_available = Math.max(acc[title].max_available, current.normalized.available_seats);
            acc[title].total_matches++;
        }
        return acc;
    }, {});

    renderMovieCards(Object.values(aggregated));
}

function renderMovieCards(movies) {
    if (!movieGrid) return;
    movieGrid.innerHTML = '';
    const resultCount = document.getElementById('result-count');
    if (resultCount) resultCount.innerText = movies.length;

    if (movies.length === 0) {
        movieGrid.innerHTML = '<p class="loading-spinner" style="padding: 5rem 0; opacity: 0.5;">조건에 맞는 상영 영화가 없습니다.</p>';
        return;
    }

    movies.forEach(movie => {
        const card = document.createElement('div');
        card.className = 'movie-card';
        card.addEventListener('click', () => window.openModal(movie.title));
        
        const providerIcons = Array.from(movie.providers).map(p => {
            const color = p === 'CGV' ? '#E71A0F' : (p === 'LOTTE' ? '#FF8C00' : '#361771');
            return `<span class="provider-tag" style="background:${color}">${p}</span>`;
        }).join('');

        card.innerHTML = `
            <div class="poster-container">
                <img src="${getPosterUrl(movie.title)}" alt="${movie.title}">
                <div class="rating-badge rating-${movie.rating.replace(/[^0-9A-Z]/g, '')}">${movie.rating}</div>
            </div>
            <div class="movie-info">
                <h3>${movie.title}</h3>
                <div class="provider-list">${providerIcons}</div>
                <div class="seat-info-summary">
                    <i class="fas fa-chair"></i> 최대 <b>${movie.max_available}</b>석 가능
                </div>
                <button class="btn-check-time">자세한 시간표 보기</button>
            </div>
        `;
        movieGrid.appendChild(card);
    });
}

// --- 모달 기능 ---
window.openModal = function(movieTitle) {
    currentSelectedMovie = movieTitle;
    const modal = document.getElementById('schedule-modal');
    if (!modal) return;

    const titleEl = document.getElementById('modal-movie-title');
    const posterEl = document.getElementById('modal-movie-poster');

    if (titleEl) titleEl.innerText = movieTitle;
    if (posterEl) posterEl.src = getPosterUrl(movieTitle);
    
    modal.classList.add('active');
    document.body.style.overflow = 'hidden'; 
    window.switchTab('ALL');
};

window.closeModal = function() {
    const modal = document.getElementById('schedule-modal');
    if (modal) modal.classList.remove('active');
    document.body.style.overflow = 'auto';
};

window.switchTab = function(provider) {
    currentTab = provider;
    document.querySelectorAll('.tab-btn').forEach(btn => {
        const isMatched = (provider === 'ALL' && btn.innerText.includes('전체')) || 
                           btn.innerText.toUpperCase().includes(provider);
        btn.classList.toggle('active', isMatched);
    });
    renderScheduleList();
};

function renderScheduleList() {
    const listEl = document.getElementById('modal-schedule-list');
    if (!listEl) return;
    listEl.innerHTML = '';

    const targetSchedules = allRawSchedules.filter(item => {
        const norm = item.normalized;
        const matchMovie = norm.title === currentSelectedMovie;
        const matchProvider = currentTab === 'ALL' || norm.provider === currentTab;
        
        // 시간대 필터 추가 (모달 내부에서도 적용)
        const matchTime = norm.start_time >= currentSearchConfig.timeStart && norm.start_time <= currentSearchConfig.timeEnd;
        
        return matchMovie && matchProvider && matchTime;
    });

    const theaterGroups = targetSchedules.reduce((acc, curr) => {
        const theater = curr.normalized.theater;
        if (!acc[theater]) acc[theater] = [];
        acc[theater].push(curr.normalized);
        return acc;
    }, {});

    if (Object.keys(theaterGroups).length === 0) {
        listEl.innerHTML = '<p class="loading-spinner" style="padding: 3rem 0;">해당 조건에 상영 정보가 없습니다.</p>';
        return;
    }

    for (const [theater, times] of Object.entries(theaterGroups)) {
        const groupEl = document.createElement('div');
        groupEl.className = 'schedule-item';
        const provider = times[0].provider;
        const color = provider === 'CGV' ? '#E71A0F' : (provider === 'LOTTE' ? '#FF8C00' : '#361771');

        let timeCardsHTML = times.map(t => {
            const isClosing = t.available_seats < 10 && t.available_seats > 0;
            return `
                <div class="time-card" onclick="alert('${t.theater} ${t.start_time} 예매 페이지로 이동합니다')">
                    <span class="start-time">${t.start_time}</span>
                    <span class="hall-info">${t.format}</span>
                    <span class="seats ${t.available_seats === 0 ? 'sold-out' : ''}">
                        ${t.available_seats === 0 ? '매진' : (isClosing ? `⚠️ ${t.available_seats}석` : `${t.available_seats}석`)}
                    </span>
                </div>
            `;
        }).join('');

        groupEl.innerHTML = `
            <div class="theater-info">
                <span class="theater-name">${theater}</span>
                <span class="theater-brand" style="background:${color}">${provider}</span>
            </div>
            <div class="time-slots">
                ${timeCardsHTML}
            </div>
        `;
        listEl.appendChild(groupEl);
    }
}

// 배경 클릭 시 닫기
window.addEventListener('click', (e) => {
    const modal = document.getElementById('schedule-modal');
    if (e.target === modal) window.closeModal();
});

function getPosterUrl(title) {
    return `https://search.pstatic.net/common/?src=http%3A%2F%2Fattachment.namu.wiki%2Fmovie_poster.jpg&type=f308_432`;
}

window.toggleChip = function(el, type) {
    const parent = el.parentElement;
    const chips = parent.querySelectorAll('.filter-chip');
    const isAll = el.getAttribute('data-value') === 'all';

    if (isAll) {
        chips.forEach(c => c.classList.remove('active'));
        el.classList.add('active');
    } else {
        el.classList.toggle('active');
        const allBtn = parent.querySelector('[data-value="all"]');
        if (allBtn) allBtn.classList.remove('active');
        const actives = Array.from(chips).filter(c => c.classList.contains('active'));
        if (actives.length === 0 && allBtn) allBtn.classList.add('active');
    }
    applyFilters();
};

// 초기화 시퀀스
document.addEventListener('DOMContentLoaded', () => {
    initSearchParams();
    loadLiveMovies();
});
