# Team Folder Setting (팀 기반 폴더 필터링) Plan

## 목표
로그인한 사용자의 `teamId`를 기반으로 사이드바 Folders 영역에 해당 팀의 폴더만 표시한다.

---

## 현황 분석

### 백엔드
- `FolderJpaEntity`: `team_id` 컬럼 없음 → 추가 필요
- `FolderController.searchAll()`: `GET /api/folders?status=` 만 지원
- `UserJpaEntity`: `team` (`TeamJpaEntity`) ManyToOne 관계 이미 존재
- JWT: `email`만 클레임에 포함됨
- `LoginService.login()`: `TokenPair`만 반환, User 정보 미포함

### 프론트엔드
- `useAuthStore`: `user.teamId` 필드 이미 정의되어 있음
- `useFolders(status?)`: teamId 파라미터 없음
- `folderApi.getAll()`: `status` 파라미터만 전송

---

## 보안 설계 원칙

- **클라이언트는 teamId를 파라미터로 전송하지 않는다.**
- 서버는 JWT → SecurityContext → email → User 조회로 teamId를 서버에서 자동 추출한다.
- `role == ADMIN` → 전체 폴더 반환
- `role == USER && teamId != null` → 해당 팀 폴더만 반환
- `role == USER && teamId == null` → 빈 목록 반환

---

## 작업 범위

### BE-1: `folders` 테이블에 `team_id` 컬럼 추가

**파일:** `FolderJpaEntity.java`
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "team_id")
@Comment("소속 팀")
private TeamJpaEntity team;
```
- `FolderJpaEntity.@Builder`에 `team` 파라미터 추가
- `updateTeam(TeamJpaEntity team)` 도메인 메서드 추가

**파일:** `Folder.java` (도메인 모델)
```java
private Long teamId;
```

---

### BE-2: 폴더 생성/수정 시 `teamId` 지정 지원

**파일:** `FolderCreateRequest.java`, `FolderUpdateRequest.java`
- `teamId` 필드 추가 (nullable)

**파일:** `FolderCreateCommand.java`, `FolderUpdateCommand.java`
- `teamId` 필드 추가

**파일:** `FolderCreateService.java`, `FolderUpdateService.java`
- `teamId`로 `TeamJpaEntity` 조회 후 `FolderJpaEntity`에 설정

---

### BE-3: SecurityUtils — 현재 사용자 추출 공통 유틸

**파일:** `common/security/SecurityUtils.java` (신규)
```java
@Component
@RequiredArgsConstructor
public class SecurityUtils {
    private final UserQueryPort userQueryPort;

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
            .getAuthentication().getName();
        return userQueryPort.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }
}
```
- 이후 여러 컨트롤러에서 재사용 가능한 단일 추상화

---

### BE-4: 팀별 폴더 조회 — 서버사이드 필터링

**파일:** `FolderJpaRepository.java`
```java
@Query("SELECT DISTINCT f FROM FolderJpaEntity f LEFT JOIN FETCH f.category LEFT JOIN FETCH f.members LEFT JOIN FETCH f.workProjects WHERE f.team.id = :teamId ORDER BY f.sortOrder ASC")
List<FolderJpaEntity> findAllWithDetailsByTeamId(@Param("teamId") Long teamId);

