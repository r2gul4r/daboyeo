# Frontend

바닐라 HTML, CSS, JavaScript 기반 사용자 화면 영역이다.

Bootstrap이나 번들러는 아직 도입하지 않는다. 사용자 화면은 영화 검색, 상영 시간 비교, 가격 비교, 좌석 상태 확인 흐름에 맞춘 커스텀 CSS와 모듈형 JS로 유지한다.

## 기본 방향

- 프레임워크 없음
- 번들러 없음
- ES module 기반 JS
- API 호출은 `src/js/api/`에 둔다
- 화면 단위 로직은 `src/js/pages/`에 둔다
- CSS는 공통, 레이아웃, 페이지 단위로 분리한다

## 로컬 확인

정적 파일이라 간단한 정적 서버로 보면 된다.

```powershell
cd frontend
python -m http.server 5173
```

브라우저 검증은 사용자가 명시적으로 요청할 때만 한다.
