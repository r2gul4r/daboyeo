// ============================================
// 1. 고정 헤더 기능
// ============================================

const fixedHeader = document.getElementById('fixedHeader');
let lastScrollTop = 0;

function handleHeaderScroll() {
  const scrollTop = window.scrollY;
  const shouldShowHeader = scrollTop > 100;

  if (shouldShowHeader && !fixedHeader.classList.contains('visible')) {
    fixedHeader.classList.add('visible');
    document.body.classList.add('header-visible');
    logEvent('고정 헤더', { action: '표시', scrollPosition: scrollTop });
  } else if (!shouldShowHeader && fixedHeader.classList.contains('visible')) {
    fixedHeader.classList.remove('visible');
    document.body.classList.remove('header-visible');
    logEvent('고정 헤더', { action: '숨김', scrollPosition: scrollTop });
  }

  lastScrollTop = scrollTop;
}

window.addEventListener('scroll', handleHeaderScroll, { passive: true });

// 헤더 버튼 이벤트
document.getElementById('headerSearchBtn').addEventListener('click', function () {
  logEvent('헤더 검색 버튼', { action: '클릭' });
  document.getElementById('movieInput').focus();
  document.querySelector('.search-card').scrollIntoView({ behavior: 'smooth' });
});

// ============================================
// 2. 맨 위로 버튼 기능
// ============================================

const scrollToTopBtn = document.getElementById('scrollToTop');

window.addEventListener('scroll', function () {
  if (window.scrollY > 300) {
    if (!scrollToTopBtn.classList.contains('visible')) {
      scrollToTopBtn.classList.add('visible');
      logEvent('맨 위로 버튼', { action: '표시', scrollPosition: window.scrollY });
    }
  } else {
    if (scrollToTopBtn.classList.contains('visible')) {
      scrollToTopBtn.classList.remove('visible');
      logEvent('맨 위로 버튼', { action: '숨김', scrollPosition: window.scrollY });
    }
  }
}, { passive: true });

scrollToTopBtn.addEventListener('click', function () {
  logEvent('맨 위로 버튼', { action: '클릭' });
  window.scrollTo({ top: 0, behavior: 'smooth' });
});

// ============================================
// footer: footer-dropdown
// ============================================

// ============================================
// 1. 전체 극장 데이터
// ============================================
const theaterData = {
  "cgv": {
    "서울": ["강남", "강변", "건대입구", "구로", "대학로", "동대문", "등촌", "명동", "명동역 씨네라이브러리", "목동", "미아", "불광", "상봉", "성신여대입구", "송파", "수유", "신촌아트레온", "압구정", "여의도", "영등포", "왕십리", "용산아이파크몰", "중계", "천호", "청담씨네시티", "피카디리1958", "하계", "홍대"],
    "경기": ["경기광주", "고양행신", "광교", "광교상현", "구리", "기흥", "김포", "김포운양", "김포풍무", "동백", "동탄", "동탄역", "동탄호수공원", "배곧", "범계", "부천", "부천역", "부천옥길", "북수원", "분당", "산본", "서현", "성남모란", "소풍", "수원", "수원역", "수지", "시흥", "안산", "안성", "야탑", "양주옥정", "역곡", "오리", "오산", "오산중앙", "용인", "의정부", "의정부태흥", "이천", "일산", "정왕", "죽전", "파주야당", "판교", "평촌", "평택", "평택고덕", "평택비전", "포천", "하남미사", "화정"],
    "인천": ["계양", "남주안", "부평", "연수역", "인천", "인천가정", "인천논현", "인천도화", "인천연수", "인천학익", "주안역", "청라"],
    "강원": ["강릉", "원주", "춘천"],
    "대전/충청": ["대전", "대전가오", "대전가양", "대전탄방", "대전터미널", "세종", "청주(서문)", "청주율량", "청주지웰시티", "청주터미널", "천안", "천안터미널", "천안펜타포트", "홍성"],
    "대구/경북": ["대구", "대구수성", "대구스타디움", "대구아카데미", "대구월성", "대구이시아", "대구한일", "대구현대", "경산", "구미", "안동", "포항"],
    "부산/울산/경남": ["부산명지", "서면", "서면상상마당", "센텀시티", "아시아드", "울산삼산", "울산신천", "울산진장", "김해", "마산", "양산물금", "진주혁신", "창원", "창원더시티", "해운대"],
    "광주/전라/제주": ["광주금남로", "광주상무", "광주용봉", "광주첨단", "광주터미널", "군산", "목포", "순천", "여수웅천", "익산", "전주효자", "제주", "제주노형"]
  },
  "megabox": {
    "서울": ["강남", "강동", "구의이스트폴", "군자", "더부티크목동현대백화점", "동대문", "마곡", "목동", "상봉", "상암월드컵경기장", "센트럴", "송파파크하비오", "신촌", "이수", "창동", "코엑스", "픽쳐하우스", "홍대", "화곡"],
    "경기": ["고양스타필드", "광명AK플라자", "구리", "김포한강신도시", "남양주현대아울렛 스페이스원", "동탄", "미사강변", "백석벨라시타", "별내", "부천스타필드시티", "분당", "수원스타필드", "안성스타필드", "영통", "의정부민락", "일산벨라시타", "킨텍스", "하남스타필드"],
    "인천": ["검단", "송도", "영종", "인천논현", "청라지젤"],
    "대전/충청/세종": ["대전신세계 아트앤사이언스", "대전현대아울렛", "세종나성", "천안", "청주충북대"],
    "부산/대구/경상": ["대구신세계", "대구이시아", "마산", "문경", "부산대", "사상", "양산라피에스타", "울산", "해운대(장산)"],
    "광주/전라": ["광주상무", "광주하남", "목포하당(포르모)", "순천", "전주혁신"],
    "강원": ["남춘천", "속초", "원주혁신", "춘천석사"],
    "제주": ["제주서귀포", "제주아라", "제주연동"]
  },
  "lotte": {
    "서울": ["가산디지털", "가양", "강동", "건대입구", "김포공항", "노원", "독산", "도곡", "라페스타", "마곡나루", "브로드웨이(신사)", "서울대입구", "수락산", "수유", "신림", "신사", "에비뉴엘(명동)", "영등포", "용산", "월드타워", "은평(롯데몰)", "장안", "청량리", "합정", "홍대입구", "황학"],
    "경기": ["검단", "광교(아울렛)", "광명(아울렛)", "광명사거리", "구리", "기흥", "동탄", "마석", "병점", "부천(역전)", "부천역", "북수원", "산본피트인", "서수원", "성남중앙", "센트럴락", "송탄", "수원(역)", "수지", "시흥장현", "안산", "안산고잔", "안양", "안양일번가", "양주옥정", "용인기흥", "용인역북", "의정부민락", "인덕원", "일산", "주엽", "파주운정", "판교(창조경제밸리)", "평촌(범계역)", "평택비전"],
    "인천": ["부평", "부평역사", "송도", "아시아드", "인천터미널", "영종하늘도시"],
    "강원": ["남춘천", "동해", "원주무실", "춘천"],
    "대전/충청": ["대전(백화점)", "대전둔산", "대전센트럴", "서청주(아울렛)", "아산터미널", "청주(성안길)", "청주용암", "충주(연수)", "천안불당"],
    "대구/경북": ["대구광장", "대구율하", "동성로", "상인", "성서", "경산", "경주", "구미공단", "포항"],
    "부산/울산/경남": ["광복", "동래", "부산본점", "사상", "센텀시티", "울산(백화점)", "울산성남", "거제", "김해부원", "마산(합성동)", "진주혁신", "창원", "해운대"],
    "광주/전라/제주": ["광주(백화점)", "광산", "수완(아울렛)", "군산나운", "목포", "순천", "익산모현", "전주(백화점)", "전주평화", "제주아라"]
  }
};