@Query("SELECT DISTINCT f FROM FolderJpaEntity f LEFT JOIN FETCH f.category LEFT JOIN FETCH f.members LEFT JOIN FETCH f.workProjects WHERE f.team.id = :teamId AND f.status = :status ORDER BY f.sortOrder ASC")
List<FolderJpaEntity> findAllWithDetailsByTeamIdAndStatus(@Param("teamId") Long teamId, @Param("status") FolderStatus status);
```

**파일:** `FolderQueryPort.java`
```java
List<FolderResult> findAllWithDetails(FolderStatus status, Long teamId);
```
- `teamId == null` (ADMIN) → 전체 조회
- `teamId != null` → 팀 필터 쿼리

**파일:** `FolderQueryUseCase.java`
```java
List<FolderResult> searchAll(FolderStatus status, Long teamId);
```

**파일:** `FolderQueryService.java`
- `searchAll(FolderStatus status, Long teamId)` 구현

**파일:** `FolderController.java`
```java
@GetMapping
public Response<List<FolderResponse>> searchAll(
    @RequestParam(required = false) FolderStatus status,
    Authentication authentication  // SecurityContext에서 주입
) {
    User currentUser = securityUtils.getCurrentUser();
    Long teamId = currentUser.getRole() == User.Role.ADMIN ? null : currentUser.getTeamId();
    return Response.ok(
        folderQueryUseCase.searchAll(status, teamId).stream()
            .map(folderWebMapper::toResponse)
            .toList()
    );
}
```
- 클라이언트 파라미터 teamId 제거
- 서버가 SecurityContext에서 teamId 자동 결정

---

### BE-5: 로그인 응답에 User 정보 포함

**파일:** `LoginResult.java` (신규 record, `auth/application/dto/`)
```java
public record LoginResult(
    String accessToken,
    String refreshToken,
    long expiresIn,
    Long userId,
    String name,
    String email,
    Long teamId,
    String teamName,
    String role
) {}
```

**파일:** `LoginUseCase.java` → 반환 타입을 `LoginResult`로 변경
**파일:** `LoginService.java` → `User` 정보를 `LoginResult`에 포함하여 반환
**파일:** Auth Controller 로그인 엔드포인트 → `LoginResult` 반환

---

### FE-1: 로그인 응답에서 teamId 저장

**파일:** `frontend/src/services/authApi.ts`
- 로그인 응답에서 `userId`, `name`, `email`, `teamId`, `teamName`, `role` 파싱
- `useAuthStore`의 `user`에 저장

**파일:** `frontend/src/stores/useAuthStore.ts`
- `user.teamId` 필드 이미 존재 → 별도 수정 불필요 (저장 로직만 확인)

---

### FE-2: `Sidebar`에서 teamId 기반 폴더 조회 (파라미터 제거)

**기존:** `useFolders()` → `GET /api/folders`  
**변경:** 그대로 `useFolders()` 유지 — 서버가 SecurityContext에서 teamId 결정

- 프론트엔드에서 teamId를 파라미터로 보내지 않음
- `useFolders` hook, `folderApi.getAll()` 시그니처 변경 없음

---

## 작업 순서

```
BE-1 → BE-2 → BE-3 → BE-4 → BE-5  (순서대로)
FE-1 → FE-2                         (BE-5 완료 후)
```

---

## DB Migration

```sql
ALTER TABLE folders ADD COLUMN team_id BIGINT REFERENCES teams(id);
-- 기존 데이터: team_id = NULL → ADMIN에서만 보임
-- 필요 시 운영 데이터 팀 매핑 UPDATE 스크립트 별도 실행
```

---

## 고려사항

| 항목 | 결정 |
|------|------|
| ADMIN 사용자 | SecurityContext role 체크 → `teamId = null` 전달 → 전체 폴더 반환 |
| USER && teamId = null | 빈 목록 반환 |
| 파라미터 보안 | 클라이언트 teamId 파라미터 없음 — 서버 추출만 허용 |
| 공통 추상화 | `SecurityUtils.getCurrentUser()` — 모든 컨트롤러 재사용 |
| 기존 폴더 데이터 | `team_id = NULL` → ADMIN 전용. 마이그레이션 스크립트 별도 실행 필요 |

---

## Review 결과
- 검토일: 2026-04-20
- 검토 항목: 보안 / 리팩토링 / 기능
- 수정 사항:
  - teamId 클라이언트 파라미터 → SecurityContext 서버 추출로 변경
  - null 사용자 → 빈 목록 반환
  - ADMIN role 체크 서버사이드 처리
  - SecurityUtils 공통 추상화 추가
