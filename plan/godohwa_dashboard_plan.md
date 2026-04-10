# Godohwa 메인 진입 화면 고도화 계획

## Context

현재 로그인 후 초기 진입 시 폴더/프로젝트를 선택하지 않으면 빈 화면이 표시됨.
사용자 경험 개선을 위해 초기 진입 시 전체 사업 현황 대시보드를 보여주고, 선택 후에만 상세 데이터를 노출하도록 변경.

---

## 화면 분기 구조

```
ReportDashboard
├── selectedFolder == null && selectedProject == null
│   └── [A] 대시보드 모드 (DashboardView)
└── selectedFolder != null || selectedProject != null
    └── [B] 상세 내역 모드 (기존 동작)
```

---

## 구현 범위

### Backend

#### 1. 대시보드 통계 API 신규 개발

**엔드포인트**: `GET /api/dashboard/summary`

**응답 DTO**:
```java
record DashboardSummaryResponse(
    int totalFolderCount,           // 총 관리 사업(폴더) 수
    int inProgressFolderCount,      // 진행중 사업 수
    int todayCommitCount,           // 금일 전체 커밋 수
    int weeklyCommitCount,          // 이번 주 전체 커밋 수
    List<RecentActiveFolderItem> recentActiveFolders  // 최근 24시간 활성 폴더 TOP3
)

record RecentActiveFolderItem(
    Long folderId,
    String folderName,
    int commitCount,
    LocalDateTime lastCommittedAt
)
```

**Service 구현**:
- `DashboardQueryService` (신규)
  - `FolderQueryPort` 재사용: 전체/진행중 폴더 수 집계
  - `CommitInfoJpaRepository` 신규 추가: `committedAt` 기반 최근 24시간 커밋 집계 쿼리
  - 최근 24시간 활성 폴더 집계 쿼리 (commit_infos → reports → projects → folders JOIN)

**신규 파일**:
- `backend/src/main/.../modules/dashboard/`
  - `DashboardController.java`
  - `DashboardQueryService.java`
  - `DashboardSummaryResponse.java`
  - `RecentActiveFolderItem.java`
- `backend/src/main/.../infrastructure/persistence/commit/CommitInfoJpaRepository.java`

**집계 쿼리 구현 방식**:

> **[이슈 수정]** JPQL `LIMIT` 미지원 + `Object[]` 타입 안전성 문제 해결.
> `DashboardQueryAdapter`를 신규 생성하여 JPAQueryFactory + QueryDSL Projections 사용.

```java
// DashboardQueryAdapter.java
@Repository
public class DashboardQueryAdapter {
    private final JPAQueryFactory queryFactory;

    public List<ActiveFolderProjection> findTop3ActiveFoldersSince(LocalDateTime since) {
        return queryFactory
            .select(Projections.constructor(ActiveFolderProjection.class,
                project.folderId,
                folder.name,
                commitInfo.id.count().intValue(),
                commitInfo.committedAt.max()
            ))
            .from(commitInfo)
            .join(commitInfo.report, report)
            .join(project).on(project.id.eq(report.projectId))
            .join(folder).on(folder.id.eq(project.folderId))
            .where(
                commitInfo.committedAt.goe(since),
                project.folderId.isNotNull()  // 미분류 프로젝트 제외
            )
            .groupBy(project.folderId, folder.name)
            .orderBy(commitInfo.id.count().desc())
            .limit(3)
            .fetch();
    }
}
```

> **[설계 결정]** 대시보드 통계는 전체 현황 조회 목적이므로 멤버십 필터 없이 전체 집계. (사용자 요청에 의해 멀티테넌시 필터 적용 생략)

**todayCommitCount / weeklyCommitCount 기준 시각**:

> **[이슈 수정]** 모든 날짜 계산은 `Asia/Seoul` 기준으로 통일.
> - `todayCommitCount`: `LocalDate.now(ZoneId.of("Asia/Seoul"))` 의 00:00:00 ~ 23:59:59
> - `weeklyCommitCount`: 이번 주 월요일 00:00:00 ~ 일요일 23:59:59 (KST 기준)

```yaml
# application.yml 추가
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: Asia/Seoul
```

**lastCommittedAt 직렬화**:

> `committedAt`은 `LocalDateTime` (기존 시스템 전체 서버 로컬 시간 기준 통일).
> Jackson ISO 8601 형식으로 명시:

```java
record RecentActiveFolderItem(
    Long folderId,
    String folderName,
    int commitCount,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime lastCommittedAt
)
```

**`inProgressFolderCount` 집계 기준**:

