# Team Project Setting (팀 기반 프로젝트 필터링) Plan

## 목표
`projects` 테이블의 `folder_id = NULL`인 미분류 프로젝트도 팀별로 격리한다.
로그인한 사용자의 `teamId` 기반으로 해당 팀 프로젝트만 사이드바 Projects 섹션에 표시.

---

## 현황 분석

### 백엔드
- `ProjectJpaEntity`: `team_id` 컬럼 없음
- `Project` 도메인 모델: `teamId` 필드 없음
- `GET /api/projects`: 파라미터 없이 전체 반환
- `ProjectPersistenceAdapter.findAll()`: 전체 조회만 지원
- `SecurityUtils`: 이미 `common/security/SecurityUtils.java` 존재 → 재사용

### 프론트엔드
- `useProjects` queryKey: `['projects']` (teamId 미포함)
- `projectApi.getAll()`: 파라미터 없이 `GET /api/projects` 호출
- `Sidebar.tsx`: `projects.filter((p) => !p.folderId)` 로 미분류 렌더링

---

## 보안 설계 원칙 (folders와 동일)
- 클라이언트는 teamId를 파라미터로 전송하지 않음
- 서버가 SecurityContext → email → User → teamId 자동 추출
- `teamId == null` → 빈 목록 반환
- ADMIN/USER 구분 없이 모두 자신의 teamId로 필터링

---

## 작업 범위

### BE-1: `ProjectJpaEntity`에 `team_id` 컬럼 추가
**파일**: `backend/src/main/java/com/hubilon/modules/project/adapter/out/persistence/ProjectJpaEntity.java`
```java
@Column(name = "team_id")
@Comment("소속 팀")
private Long teamId;
```
- `@Builder` 생성자에 `teamId` 파라미터 추가

---

### BE-2: `Project` 도메인 모델에 `teamId` 추가
**파일**: `backend/src/main/java/com/hubilon/modules/project/domain/model/Project.java`
```java
private Long teamId;
```

---

### BE-3: `ProjectJpaRepository`에 팀 기반 쿼리 추가
**파일**: `backend/src/main/java/com/hubilon/modules/project/adapter/out/persistence/ProjectJpaRepository.java`
```java
List<ProjectJpaEntity> findAllByTeamIdOrderBySortOrderAsc(Long teamId);
```

---

### BE-4: `ProjectQueryPort` 시그니처 변경
**파일**: `backend/src/main/java/com/hubilon/modules/project/domain/port/out/ProjectCommandPort.java` (또는 별도 QueryPort)
```java
List<Project> findAll(Long teamId);
```
- `teamId != null` → 팀 필터
- `teamId == null` → 전체 조회 (현재 구조 보존)

---

### BE-5: `ProjectPersistenceAdapter.findAll()` 구현 변경
**파일**: `backend/src/main/java/com/hubilon/modules/project/adapter/out/persistence/ProjectPersistenceAdapter.java`
- `teamId != null` → `findAllByTeamIdOrderBySortOrderAsc(teamId)`
- `teamId == null` → 기존 `findAllByOrderBySortOrderAsc()`
- `toDomain()`에 `entity.getTeamId()` 매핑 추가

---

### BE-6: `ProjectSearchUseCase` / `ProjectSearchService` 시그니처 변경
**파일**: `backend/src/main/java/com/hubilon/modules/project/domain/port/in/ProjectSearchUseCase.java`
**파일**: `backend/src/main/java/com/hubilon/modules/project/application/service/query/ProjectSearchService.java`
```java
List<ProjectResult> searchAll(Long teamId);
```

---

### BE-7: `ProjectController.searchAll()` — SecurityUtils 적용
**파일**: `backend/src/main/java/com/hubilon/modules/project/adapter/in/web/ProjectController.java`
- `SecurityUtils` 주입 (재사용)
- ADMIN/USER 모두 `currentUser.getTeamId()` 사용
- `teamId == null` → 빈 목록 반환
```java
@GetMapping
public Response<List<ProjectResponse>> searchAll() {
    User currentUser = securityUtils.getCurrentUser();
    Long teamId = currentUser.getTeamId();
    if (teamId == null) return Response.ok(List.of());
    return Response.ok(
        projectSearchUseCase.searchAll(teamId).stream()
            .map(projectWebMapper::toResponse)
            .toList()
    );
}
```

