# 고도화 기능 설계: View Mode 분리 + 폴더 단위 통합 AI 보고서

## Context

`prompt/godohwa_prompt.md` 요청 사항 구현 설계.  
현재 시스템은 프로젝트 단위 보고서만 지원. 이 플랜은 **사업 폴더** 단위 View Mode를 분리하고, 폴더 내 모든 프로젝트를 통합한 AI 보고서를 제공하는 기능을 추가한다.

---

## 현재 상태 분석

| 항목 | 현재 | 목표 |
|------|------|------|
| activeTab | 'all' / 'individual' (전체 프로젝트 기준) | 'all' = 선택된 폴더의 모든 프로젝트 카드 나열 |
| 보고서 패널 | 단일 프로젝트 AI 요약 | 선택된 폴더 전체 통합 AI 요약, 폴더명 제목 |
| 폴더 선택 | 사이드바 expand/collapse만 | 폴더 클릭 → 폴더 선택 상태 + Total View 전환 |
| 선택 상태 | selectedProjectId만 존재 | selectedFolderId 추가 |

---

## 변경 범위

### [Frontend]

#### 1. `frontend/src/store/useReportStore.ts`
- `selectedFolderId: number | null` 상태 추가
- `setSelectedFolder(folderId: number | null)` 액션 추가
- 폴더 선택 시 `activeTab`을 `'all'`로 자동 설정

#### 2. `frontend/src/components/layout/Sidebar.tsx`
- 폴더 헤더 클릭 이벤트 변경:
  - 현재: `handleToggleFolder()` (expand/collapse)
  - 변경: expand/collapse 유지 + `setSelectedFolder(folderId)` 호출
- 선택된 폴더 강조 스타일 (활성 배경색)

#### 3. `frontend/src/pages/ReportDashboard.tsx`
- Total View 필터링 로직 수정:
  ```ts
  // 현재: 모든 프로젝트
  // 변경: selectedFolderId 있으면 해당 폴더 프로젝트만
  const targetProjects = selectedFolderId
    ? projects.filter(p => p.folderId === selectedFolderId)
    : projects
  ```
- useReports 쿼리 파라미터에 `projectIds` 배열 전달:
  ```ts
  projectIds: targetProjects.map(p => p.id)
  ```
- 우측 패널 조건 분기:
  - 폴더 선택 상태 → `<FolderReportPanel />`
  - 프로젝트 선택 상태 → 기존 `<ReportPanel />`

#### 4. `frontend/src/components/report/FolderReportPanel.tsx` (신규)
- Props: `{ folderId: number, folderName: string, reports: Report[] }`
- 폴더명을 패널 타이틀로 표시
- 통계 요약: 전체 커밋 수, 기여자 수 (중복 제거)
  ```ts
  const totalCommits = reports.reduce((sum, r) => sum + r.commitCount, 0)
  const uniqueContributors = new Set(
    reports.flatMap(r => r.commits.map(c => c.authorEmail))
  ).size
  ```
- AI 요약 버튼 → `generateFolderAiSummary(folderId, startDate, endDate)` 호출
- 요약 편집/저장 기능 (기존 SummaryEditor 재사용)

#### 5. `frontend/src/services/reportApi.ts`
- `generateFolderAiSummary(folderId, startDate, endDate)` 추가
  - POST `/api/reports/folder-summary/ai-summary`
- `getFolderSummary(folderId, startDate, endDate)` 추가
  - GET `/api/reports/folder-summary`

#### 6. `frontend/src/hooks/useReports.ts`
- `useGenerateFolderAiSummary()` 훅 추가 (React Query mutation)
- `useFolderSummary(folderId, startDate, endDate)` 훅 추가

#### 7. `frontend/src/types/report.ts`
- `FolderSummary` 타입 추가:
  ```ts
  FolderSummary {
    folderId: number
    folderName: string
    startDate: string
    endDate: string
    totalCommitCount: number
    uniqueContributorCount: number
    summary: string
    manuallyEdited: boolean
    aiSummaryFailed: boolean
  }
  ```

---

### [Backend]

#### 1. 신규: FolderSummary 도메인/기능

**도메인 모델**: `FolderSummary`
- folderId, folderName, startDate, endDate
- totalCommitCount, uniqueContributorCount
- summary, manuallyEdited, aiSummaryFailed

**DB 테이블**: `folder_summary`
- 폴더+기간 조합을 PK로 저장 (folderId + startDate + endDate)

**엔드포인트**:

| HTTP | 경로 | 설명 |
|------|------|------|
| GET | `/api/reports/folder-summary` | 폴더 요약 조회 (folderId, startDate, endDate) |
| POST | `/api/reports/folder-summary/ai-summary` | 폴더 전체 AI 요약 생성 |
| PUT | `/api/reports/folder-summary/{id}/summary` | 폴더 요약 수동 편집 |

**AI 요약 생성 로직**:
```
1. 해당 기간의 folderId에 속한 모든 Project의 Report 조회
2. 모든 Report의 커밋 메시지 통합
3. 기존 AI 요약 서비스 재활용 (GitLabAiSummaryService 등)
4. FolderSummary 엔티티에 저장
```

