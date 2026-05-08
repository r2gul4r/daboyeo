# Workspace Override: daboyeo

CGV, 롯데시네마, 메가박스 상영 정보를 수집하고 비교하는 개인용 영화 상영 데이터 도구.

이 파일은 전역 AGENTS 규칙 위에 이 저장소 전용 제약만 추가한다.

## Repository Facts

- task board path: `STATE.md`
- error log path: `ERROR_LOG.md`
- shell runtime: `PowerShell`
- 유지보수 우선순위: 로컬 실행, 수집, 검증 흐름을 실제 사용자가 따라가기 쉽게 유지

## Shared Contracts

- 존재하지 않는 외부 문서를 source of truth로 가정하지 않는다.
- 수집기는 극장사 원본 데이터 특성을 가능한 한 보존한다.
- 비교 모델은 최소 공통 필드 중심으로 유지한다.
- 비밀값, 토큰, 쿠키, API 키는 코드에 하드코딩하지 않는다.
- 외부 입력과 HTML 렌더링은 경계에서 검증하고 이스케이프한다.
- 검증은 로컬 명령과 코드 리뷰를 기본값으로 둔다.

## Shared Asset Paths

- `collectors/**`
- `docs/**`
- `scripts/**`

## Repo-Specific Hard Triggers

- 새 프레임워크나 큰 외부 라이브러리를 도입하는 변경
- 수집 데이터 모델이나 원본 속성 보존 정책을 바꾸는 변경
- PowerShell 기반 실행 흐름을 바꾸는 변경
- 인증, 권한, 세션, 비밀값, 외부 요청, HTML 렌더링을 건드리는 변경
- 배포 또는 외부 서비스 연결 설정을 바꾸는 변경

## Manual Approval Zones

- 기술 스택 변경
- 배포 또는 외부 서비스 연결
- 비밀값 또는 계정 설정 변경
- 사용자용/관리자용 기능 경계 변경

## Reviewer Focus

- 수집기 우선순위와 데이터 보존 정책 유지
- 비밀값 하드코딩 방지
- 검증 누락 여부
- 사용자용/관리자용 책임 분리 유지
