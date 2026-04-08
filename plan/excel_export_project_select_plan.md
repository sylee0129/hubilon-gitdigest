# 전체 프로젝트 탭 — 프로젝트 선택 엑셀 내보내기

## Context

현재 "전체 프로젝트" 탭에는 하단에 "📥 엑셀 내보내기" 버튼이 있지만, 항상 **전체** 프로젝트를 내보낸다.
사용자가 특정 프로젝트만 선택해서 내보내고 싶은 요구사항이다.
현재 백엔드 `/reports/export`는 단일 `projectId`(선택)만 받으며, 프론트엔드에는 체크박스 선택 UI가 없다.

---

## 변경 범위

### 프론트엔드 (3개 파일)

#### 1. `frontend/src/services/reportApi.ts`
- `ReportQueryParams`에 `projectIds?: number[]` 추가
- `exportExcel()`에서 `projectIds`를 `projectIds=1&projectIds=2` 형태로 전달
  ```ts
  exportExcel: async (params: ReportQueryParams): Promise<Blob> => {
    const res = await apiClient.get('/reports/export', {
      params,
      responseType: 'blob',
      // axios가 배열을 repeat 형태로 직렬화하도록 paramsSerializer 옵션 추가
    })
  }
  ```
  > `axios`의 `paramsSerializer`에 `{ indexes: null }` 설정 또는 URLSearchParams 직접 구성

#### 2. `frontend/src/components/report/ReportCard.tsx`
- 선택적 props 추가:
  ```ts
  interface ReportCardProps {
    report: Report
    showCheckbox?: boolean     // 체크박스 표시 여부 (all 탭에서만 true)
    isSelected?: boolean
    onSelect?: (projectId: number, checked: boolean) => void
  }
  ```
- 체크박스를 카드 좌측 상단에 렌더링 (showCheckbox가 true일 때만)
- 기존 카드 동작 유지

#### 3. `frontend/src/pages/ReportDashboard.tsx`
- `selectedProjectIds: Set<number>` 로컬 상태 추가
- activeTab이 `'individual'`로 바뀌면 선택 초기화
- `handleExport` 수정: "all" 탭에서 selectedProjectIds가 있으면 해당 IDs만 전달
- `ReportCard`에 `showCheckbox`, `isSelected`, `onSelect` props 전달
- 탭 버튼 우측에 "전체 선택 / 해제" 버튼 추가 (all 탭에서만 노출)
- 엑셀 버튼 옆에 `N개 선택됨` 뱃지 표시 (선택이 있을 때)

---

### 백엔드 (3개 파일)

#### 1. `backend/src/main/java/com/hubilon/modules/report/adapter/in/web/ReportController.java`
- `exportExcel()` 파라미터 변경:
  ```java
  @RequestParam(required = false) List<Long> projectIds,
  // 기존 단일 projectId 파라미터 제거
  ```

#### 2. `backend/src/main/java/com/hubilon/modules/report/application/dto/ReportExportQuery.java`
- `Long projectId` → `List<Long> projectIds` 변경
  ```java
  public record ReportExportQuery(List<Long> projectIds, LocalDate startDate, LocalDate endDate) {}
  ```

#### 3. 엑셀 내보내기 서비스 구현체 (ReportExportUseCase 구현)
- `projectIds`가 비어 있으면 전체 조회 (기존 동작 유지)
- `projectIds`가 있으면 `IN` 조건으로 필터링

---

## 핵심 로직 흐름

```
[전체 프로젝트 탭]
ReportCard에 체크박스 표시
  └─ 체크 → selectedProjectIds(Set) 업데이트
  └─ 전체선택 버튼 → reportsQuery.data의 모든 projectId 추가

[엑셀 내보내기 클릭]
  선택 없음 → 기존처럼 전체 내보내기 (projectIds 미전달)
  선택 있음 → projectIds=[1,2,3] 전달 → 백엔드 IN 필터
```

---

## 주의사항

- `projectIds` 배열을 axios query param으로 전달 시 `paramsSerializer` 설정 필요
  - 방법 A: `axios.get(url, { params: { projectIds: [1,2,3] }, paramsSerializer: { indexes: null } })`
  - 방법 B: `URLSearchParams`로 직접 직렬화

- 백엔드 `@RequestParam List<Long> projectIds`는 Spring이 `projectIds=1&projectIds=2` 형태로 자동 바인딩

- 기존 단일 `projectId` 파라미터를 사용하는 `/reports` GET API는 **변경하지 않음** (영향 범위 최소화)

---

## 수정 파일 목록

| 파일 | 변경 유형 |
|------|----------|
| `frontend/src/services/reportApi.ts` | 수정 |
| `frontend/src/components/report/ReportCard.tsx` | 수정 |
| `frontend/src/pages/ReportDashboard.tsx` | 수정 |
| `backend/.../adapter/in/web/ReportController.java` | 수정 |
| `backend/.../application/dto/ReportExportQuery.java` | 수정 |
| 백엔드 export 서비스 구현체 | 수정 |

---

## 검증 방법

1. 전체 프로젝트 탭 → 체크박스 없는 상태에서 "엑셀 내보내기" → 전체 프로젝트 xlsx 다운로드 확인
2. 전체 프로젝트 탭 → 일부 프로젝트 체크 → "엑셀 내보내기" → 선택된 프로젝트만 포함된 xlsx 확인
3. "전체 선택" → 모든 카드 체크 확인, "전체 해제" → 모두 해제 확인
4. 개별 프로젝트 탭 전환 시 체크박스 미표시 + 선택 초기화 확인
5. 백엔드: `GET /reports/export?projectIds=1&projectIds=2&startDate=...&endDate=...` 요청 정상 처리 확인