> `FolderStatus.IN_PROGRESS` 상태인 폴더 수 (`FolderQueryPort.findAllWithDetailsByStatus(IN_PROGRESS)` 재사용).
```

---

### Frontend

#### 1. DashboardView 컴포넌트 신규 개발

**파일**: `frontend/src/components/dashboard/DashboardView.tsx`
**스타일**: `frontend/src/components/dashboard/DashboardView.module.css`

**구성 요소**:

| 섹션 | 내용 |
|------|------|
| 통계 배지 | 총 사업 수, 진행중 사업 수, 금일 커밋 수 (큰 폰트 + Badge) |
| 최근 업데이트 사업 | 최근 24시간 활성 폴더 TOP3 리스트 (클릭 → setSelectedFolder 연동) |
| 가이드 섹션 | "분석할 사업 폴더나 개별 프로젝트를 왼쪽 사이드바에서 선택..." 안내 텍스트 |
| 스켈레톤 UI | 데이터 로딩 중 표시 |

**Props**:
```typescript
interface DashboardViewProps {
  onFolderSelect: (folderId: number) => void  // 최근 활성 폴더 클릭 핸들러
}
```

**에러/로딩 상태 처리**:

> **[이슈 수정 #4]** API 실패 시 에러 UI 명시:

```
로딩 중  → 스켈레톤 UI (각 섹션별 shimmer 효과)
에러 발생 → 에러 메시지 + "새로고침" 버튼 (queryClient.invalidateQueries)
데이터 없음 → 빈 상태 메시지 ("등록된 사업이 없습니다")
```

**폴더 클릭 → View Mode 전환**:

> **[이슈 수정 #6]** `onFolderSelect(folderId)` 호출 시 `useReportStore.setSelectedFolder(folderId)` 실행.
> 기존 스토어 로직상 `setSelectedFolder` 호출 시 `activeTab = 'all'`로 자동 전환, `selectedProjectId = null`로 초기화됨.
> → ReportDashboard가 즉시 폴더 상세 뷰(FolderReportPanel + 프로젝트 카드 목록)로 전환됨.

#### 2. API 훅 신규 개발

**파일**: `frontend/src/hooks/useDashboard.ts`

```typescript
export function useDashboardSummary() {
  return useQuery({
    queryKey: ['dashboard', 'summary'],
    queryFn: () => dashboardApi.getSummary(),
    staleTime: 1000 * 60 * 5,  // 5분 캐시
  })
}
```

**파일**: `frontend/src/services/dashboardApi.ts`

```typescript
export const dashboardApi = {
  getSummary: () => axiosInstance.get<DashboardSummaryResponse>('/dashboard/summary')
}
```

#### 3. ReportDashboard.tsx 조건부 렌더링 추가

**파일**: `frontend/src/pages/ReportDashboard.tsx` (기존 수정)

```typescript
// 추가할 조건부 렌더링 로직
const { selectedFolderId, selectedProjectId, setSelectedFolder } = useReportStore()
const showDashboard = !selectedFolderId && !selectedProjectId

// 중앙 섹션 렌더링
{showDashboard
  ? <DashboardView onFolderSelect={setSelectedFolder} />
  : <기존 ReportCard 목록 렌더링 />
}
```

#### 4. 우측 보고서 영역 가이드 텍스트

**파일**: `frontend/src/components/report/ReportPanel.tsx` 또는 `FolderReportPanel.tsx` (기존 수정)

- `showDashboard` 상태일 때 우측 패널에 안내 문구 표시:
  "사업을 선택하면 AI 요약 보고서가 여기에 생성됩니다."

---

## 기존 재사용 요소

| 항목 | 파일 | 용도 |
|------|------|------|
| `useReportStore` | `frontend/src/stores/useReportStore.ts` | selectedFolderId, setSelectedFolder 재사용 |
| `useFolders` | `frontend/src/hooks/useFolders.ts` | 폴더 수 집계 (IN_PROGRESS 필터) 재사용 |
| `FolderQueryPort` | backend port | 폴더 조회 재사용 |
| `axiosInstance` | `frontend/src/services/axios.ts` | API 호출 재사용 |

---

## 구현 순서

1. **Backend**: `DashboardController` + `DashboardQueryService` + `CommitInfoJpaRepository` 집계 쿼리
2. **Frontend**: `dashboardApi.ts` + `useDashboard.ts`
3. **Frontend**: `DashboardView.tsx` + `DashboardView.module.css`
4. **Frontend**: `ReportDashboard.tsx` 조건부 렌더링 추가
5. **Frontend**: 우측 패널 가이드 텍스트 처리

---

## Review 결과
- 검토일: 2026-04-09
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: HIGH 2건, MEDIUM 3건, LOW 1건 → 전항목 수정 반영 완료

---

## 검증 방법

1. 로그인 후 아무것도 선택하지 않은 상태 → DashboardView 렌더링 확인
2. 통계 수치가 실제 DB 데이터와 일치하는지 확인
3. 최근 활성 폴더 클릭 → 해당 폴더의 상세 보고서로 전환 확인
4. 사이드바에서 폴더/프로젝트 선택 → 기존 상세 모드로 전환 확인
5. 로딩 중 스켈레톤 UI 노출 확인
6. 우측 패널 초기 상태 안내 문구 노출 확인
