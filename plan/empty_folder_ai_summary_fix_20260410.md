# 빈 폴더 AI 요약/편집 기능 개선 계획

**날짜:** 2026-04-10  
**상태:** 계획

---

## 문제 상황

### 문제 1: 빈 폴더 데드락
폴더에 프로젝트가 없을 때:
- AI 요약 버튼 클릭 → 404 오류 발생
- FolderSummary가 생성되지 않음
- 편집 버튼 비활성화 → 수동 입력도 불가
- **데드락**: FolderSummary 없이는 아무것도 할 수 없음

### 문제 2: 커밋/기여자 통계 미갱신
AI 요약 생성 후 커밋 개수·기여자 수 배지가 갱신되지 않음:
- 배지는 `reports` prop에서 실시간 계산 (`FolderReportPanel.tsx:32-35`)
- AI 요약 생성 후 `['folder-summary']` 캐시만 무효화, `['reports']` 캐시는 미포함
- 백엔드가 커밋 재집계해도 프론트는 구버전 `reports` 데이터를 계속 사용
- 날짜 범위 변경 시에도 배지가 즉시 반영 안 되는 케이스 발생 가능

---

## 근본 원인

### 백엔드 (Critical)

**파일:** `backend/src/main/java/com/hubilon/modules/report/application/service/FolderSummaryAiSummarizeService.java:53-56`

```java
List<Project> projects = projectQueryPort.findByFolderId(command.folderId());
if (projects.isEmpty()) {
    throw new NotFoundException("폴더에 프로젝트가 없습니다.");  // ← 문제
}
```

빈 폴더 → NotFoundException → HTTP 404 → FolderSummary 생성 실패

### 프론트엔드 (High)

**파일:** `frontend/src/components/report/FolderReportPanel.tsx:154, 171`

```tsx
disabled={!folderSummary}  // 편집 버튼
disabled={!folderSummary}  // 저장 버튼
```

FolderSummary null → 편집/저장 불가 → 수동 입력 진입 불가

---

## 데드락 흐름

```
빈 폴더 열기
  → GET /api/reports/folder-summary → data: null
  → folderSummary = null
  → 편집 버튼 disabled
  → AI 요약 클릭 → 404 NotFoundException
  → folderSummary 여전히 null
  → 수동 입력도 불가 (DeadLock)
```

---

## 개선 방향

### 백엔드

#### 1. `FolderSummaryAiSummarizeService.java`
- `projects.isEmpty()` 시 NotFoundException 제거
- 빈 폴더도 정상 케이스로 처리 (커밋 0개)
- `AiSummaryAdapter.java:49-59`에 이미 커밋 없음 stub 처리 로직 있음 → 재사용

```java
// 변경 전
if (projects.isEmpty()) {
    throw new NotFoundException("폴더에 프로젝트가 없습니다.");
}

// 변경 후
// projects가 비어있어도 계속 진행 (커밋 수집 결과가 빈 리스트)
// → AiSummaryAdapter가 "커밋 없음" stub 응답 반환
```

### 프론트엔드

#### 2. `FolderReportPanel.tsx` — 편집 버튼 클릭 분기
- `disabled` 조건 제거
- 편집 버튼 `onClick`에서 분기:
  - `folderSummary` 없음 → `handleGenerateAi()` 호출 (AI 요약 먼저 생성)
  - `folderSummary` 있음 → `setIsEditing(true)`
- `handleSave`는 저장 전용으로 유지 (책임 혼합 금지)

#### 3. `FolderReportPanel.tsx` — 배지 데이터 소스 단일화
- 배지는 `reports` props 집계값 단독 사용 유지 (혼용 제거)
- AI 요약 후 `reports` 캐시 무효화로 자동 갱신

#### 4. `useReports.ts` — 캐시 무효화 범위 확대
- `useGenerateFolderAiSummary.onSuccess`: `['reports']` 캐시 추가 무효화
- `useUpdateFolderSummary.onSuccess`: `['reports']` 캐시 추가 무효화

---

## 수정 대상 파일

| 파일 | 변경 내용 |
|------|----------|
| `FolderSummaryAiSummarizeService.java:53-56` | `projects.isEmpty()` 에러 처리 제거 |
| `FolderReportPanel.tsx:169-175` | 편집 버튼 `disabled` 제거, `onClick` 분기 처리 |
| `useReports.ts:22-27` | `useGenerateFolderAiSummary.onSuccess`에 `['reports']` 캐시 무효화 추가 |
| `useReports.ts:41-43` | `useUpdateFolderSummary.onSuccess`에 `['reports']` 캐시 무효화 추가 |

---

## Review 결과
- 검토일: 2026-04-10
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: 이슈 발견 후 계획 조정 완료 (F-1 편집버튼 분기, F-2 캐시 무효화 범위, F-3 배지 혼용 제거)

## 검증 방법

1. 빈 폴더(프로젝트 없음) 생성
2. AI 요약 버튼 클릭 → 오류 없이 "커밋 없음" 요약 생성 확인
3. 편집 버튼 클릭 → 진입 가능 확인
4. 내용 입력 후 저장 → FolderSummary 저장 확인
5. 프로젝트 있는 기존 폴더 → AI 요약 후 커밋/기여자 배지 즉시 갱신 확인
6. 날짜 범위 변경 → 배지 값 변경 확인
7. 기존 폴더(프로젝트 있음) → 동작 변화 없음 확인
