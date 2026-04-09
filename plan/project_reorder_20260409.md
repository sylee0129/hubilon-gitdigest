# 프로젝트 순서 변경 기능 구현 계획

## Context

현재 사이드바에 프로젝트 목록이 표시되지만, 사용자가 원하는 순서로 재배열할 수 없다.
프로젝트 엔티티에 정렬 순서 필드가 없고, 드래그앤드롭 라이브러리도 설치되지 않은 상태이다.
사용자가 드래그앤드롭으로 사이드바의 프로젝트 순서를 변경하고, 변경된 순서가 DB에 저장되어 다음 접속 시에도 유지되도록 구현한다.

---

## 구현 범위

### 백엔드

1. **Project 도메인 모델에 `sortOrder` 필드 추가**
   - `backend/src/main/java/com/hubilon/modules/project/domain/model/Project.java`
   - `backend/src/main/java/com/hubilon/modules/project/adapter/out/persistence/ProjectJpaEntity.java`
     - 테이블 컬럼: `sort_order INTEGER` (기본값: 0 또는 현재 등록 순서)

2. **프로젝트 목록 조회 시 `sortOrder` 오름차순 정렬 적용**
   - `ProjectJpaRepository.java` — `findAllByOrderBySortOrderAsc()` 메서드 추가
   - `ProjectPersistenceAdapter.java` — `findAll()` 호출을 정렬 쿼리로 교체

3. **순서 변경 API 추가: `PATCH /api/projects/reorder`**
   - 요청 바디: `{ projectIds: [3, 1, 2] }` (순서대로 나열한 프로젝트 ID 배열)
   - 인덱스 순서대로 각 프로젝트의 `sortOrder` 업데이트 (배치)
   - 새 파일들:
     - `ProjectReorderUseCase.java` (port/in)
     - `ProjectReorderCommand.java` (application/dto)
     - `ProjectReorderService.java` (application/service/command)
   - 기존 파일 수정:
     - `ProjectCommandPort.java` — `updateSortOrders(List<Long> orderedIds)` 추가
     - `ProjectPersistenceAdapter.java` — 배치 업데이트 구현
     - `ProjectController.java` — `PATCH /api/projects/reorder` 엔드포인트 추가
     - `ProjectSearchResponse.java` — `sortOrder` 필드 추가

4. **기존 데이터 마이그레이션**
   - `ProjectJpaEntity`에 `@Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")` 적용
   - H2 DDL auto 사용 중이므로 별도 마이그레이션 스크립트 불필요 (schema 재생성)

---

### 프론트엔드

1. **드래그앤드롭 라이브러리 설치**
   ```
   npm install @dnd-kit/core @dnd-kit/sortable @dnd-kit/utilities
   ```

2. **Project 타입에 `sortOrder` 추가**
   - `frontend/src/types/report.ts`
   ```typescript
   export interface Project {
     id: number
     name: string
     gitlabUrl: string
     authType: 'PAT' | 'OAUTH'
     createdAt: string
     sortOrder: number  // 추가
   }
   ```

3. **프로젝트 API에 `reorder` 메서드 추가**
   - `frontend/src/services/projectApi.ts`
   ```typescript
   reorder: (projectIds: number[]) =>
     axiosInstance.patch('/projects/reorder', { projectIds })
       .then(res => res.data)
   ```

4. **React Query 훅에 `useReorderProjects` 추가**
   - `frontend/src/hooks/useProjects.ts`
   - optimistic update: 로컬 상태를 즉시 업데이트하고, API 성공 시 캐시 무효화
   - 실패 시 이전 순서로 롤백

5. **Sidebar 컴포넌트에 드래그앤드롭 적용**
   - `frontend/src/components/layout/Sidebar.tsx`
   - `@dnd-kit/sortable`의 `SortableContext` + `useSortable` 사용
   - 드래그 핸들 아이콘(⠿) 추가 (프로젝트명 좌측)
   - 드래그 종료(`onDragEnd`) 시 `reorderProjects` 뮤테이션 호출
   - `frontend/src/components/layout/Sidebar.module.css` — 드래그 핸들 스타일 추가

---

## 파일 목록 (수정/생성)

### 백엔드 수정
| 파일 | 변경 내용 |
|------|-----------|
| `domain/model/Project.java` | `sortOrder` 필드 추가 |
| `adapter/out/persistence/ProjectJpaEntity.java` | `sort_order` 컬럼 추가 |
| `adapter/out/persistence/ProjectJpaRepository.java` | `findAllByOrderBySortOrderAsc()` 추가 |
| `adapter/out/persistence/ProjectPersistenceAdapter.java` | 정렬 조회 + 배치 업데이트 구현 |
| `domain/port/out/ProjectCommandPort.java` | `updateSortOrders()` 추가 |
| `domain/port/in/ProjectReorderUseCase.java` | 신규 생성 |
| `application/dto/ProjectReorderCommand.java` | 신규 생성 |
| `application/service/command/ProjectReorderService.java` | 신규 생성 |
| `adapter/in/web/ProjectController.java` | `PATCH /reorder` 엔드포인트 추가 |
| `adapter/in/web/ProjectSearchResponse.java` | `sortOrder` 필드 추가 |

### 프론트엔드 수정
| 파일 | 변경 내용 |
|------|-----------|
| `package.json` | `@dnd-kit/*` 의존성 추가 |
| `src/types/report.ts` | `Project.sortOrder` 필드 추가 |
| `src/services/projectApi.ts` | `reorder()` 메서드 추가 |
| `src/hooks/useProjects.ts` | `useReorderProjects()` 훅 추가 |
| `src/components/layout/Sidebar.tsx` | 드래그앤드롭 적용 |
| `src/components/layout/Sidebar.module.css` | 드래그 핸들 스타일 추가 |

---

## 검증

1. 백엔드 서버 기동 후 기존 프로젝트 목록 `GET /api/projects` 응답에 `sortOrder` 포함 확인
2. 프론트엔드 개발 서버 기동 후 사이드바에서 프로젝트 항목 드래그앤드롭 가능 확인
3. 순서 변경 후 페이지 새로고침해도 변경된 순서 유지 확인
4. 네트워크 탭에서 `PATCH /api/projects/reorder` 요청 확인
