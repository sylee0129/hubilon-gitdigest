# Plan: 엑셀 내보내기 기능 제거

## Context
사용자 요청에 따라 엑셀 내보내기(Excel Export) 기능 전체를 제거한다.
백엔드 API, 서비스 클래스, UseCase/DTO, 의존성, 프론트엔드 버튼/훅/API 함수 모두 삭제.

---

## 제거 대상

### 백엔드 — 삭제할 파일
1. `backend/src/main/java/com/hubilon/modules/report/domain/port/in/ReportExportUseCase.java`
2. `backend/src/main/java/com/hubilon/modules/report/application/dto/ReportExportQuery.java`
3. `backend/src/main/java/com/hubilon/modules/report/application/service/ReportExportService.java`

### 백엔드 — 수정할 파일
4. `backend/src/main/java/com/hubilon/modules/report/adapter/in/web/ReportController.java`
   - `exportExcel()` 메서드 (라인 79-100) 제거
   - `ReportExportUseCase` import 및 주입 필드 제거

5. `backend/build.gradle.kts`
   - `implementation("org.apache.poi:poi-ooxml:5.3.0")` 제거

### 프론트엔드 — 수정할 파일
6. `frontend/src/services/reportApi.ts`
   - `exportExcel()` 함수 제거

7. `frontend/src/hooks/useReports.ts`
   - `useExportExcel()` 훅 제거

8. `frontend/src/pages/ReportDashboard.tsx`
   - `useExportExcel` import 제거
   - `handleExport` 함수 제거
   - 엑셀 내보내기 버튼 UI 제거

---

## 추가 조치 (Review 결과 반영)
- `ReportController.java`: `ContentDisposition`, `HttpHeaders`, `MediaType`, `DateTimeFormatter` import 4개 추가 제거
- `ReportDashboard.module.css`: `.exportBtn` 관련 CSS 3개 블록 제거
- `ReportDashboard.tsx`: 버튼 제거 후 빈 `<div className={styles.footer}>` 및 `.footer` CSS 처리
- `ReportQueryPort.java`의 메서드는 다른 기능에서 사용될 수 있으므로 수정하지 않음
- 프론트엔드 package.json에는 Excel 전용 라이브러리 없으므로 변경 불필요

---

## 검증
1. 백엔드 빌드: `./gradlew build` — Java 17+ 환경 필요 (기존 환경 이슈, 이번 변경과 무관)
2. 프론트엔드 빌드: `npm run build` — ✅ 성공
3. 화면에 엑셀 내보내기 버튼 사라졌는지 확인

## Review 결과
- 검토일: 2026-04-10
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: 이상 없음 (누락 항목 보완 후 구현)
