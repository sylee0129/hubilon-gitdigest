# Keycloak Role/Department/Team → DB 동기화 설계

## Context

Keycloak에 role(ROLE_ADMIN/ROLE_USER), 부서(서비스개발실/플랫폼개발실), 팀(플랫폼개발팀 등)을 그룹 계층으로 설정했다. JWT 토큰에 이미 클레임으로 포함되어 있는데, 이 정보를 DB와 동기화할지 Keycloak에서 매번 조회할지 결정이 필요하다.

현재 `SecurityUtils.getCurrentUser()`는 email → DB 조회만 하며, DB에 유저가 없으면 `NotFoundException`을 던진다. 즉, **첫 로그인 사용자 자동 생성(JIT Provisioning)이 없다**.

---

## 결론: JWT Claims → DB JIT Provisioning

**매 요청 Keycloak Admin API 호출 금지.** JWT 토큰에 이미 role과 department가 포함되어 있음.

| 항목 | JWT Claim | DB 컬럼 |
|------|-----------|---------|
| 권한 | `roles` → `["ROLE_ADMIN"]` | `users.role` (ADMIN/USER) |
| 팀 | `department` → `["/Hubilon/플랫폼개발실/플랫폼개발팀"]` | `users.team_id` → `teams` 테이블 |

**동기화 시점**: 로그인 후 첫 API 호출 시 (JIT), 이후 팀/역할 변경 시 재동기화

---

## JWT Claim 구조

```
roles claim:      ["ROLE_USER"] or ["ROLE_ADMIN"]
department claim: ["/Hubilon/플랫폼개발실/플랫폼개발팀"]
                   └ Full group path: On 설정
```

파싱:
- `roles[0]` → `ROLE_ADMIN` → `User.Role.ADMIN`
- `department[0]` → split by `/` → index 2 = `플랫폼개발실` (부서), index 3 = `플랫폼개발팀` (팀)

---

## 구현 계획

### Step 1: UserContext 커스텀 클레임 접근 확인
`common-auth-lib`의 `UserContext`가 `department` 클레임을 제공하는지 확인.
- 제공하면: `UserContext.getClaim("department")` 사용
- 미제공 시: `SecurityContextHolder`의 JWT에서 직접 파싱

### Step 2: UserProvisioningService 생성
**경로**: `backend/src/main/java/com/hubilon/modules/user/application/service/UserProvisioningService.java`

```java
@Service
public class UserProvisioningService {
    // JWT claims 파싱 → User 생성 or 업데이트
    public User provisionOrSync(String email) {
        // 1. UserContext에서 roles, department 클레임 추출
        // 2. department 파싱 → 팀명 추출 (마지막 세그먼트)
        // 3. Team find-or-create (teams 테이블)
        // 4. User upsert (email 기준)
    }
}
```

### Step 3: SecurityUtils.getCurrentUser() 수정
**경로**: `backend/src/main/java/com/hubilon/common/security/SecurityUtils.java`

```java
// 변경 전
return userQueryPort.findByEmail(email)
    .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

// 변경 후
return userQueryPort.findByEmail(email)
    .orElseGet(() -> userProvisioningService.provisionOrSync(email));
```

### Step 4: Team Find-or-Create
`teams` 테이블에서 팀명으로 조회 → 없으면 생성
- `TeamRepository.findByName(teamName)` 또는 `findOrCreate(teamName, departmentName)`

### Step 5: 재동기화 (로그인 시)
매 로그인 시 role/team 변경 감지 → DB 업데이트
- JWT claim과 DB 값 비교 → 다르면 업데이트 (역할 변경, 팀 이동 반영)

---

## 수정 파일 목록

| 파일 | 변경 내용 |
|------|---------|
| `SecurityUtils.java` | `orElseThrow` → `orElseGet` + provisioning 호출 |
| `UserProvisioningService.java` | 신규 생성 |
| `UserCommandPort.java` | `save(User)` 포트 확인/추가 |
| `TeamCommandPort.java` | 신규 생성 (`save(Team)`) |
| `TeamQueryPort.java` | `findByName` 메서드 추가 |
| `UserJpaEntity.java` | `password` nullable=true, `keycloak_username` 컬럼 추가 |
| `AuthController.java` | `me()` → JIT Provisioning 적용 |
| DB Migration | `users` 테이블: `password` nullable, `keycloak_username` 컬럼 추가 |

## JWT Claim 매핑 (확정)

| JWT Claim | DB 컬럼 | 비고 |
|-----------|---------|------|
| `roles[0]` | `users.role` | ROLE_ADMIN → ADMIN |
| `department[0]` (마지막 세그먼트) | `users.team_id` | 방어적 파싱 |
| `given_name + " " + family_name` | `users.name` | — |
| `preferred_username` | `users.keycloak_username` | 신규 컬럼 |
| `email` | `users.email` | — |

---

## Review 결과
- 검토일: 2026-05-08
- 검토 항목: 보안 / 리팩토링 / 기능
- 주요 수정사항:
  - [보안-2] Race Condition → `@Retryable` + `DataIntegrityViolationException` 핸들링
  - [보안-3] JWT `department` 파싱 → 마지막 세그먼트 방어적 파싱으로 변경
  - [보안-4] `AuthController.me()` JIT 적용 추가
  - [기능-1] `password` nullable=true로 변경
  - [기능-2] `name` = `given_name + " " + family_name`, `keycloak_username` 컬럼 추가
  - [리팩-2] `TeamCommandPort` 신규 생성
  - [리팩-3] Department find-or-create 흐름 추가

## 검증 방법

1. Keycloak 로그인 → DB에 users 레코드 자동 생성 확인
2. `GET /api/auth/me` 응답에 `teamId`, `teamName`, `role` 포함 확인
3. Keycloak에서 유저 그룹 변경 → 재로그인 → DB role/team 업데이트 확인
4. 존재하는 유저 재로그인 → 중복 생성 없음 확인
