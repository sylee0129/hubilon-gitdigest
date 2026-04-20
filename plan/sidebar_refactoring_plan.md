# 사이드바 리팩토링 Plan

## 목표
- 사이드바 열기/접기(Toggle) 기능 추가
- 사이드바 min-width 설정
- 사이드바 배경색 흰색(#FFFFFF) 변경
- 주간보고 스케줄러 화면에 사이드바 노출

---

## 현황 파악 (착수 전 확인)

- [ ] 현재 사이드바 컴포넌트 위치 확인
- [ ] 주간보고 스케줄러 페이지 라우트/컴포넌트 확인
- [ ] 레이아웃 구조 확인 (공통 Layout 사용 여부)

---

## 태스크 목록

### FE-01: 사이드바 Toggle 기능 추가
- 접기/펼치기 버튼 UI 추가
- 접었을 때: 아이콘만 표시 (width ~48px)
- 펼쳤을 때: 기존 너비 유지
- 상태 관리: `useState` 또는 Context (페이지 이동 시 유지 목적이면 Context 권장)

### FE-02: 사이드바 min-width 설정
- 펼쳤을 때 `min-width` 설정 (예: 200px)
- 드래그 조절 시 min 이하로 줄어들지 않도록 제한

### FE-03: 사이드바 배경색 변경
- `background: #FFFFFF`
- 콘텐츠 영역과 구분을 위해 `border-right` 또는 `box-shadow` 추가

### FE-04: 주간보고 스케줄러 페이지 레이아웃 변경
- 현재: 전체 화면 단독 사용
- 변경: 대시보드와 동일한 레이아웃(사이드바 + 콘텐츠 영역) 적용
- 사이드바 : 콘텐츠 = 적절한 비율(flex 1:N)로 배치

---

## 구현 순서

1. **사이드바 컴포넌트 파악** — 기존 구조 확인
2. **FE-03** — 배경색 변경 (가장 단순, 선행)
3. **FE-01 + FE-02** — Toggle + min-width (세트로 구현)
4. **FE-04** — 스케줄러 페이지 레이아웃 통합

---

## 기술 요구사항

| 항목 | 내용 |
|------|------|
| 상태 관리 | **Zustand** — Toggle 상태를 전역 관리하여 페이지 이동 시 유지 (F-01/F-02 해결) |
| 드래그 리사이즈 | 공통 레이아웃 컴포넌트(`SidebarLayout`)로 추출 — ReportDashboard·SchedulerPage 공유 |
| min-width | SIDEBAR_MIN=160, SIDEBAR_MAX=480 (기존 값 유지) |
| 반응형 | 사이드바 접힘 시 콘텐츠 영역 flex로 확장 |
| 스타일 | 기존 CSS/Tailwind 방식 유지 |

## 설계 결정 (Review 반영)

- **F-01/F-02**: Toggle 상태 → Zustand (`useSidebarStore`) 로 관리
  - `isCollapsed: boolean`, `toggleSidebar()` 액션 포함
  - Toggle 접기 시 width=48px 강제, 드래그 리사이즈 비활성화
- **F-03**: SchedulerPage 드래그 리사이즈 **포함**
  - 드래그 리사이즈 로직을 `SidebarLayout` 공통 컴포넌트로 추출
  - ReportDashboard·SchedulerPage 모두 `SidebarLayout` 사용
- **R-01/R-02**: 리팩토링은 이번 범위에서 제외 (별도 태스크로 분리)

---

## 완료 기준

- [ ] 사이드바 접기/펼치기 버튼 동작 확인
- [ ] 접혔을 때 아이콘만 보이고 콘텐츠 영역 확장 확인
- [ ] min-width 이하로 줄어들지 않음 확인
- [ ] 사이드바 배경 흰색, 구분선/그림자 확인
- [ ] 주간보고 스케줄러 페이지에 사이드바 노출 확인
- [ ] 페이지 이동 시 사이드바 상태 유지 확인

---

## Review 결과
- 검토일: 2026-04-20
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: 이슈 발견 후 설계 결정 완료 (B안 채택, 리사이즈 포함)
- 반영: Toggle 상태 Zustand 관리, SidebarLayout 공통 컴포넌트 추출