#### 2. 기존 ReportController 수정 (선택)
- GET `/api/reports?projectIds=1,2,3` 배열 파라미터 지원 확인 및 추가
  - 현재 `projectId` 단수만 지원 → `projectIds` 복수 지원 추가

---

## 컴포넌트 데이터 흐름

```
Sidebar (폴더 클릭)
    ↓ setSelectedFolder(folderId)
    ↓ setTab('all')
useReportStore (selectedFolderId 갱신)
    ↓
ReportDashboard
    ├─ targetProjects = projects.filter(p => p.folderId === selectedFolderId)
    ├─ useReports({ projectIds: targetProjects.map(p => p.id), ... })
    ↓
중앙 섹션: 폴더 내 ProjectCard 세로 나열 (Total View)
우측 섹션: FolderReportPanel
    ├─ 폴더명 타이틀
    ├─ 통합 통계 (커밋 수, 기여자 수)
    ├─ AI 요약 버튼 → generateFolderAiSummary()
    └─ 요약 편집/저장 (SummaryEditor 재사용)
```

---

## 수정 대상 파일 목록

### Frontend
| 파일 | 변경 유형 |
|------|---------|
| `frontend/src/store/useReportStore.ts` | 수정 |
| `frontend/src/components/layout/Sidebar.tsx` | 수정 |
| `frontend/src/pages/ReportDashboard.tsx` | 수정 |
| `frontend/src/components/report/FolderReportPanel.tsx` | 신규 |
| `frontend/src/services/reportApi.ts` | 수정 |
| `frontend/src/hooks/useReports.ts` | 수정 |
| `frontend/src/types/report.ts` | 수정 |

### Backend
| 파일/모듈 | 변경 유형 |
|------|---------|
| `backend/.../report/domain/model/FolderSummary.java` | 신규 |
| `backend/.../report/adapter/out/persistence/FolderSummaryJpaEntity.java` | 신규 |
| `backend/.../report/adapter/in/web/ReportController.java` | 수정 (projectIds 배열 지원) |
| `backend/.../report/adapter/in/web/FolderSummaryController.java` | 신규 |
| `backend/.../report/application/service/FolderSummaryService.java` | 신규 |

---

## 리뷰 보완 사항 (고/중 이슈)

### 고 이슈

1. **빈 폴더 오동작 방지**
   - 백엔드: `projectIds` 빈 배열 수신 시 즉시 빈 결과 반환 (전체 조회 분기 차단)
   - 프론트: `targetProjects.length === 0`이면 API 호출 스킵, 빈 상태 UI 표시

2. **폴더→프로젝트 조회**
   - 기존 `GET /api/projects` 응답에 `folderId` 필드가 이미 포함됨
   - 프론트 `useProjects()` 훅 결과를 `folderId` 기준으로 클라이언트 필터링으로 해결
   - 별도 엔드포인트 불필요

### 중 이슈

3. **projectIds 소유권 검증**
   - 백엔드 `ReportAnalyzeService`에서 `projectIds` 각 항목이 현재 로그인 사용자의 접근 가능 프로젝트인지 확인 후 조회
   - 없는 id 또는 권한 없는 id는 `404 Not Found` 반환

4. **FolderSummary PUT 인가**
   - PUT 호출 시 해당 folderId의 `FolderMember` 목록에 현재 사용자 포함 여부 확인
   - 미포함 시 `403 Forbidden` 반환

5. **ReportAnalyzeCommand DTO 구조**
   - 기존 단일 `projectId` 유지, `projectIds: List<Long>` 추가 (nullable)
   - 서비스에서 `projectIds != null` 이면 배열 처리, 아니면 기존 단일 처리

6. **FolderSummary 모듈 배치**
   - `report` 모듈 하위에 위치 (`com.hubilon.modules.report.domain.model.FolderSummary`)
   - 폴더 집계 결과이지만 보고서(Report) 범주이므로 report 모듈에 귀속

7. **기간 파라미터 명세**
   - GET: `?folderId=1&startDate=2026-04-01&endDate=2026-04-07`
   - POST: body `{ folderId, startDate, endDate }`
   - PUT: body `{ summary }`

---

## 검증 방법

1. **Total View**: 폴더 클릭 → 해당 폴더 내 프로젝트 카드만 중앙에 표시되는지 확인
2. **FolderReportPanel**: 우측 패널에 폴더명 타이틀 + 통합 통계 표시 확인
3. **AI 요약**: "AI 요약" 버튼 클릭 → 폴더 내 모든 프로젝트 커밋 통합 요약 생성 확인
4. **Individual View**: 사이드바에서 프로젝트 클릭 → 단일 프로젝트 카드만 표시 + 기존 ReportPanel 표시 확인
5. **폴더 해제**: 폴더 외부 영역 클릭 또는 미분류 프로젝트 클릭 → selectedFolderId null 전환 확인

---

## Review 결과
- 검토일: 2026-04-09
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: 고 2건 / 중 5건 이슈 발견 → 위 보완 사항 반영 완료
