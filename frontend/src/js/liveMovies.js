/**
 * Daboyeo Live Movies Aggregator
 * (C) 2024 Antigravity
 */

const movieGrid = document.getElementById('movie-grid');

// 임시 이미지 생성용 (영화 포스터 대용)
const getPosterUrl = (title) => `https://via.placeholder.com/300x450/222/ddd?text=${encodeURIComponent(title)}`;

// 실시간 영화 데이터 로드 및 중복 통합
async function loadLiveMovies() {
    if (!movieGrid) return;
    movieGrid.innerHTML = '<div class="loading-spinner">최신 상영 정보를 통합하는 중...</div>';

    try {
        // 실제 운영 시에는 모든 영화사 데이터를 가져오겠지만, 
        // 여기서는 데모를 위해 통합된 데이터 구조를 상상하여 예시를 만듭니다.
        const mockRawData = [
            { title: "인사이드 아웃 2", provider: "CGV", rating: "ALL", time: "14:30" },
            { title: "인사이드 아웃 2", provider: "LOTTE", rating: "ALL", time: "15:00" },
            { title: "인사이드 아웃 2", provider: "MEGA", rating: "ALL", time: "13:30" },
            { title: "범죄도시4", provider: "CGV", rating: "15", time: "19:00" },
            { title: "범죄도시4", provider: "LOTTE", rating: "15", time: "18:30" },
            { title: "퓨리오사: 매드맥스 사가", provider: "MEGA", rating: "15", time: "20:00" },
            { title: "퓨리오사: 매드맥스 사가", provider: "CGV", rating: "15", time: "21:00" }
        ];

        // [중요] 데이터 통합 처리 (Grouping by Title)
        const aggregated = mockRawData.reduce((acc, current) => {
            if (!acc[current.title]) {
                acc[current.title] = {
                    title: current.title,
                    rating: current.rating,
                    providers: new Set([current.provider]),
                    showtimes: [current.time]
                };
            } else {
                acc[current.title].providers.add(current.provider);
                acc[current.title].showtimes.push(current.time);
            }
            return acc;
        }, {});

        // 화면 렌더링
        renderMovieCards(Object.values(aggregated));
    } catch (err) {
        movieGrid.innerHTML = '<p style="color:red;">데이터를 불러오는 중 오류가 발생했습니다.</p>';
        console.error(err);
    }
}

function renderMovieCards(movies) {
    movieGrid.innerHTML = '';
    
    movies.forEach(movie => {
        const card = document.createElement('div');
        card.className = 'movie-card';
        
        // 상영사 아이콘 생성
        const providerIcons = Array.from(movie.providers).map(p => {
            const color = p === 'CGV' ? '#E71A0F' : (p === 'LOTTE' ? '#FF8C00' : '#361771');
            return `<span class="provider-tag" style="background:${color}">${p}</span>`;
        }).join('');

        card.innerHTML = `
            <div class="poster-container">
                <img src="${getPosterUrl(movie.title)}" alt="${movie.title}">
                <div class="rating-badge rating-${movie.rating}">${movie.rating}</div>
            </div>
            <div class="movie-info">
                <h3>${movie.title}</h3>
                <div class="provider-list">
                    ${providerIcons}
                </div>
                <button class="btn-check-time">상영 시간표 확인</button>
            </div>
        `;
        movieGrid.appendChild(card);
    });
}

// 초기화 연동
window.addEventListener('DOMContentLoaded', loadLiveMovies);