---

### BE-8: 프로젝트 등록 시 `teamId` 자동 설정
**파일**: `backend/src/main/java/com/hubilon/modules/project/adapter/in/web/ProjectController.java` (register 메서드)
**파일**: `backend/src/main/java/com/hubilon/modules/project/application/dto/ProjectRegisterCommand.java`
**파일**: `backend/src/main/java/com/hubilon/modules/project/application/service/command/ProjectRegisterService.java`
- Controller register 시 SecurityContext에서 `teamId` 추출 → Command에 포함
- Service에서 `ProjectJpaEntity` 생성 시 `teamId` 설정

---

### FE: `useProjects` hook — queryKey에 teamId 추가
**파일**: `frontend/src/hooks/useProjects.ts`
```ts
export function useProjects() {
  const teamId = useAuthStore((s) => s.user?.teamId)
  return useQuery({
    queryKey: ['projects', teamId],
    queryFn: projectApi.getAll,
    enabled: teamId != null,
  })
}
```
- `teamId`가 바뀌면 자동 refetch
- `teamId == null`이면 쿼리 비활성화 (folders와 동일 패턴)

---

## 작업 순서
```
BE-1 → BE-2 → BE-3 → BE-4 → BE-5 → BE-6 → BE-7 → BE-8  (순서대로)
FE: BE-7 완료 후 진행
```

---

## DB Migration
```sql
ALTER TABLE projects ADD COLUMN team_id BIGINT REFERENCES teams(id);
-- 기존 데이터: team_id = NULL → 조회 안 됨 (빈 목록)
-- 필요 시 운영 데이터 팀 매핑 UPDATE 스크립트 별도 실행
```

---

## 고려사항

| 항목 | 결정 |
|------|------|
| teamId = null 사용자 | 빈 목록 반환 (folders와 동일) |
| ADMIN 처리 | 자신의 teamId로 필터링 (folders와 동일) |
| 클라이언트 파라미터 | 없음 — 서버 SecurityContext 추출만 허용 |
| 기존 projects 데이터 | team_id = NULL → 보이지 않음. 마이그레이션 스크립트 별도 실행 필요 |
| queryKey | teamId 포함으로 사용자 전환 시 자동 refetch |

---

## Review 결과 및 이슈 반영

- 검토일: 2026-04-20
- 검토 항목: 보안 / 리팩토링 / 기능

### 보안 이슈 반영
- **BE-5 수정**: Adapter에서 `teamId == null → findAll()` 폴백 제거. null 가드는 Controller(BE-7) 단일 지점에서만 처리.
- **BE-8 명시**: 등록 시 요청 바디의 teamId 사용 금지. 반드시 `securityUtils.getCurrentUser().getTeamId()` 강제 사용.

### 리팩토링 이슈 반영
- **BE-6**: Folder 모듈의 `findAll(Long teamId)` 패턴과 동일하게 구현하여 일관성 유지.
- **null 체크 단일화**: Controller에서만 `teamId == null` 체크 후 빈 목록 반환. Adapter는 항상 teamId가 있다고 가정.

### 기능 이슈 반영
- **단건 CRUD 팀 격리 추가 (BE-9)**: `GET /projects/{id}`, `PUT /projects/{id}`, `DELETE /projects/{id}` 에서도 해당 프로젝트의 teamId가 현재 사용자의 teamId와 일치하는지 검증. 불일치 시 403 반환.
- **DB Migration 전략**: `team_id` 컬럼은 NULL 허용으로 추가. 기존 데이터는 NULL → 조회 안 됨(의도된 동작). 운영 데이터 팀 매핑은 별도 UPDATE 스크립트로 실행.
- **FE 상태 구분**: `teamId == null`(팀 미배정)과 로딩 중 상태를 UI에서 구분 처리. 팀 미배정 시 안내 메시지 표시.
