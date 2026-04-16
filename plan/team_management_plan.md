# 팀 관리 테이블 추가 및 User 매핑

## Context
현재 `users.department`는 자유 문자열. DataInitializer가 NULL 값을 '플랫폼개발팀'으로 패치하는 임시방편 사용 중.
팀을 별도 테이블로 관리하고 users가 FK로 참조하도록 변경. 팀 관리는 조회(GET)만 지원.

---

## Backend

### 1. Team 도메인 모델 신규 생성
- `backend/src/main/java/com/hubilon/modules/team/domain/model/Team.java`
  ```java
  public class Team {
      private Long id;
      private String name;
  }
  ```

### 2. TeamJpaEntity 생성
- `backend/src/main/java/com/hubilon/modules/team/adapter/out/persistence/TeamJpaEntity.java`
  - `@Table(name = "teams")`
  - 필드: `id`, `name (unique, not null)`, `createdAt`, `updatedAt`

### 3. TeamQueryPort 생성 (Port 계층)
- `backend/src/main/java/com/hubilon/modules/team/application/port/out/TeamQueryPort.java`
  ```java
  public interface TeamQueryPort {
      Optional<Team> findByName(String name);
      List<Team> findAll();
  }
  ```

### 3-1. TeamRepository 생성 (Adapter 구현)
- `backend/src/main/java/com/hubilon/modules/team/adapter/out/persistence/TeamRepository.java`
  - `findByName(String name): Optional<TeamJpaEntity>`
- `backend/src/main/java/com/hubilon/modules/team/adapter/out/persistence/TeamPersistenceAdapter.java`
  - `TeamQueryPort` 구현체

### 4. TeamController 생성
- `backend/src/main/java/com/hubilon/modules/team/adapter/in/web/TeamController.java`
  - `GET /api/teams` → `List<TeamResponse>` (id, name)

### 5. UserJpaEntity 수정
- `backend/src/main/java/com/hubilon/modules/user/adapter/out/persistence/UserJpaEntity.java`
  - `String department` → `@ManyToOne(fetch = LAZY) @JoinColumn(name = "team_id") TeamJpaEntity team`

### 6. User 도메인 모델 수정
- `backend/src/main/java/com/hubilon/modules/user/domain/model/User.java`
  - `String department` → `Long teamId` + `String teamName`

### 7. UserPersistenceAdapter 수정
- `backend/src/main/java/com/hubilon/modules/user/adapter/out/persistence/UserPersistenceAdapter.java`
  - toDomain(): `team.getId()`, `team.getName()` → User 매핑
  - save(): teamId로 TeamJpaEntity 조회 후 FK 설정

### 8. DTO 수정
| 파일 | 변경 |
|------|------|
| `UserRegisterRequest.java` | `String department` → `Long teamId` |
| `UserRegisterCommand.java` | `String department` → `Long teamId` |
| `UserRegisterResult.java` | `String department` → `Long teamId`, `String teamName` |
| `UserSearchResult.java` | `String department` → `Long teamId`, `String teamName` |

### 9. UserRegisterService 수정
- `backend/src/main/java/com/hubilon/modules/user/application/service/UserRegisterService.java`
  - TeamQueryPort 주입하여 teamId로 팀 조회 후 User 생성

### 10. DataInitializer 수정
- `backend/src/main/java/com/hubilon/common/init/DataInitializer.java`
  - 시작 시 '플랫폼개발팀' 팀이 없으면 생성
  - 초기 사용자 생성 시 team FK 할당
  - 기존 NULL department UPDATE 쿼리 → team_id 업데이트로 변경

---

## Frontend

### 11. teamApi 신규 생성
- `frontend/src/services/teamApi.ts`
  - `getTeams(): Promise<Team[]>` → GET /api/teams

### 12. SignupPage 수정
- `frontend/src/pages/SignupPage.tsx`
  - `<input type="text" placeholder="예: 플랫폼개발팀">` → `<select>` (팀 목록 API)
  - `department: string` → `teamId: number`

### 13. authApi / userApi 수정
- `frontend/src/services/authApi.ts`: `department: string` → `teamId: number`
- `frontend/src/services/userApi.ts`: 동일

### 14. 타입 수정
- `frontend/src/types/folder.ts`: `department?: string` → `teamId?: number`, `teamName?: string`
- `frontend/src/stores/useAuthStore.ts`: `department: string | null` → `teamName: string | null`

---

## DB 마이그레이션 전략
- Hibernate `ddl-auto: update` 사용 → `teams` 테이블, `users.team_id` 컬럼 자동 생성
- 기존 `users.department` 컬럼은 엔티티에서 제거해도 Hibernate가 자동 drop하지 않음 (무시)
- DataInitializer가 시작 시 팀 생성 + 기존 team_id 없는 users 업데이트

---

## 검증
1. 앱 시작 → `teams` 테이블에 '플랫폼개발팀' 행 생성 확인
2. `users.team_id` → 모든 기존 사용자 FK 참조 확인
3. `GET /api/teams` → `[{"id":1,"name":"플랫폼개발팀"}]` 반환 확인
4. 회원가입 페이지 → 팀 드롭다운 표시, 팀 선택 후 가입 정상 처리 확인
5. FolderModal 사용자 검색 → teamName 표시 확인

---

## Review 결과
- 검토일: 2026-04-15
- 검토 항목: 보안 / 리팩토링 / 기능
- 이슈 반영: TeamQueryPort 계층 추가 (Hexagonal Pattern 준수)