// ============================================
// 2. 극장 오버레이 로직
// ============================================
const theaterchBtn = document.getElementById('theaterSearchBtn');
const overlay = document.getElementById('theaterOverlay');
const closeBtn = document.getElementById('closeOverlay');
const grid = document.getElementById('theaterGrid');
const tabItems = document.querySelectorAll('.overlay-tab-item');

theaterchBtn.addEventListener('click', (e) => {
  e.stopPropagation();
  overlay.classList.toggle('active');
  if (overlay.classList.contains('active')) {
    renderTheaters(document.querySelector('.overlay-tab-item.active').dataset.brand);
  }
});

closeBtn.addEventListener('click', () => {
  overlay.classList.remove('active');
});

tabItems.forEach(tab => {
  tab.addEventListener('click', () => {
    tabItems.forEach(t => t.classList.remove('active'));
    tab.classList.add('active');
    renderTheaters(tab.dataset.brand);
  });
});

function renderTheaters(brand) {
  const data = theaterData[brand];
  let html = '';
  for (const region in data) {
    html += `
                    <div class="region-row">
                        <div class="region-name">${region}</div>
                        <div class="theater-list">
                            ${data[region].map(name => `<span class="theater-item">${name}</span>`).join('')}
                        </div>
                    </div>
                `;
  }
  grid.innerHTML = html;
}

// ============================================
// 3. 기존 패밀리사이트 드롭다운 로직 유지
// ============================================
const dropdown = document.querySelector('.custom-dropdown');
const selected = dropdown.querySelector('.selected');
const items = dropdown.querySelectorAll('.dropdown-list li');

selected.addEventListener('click', (e) => {
  e.stopPropagation();
  dropdown.classList.toggle('active');
});

items.forEach(item => {
  item.addEventListener('click', () => {
    const url = item.dataset.url;
    selected.textContent = item.textContent;
    dropdown.classList.remove('active');
    if (url && url !== '/') window.open(url, '_blank');
  });
});

// 공통: 외부 클릭 시 닫기
document.addEventListener('click', (e) => {
  if (!dropdown.contains(e.target)) dropdown.classList.remove('active');
  if (overlay.classList.contains('active') && !overlay.contains(e.target) && e.target !== theaterchBtn) {
    overlay.classList.remove('active');
  }
});