# UI 리디자인: Aura Logic 스타일 적용

## Context
현재 UI는 Notion 스타일(파란색 primary, 베이지 배경, 시스템 폰트)을 사용 중. `sample/stitch-sample html`의 Aura Logic 디자인 스타일(보라색 primary, Inter 폰트, 둥근 카드, 그라데이션, Material Design 3 컬러 시스템)로 전환한다.

**접근 방식:** CSS Modules 유지 (Tailwind 전환 없음). CSS 변수 + 각 .module.css 파일 수정으로 디자인 전환.

---

## 수정 파일 목록 (11개)

### Phase 1: 기반 (폰트 + 컬러 토큰)

**1. `frontend/index.html`**
- `<head>`에 Inter 폰트 CDN 링크 추가

**2. `frontend/src/index.css`**
- CSS 변수 컬러 팔레트 변경 (파란→보라):
  - `--color-primary: #702ae1`, `--color-primary-hover: #5b1fc4`
  - `--color-bg: #f4f0fa`, `--color-border: #e8e0f0`
  - `--color-text-primary: #1e1b2e`, `--color-text-secondary: #7c7591`
- 신규 변수 추가:
  - `--color-primary-light`, `--color-primary-surface`, `--color-secondary`
  - `--radius-card`, `--radius-button`, `--radius-input`
  - `--shadow-card`, `--shadow-card-hover`, `--transition-smooth`
- `body` 폰트 → `'Inter', ...` 로 변경

### Phase 2: 레이아웃 셸

**3. `frontend/src/components/layout/Header.module.css`**
- 높이 44→56px, border-bottom 제거, box-shadow 추가
- 버튼들 → rounded-full (pill shape)
- 드롭다운 → 더 큰 border-radius, 더 부드러운 shadow
- hover 시 translateX(4px) 슬라이드 효과

**4. `frontend/src/components/layout/Sidebar.module.css`**
- 다크 사이드바: `background-color: #1e1b2e`, `color: #fff`
- 우측 모서리 라운딩: `border-radius: 0 2rem 2rem 0`
- 프로젝트 아이템 hover 시 translateX(4px) 효과
- 버튼/구분선 → 반투명 흰색 처리

**5. `frontend/src/pages/ReportDashboard.module.css`**
- 탭 → pill 스타일 (underline 제거, 활성탭 배경색)
- padding/gap 확대 (24→32px, 12→24px)
- 리사이즈 핸들 → 더 subtle한 스타일
- 엑셀 내보내기 버튼 → pill shape

### Phase 3: 콘텐츠 카드

**6. `frontend/src/components/report/ReportCard.module.css`**
- 카드 → border 제거, radius 16px, 보라색 그림자
- hover 시 translateY(-2px) 리프트 효과
- AI 버튼 → 그라데이션 (`linear-gradient(135deg, primary, secondary)`)
- stat 아이템 → pill 배지 스타일
- padding/margin 확대

**7. `frontend/src/components/report/CommitList.module.css`**
- groupHeader 배경 → primary-light
- commitRow hover → translateX(4px) 슬라이드
- border-radius 확대

**8. `frontend/src/components/report/FileChangeList.module.css`**
- badge → 더 큰 radius
- badgeM 색상 → primary 테마 매칭
- fileRow hover → translateX(2px) 효과

### Phase 4: 보조 컴포넌트

**9. `frontend/src/components/report/ReportCard.module.css`** (SummaryEditor 부분 포함)
- textarea focus shadow → 보라색 계열
- 저장/취소 버튼 → pill shape

**10. `frontend/src/components/project/AddProjectModal.module.css`**
- 모달 radius 16→24px, 더 부드러운 shadow
- input focus shadow → 보라색 계열
- 버튼들 → pill shape
- oauthSection → primary-surface 색상

**11. `frontend/src/components/common/Toast.module.css`**
- pill shape, backdrop-filter blur 추가

---

## TSX 변경: 없음
아이콘(이모지)과 컴포넌트 구조는 변경하지 않음. 순수 CSS 수정만 진행.

---

## 검증 방법
1. `npm run dev`로 개발 서버 실행
2. 전체 페이지 시각 확인: 컬러, 폰트, 라운딩, 그림자
3. 사이드바 리사이즈 핸들 동작 확인
4. 탭 전환, 프로젝트 선택 상호작용 확인
5. AI 요약 버튼 그라데이션 확인
6. 모달 열기/닫기 확인
7. 반응형(900px 미만) 확인
