# 프로젝트 상태 관리 기능 계획

## 요구사항 요약

- 프로젝트에 상태(시작전 / 진행중 / 중단됨) 추가
- 사이드바 프로젝트 항목에서 **토글 클릭**으로 상태 순환 변경
- 사이드바에서 **드래그 앤 드롭**으로 순서 변경
- **전체 프로젝트 탭**에 상태 필터 추가 (기본값: 진행중)

---

## 설계 결정

### 상태 Enum (3단계 순환 토글)
```
시작전 (BEFORE_START) → 진행중 (IN_PROGRESS) → 중단됨 (STOPPED) → 시작전 ...
```
- 사이드바 프로젝트 항목 좌측 컬러 뱃지 클릭 시 상태 토글
- 각 상태별 색상: 시작전(회색), 진행중(초록), 중단됨(빨강)

### 순서 관리
- DB에 `sort_order` 컬럼 추가 (INT, nullable — NULL이면 등록순)
- 드래그 완료(mouseup) 시점에 변경된 순서를 일괄 API 전송
- 낙관적 UI 업데이트 적용 (drag 중 즉시 반영, 실패 시 rollback)

### 전체 프로젝트 탭 필터
- 필터 버튼: `전체` / `시작전` / `진행중` / `중단됨` (기본: 진행중)
- 백엔드 API에 `status` 쿼리 파라미터 추가 (없으면 전체 반환)
- 프론트 `useReportStore`에 `projectStatusFilter` 상태 추가

---

## 백엔드 변경 범위

### 1. DB 스키마 변경 (Flyway/DDL)
```sql
ALTER TABLE projects
  ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
  ADD COLUMN sort_order INT;
```

### 2. `ProjectJpaEntity` 변경
```java
@Comment("프로젝트 상태")
@Column(nullable = false)
@Enumerated(EnumType.STRING)
private ProjectStatus status;   // 기본값: IN_PROGRESS

@Comment("표시 순서")
private Integer sortOrder;
```

### 3. `Project` 도메인 모델 변경
- `status: ProjectStatus` 필드 추가
- `sortOrder: Integer` 필드 추가
- `ProjectStatus` enum: `BEFORE_START`, `IN_PROGRESS`, `STOPPED`

### 4. 신규 UseCase / Service 추가

#### `ProjectStatusUpdateUseCase` (상태 변경)
```java
void updateStatus(Long id, ProjectStatusUpdateCommand command);
```

#### `ProjectSortOrderUpdateUseCase` (순서 변경)
```java
void updateSortOrder(List<ProjectSortOrderItem> items);
```

### 5. Controller 신규 엔드포인트
| Method | Path | 설명 |
|--------|------|------|
| PATCH | `/api/projects/{id}/status` | 상태 변경 |
| PATCH | `/api/projects/sort-order` | 순서 일괄 변경 |

### 6. `ProjectSearchService` 변경
- `status` 파라미터로 필터링 지원 (null이면 전체)
- `sort_order ASC, created_at ASC` 정렬

---

## 프론트엔드 변경 범위

### 1. 타입 정의 (`types/report.ts` 또는 `types/project.ts`)
```typescript
export type ProjectStatus = 'BEFORE_START' | 'IN_PROGRESS' | 'STOPPED'

export interface Project {
  id: number
  name: string
  status: ProjectStatus
  sortOrder: number | null
  // 기존 필드...
}
```

### 2. API 서비스 추가 (`services/projectApi.ts`)
```typescript
updateStatus: (id: number, status: ProjectStatus) => ...
updateSortOrder: (items: { id: number; sortOrder: number }[]) => ...
```

### 3. 훅 추가 (`hooks/useProjects.ts`)
```typescript
useUpdateProjectStatus()   // PATCH /projects/{id}/status
useUpdateProjectSortOrder() // PATCH /projects/sort-order
```

### 4. `Sidebar.tsx` 변경
- 프로젝트 항목에 상태 뱃지(컬러 원) 추가 — 클릭 시 상태 토글
- `@dnd-kit/core` 또는 순수 HTML5 드래그 이벤트로 순서 변경 구현
  - `@dnd-kit`이 없으면 `onDragStart` / `onDragOver` / `onDrop` 사용
- 드래그 완료 시 `useUpdateProjectSortOrder` 호출

### 5. `useReportStore.ts` 변경
```typescript
projectStatusFilter: ProjectStatus | 'ALL'   // 기본값: 'IN_PROGRESS'
setProjectStatusFilter: (filter) => void
```

### 6. `ReportDashboard.tsx` 변경 (`전체 프로젝트` 탭)
- 상태 필터 버튼 영역 추가 (전체 / 시작전 / 진행중 / 중단됨)
- `useReports` 훅에 `projectStatus` 파라미터 전달

---

## 구현 순서

### 백엔드 (1~5 순서대로)
1. DB 컬럼 추가 (DDL or Flyway migration)
2. `ProjectStatus` enum, `ProjectJpaEntity` 수정
3. `Project` 도메인 모델 수정, 기존 Mapper 갱신
4. `ProjectStatusUpdateService` + `ProjectSortOrderUpdateService` 구현
5. `ProjectController` 엔드포인트 추가, `ProjectSearchService` 필터 지원

### 프론트엔드 (백엔드 완료 후)
1. 타입 정의 갱신
2. API 서비스 / 훅 추가
3. `Sidebar.tsx` 상태 뱃지 + 드래그 구현
4. `useReportStore` 필터 상태 추가
5. `ReportDashboard.tsx` 필터 UI 추가

---

## 의존성 라이브러리

### 프론트엔드 드래그 구현 선택지
| 옵션 | 장점 | 단점 |
|------|------|------|
| `@dnd-kit/core` (권장) | 접근성, 터치 지원, 가상화 호환 | 패키지 추가 필요 |
| 순수 HTML5 Drag API | 추가 의존성 없음 | 모바일 미지원, 커스터마이징 복잡 |

→ **`@dnd-kit/core` + `@dnd-kit/sortable` 사용** (각 ~15kb)

---

## API 스펙 (최종)

### PATCH `/api/projects/{id}/status`
```json
Request: { "status": "IN_PROGRESS" }
Response: { "success": true, "data": null, "message": null }
```

### PATCH `/api/projects/sort-order`
```json
Request: [{ "id": 1, "sortOrder": 0 }, { "id": 3, "sortOrder": 1 }]
Response: { "success": true, "data": null, "message": null }
```

### GET `/api/projects` (기존 변경)
```
Query: ?status=IN_PROGRESS  (없으면 전체)
Response: 기존 응답 + status, sortOrder 필드 추가
```

### GET `/api/reports` (기존 변경)
```
Query: ?projectStatus=IN_PROGRESS  (전체 프로젝트 탭 필터용)
```
