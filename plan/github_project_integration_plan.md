# GitHub 프로젝트 추가 기능 설계

## 목표
GitLab 프로젝트 추가 버튼 옆에 GitHub 프로젝트 추가 버튼을 추가하고,
GitHub PAT / OAuth 인증을 통해 프로젝트를 등록할 수 있도록 한다.

---

## 현황 분석

### 기존 GitLab 흐름
```
Sidebar → AddProjectModal
  PAT: gitlabUrl + accessToken → POST /api/projects
  OAuth: GitLabOAuthController → 팝업 → postMessage → POST /api/projects
```

### DB (projects 테이블)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| git_provider | VARCHAR | 없음 (현재 GitLab 고정) |
| gitlab_url | VARCHAR | Git 저장소 URL (기술부채: 실제로는 repo_url) |
| gitlab_project_id | BIGINT | Git 프로젝트 숫자 ID (기술부채: GitHub repo id도 여기 저장) |

---

## 변경 범위

### 1. DB 마이그레이션

**마이그레이션 파일**: `V{next}__add_git_provider_to_projects.sql`
```sql
ALTER TABLE projects ADD COLUMN git_provider VARCHAR(10) NOT NULL DEFAULT 'GITLAB';
-- 기존 NULL row 대비 명시적 업데이트
UPDATE projects SET git_provider = 'GITLAB' WHERE git_provider IS NULL;
```

---

### 2. Backend

#### 2-1. GitProviderAdapter 인터페이스 (신규)
경로: `project/application/port/out/GitProviderAdapter.java`

```java
public interface GitProviderAdapter {
    Long resolveProjectId(String repoUrl, String token);
    String resolveProjectName(String repoUrl, String token);
    GitProvider supports();
}
```

- `GitLabAdapter`, `GitHubAdapter` 모두 이 인터페이스 구현
- `ProjectRegisterService`는 `Map<GitProvider, GitProviderAdapter>`로 주입받아 분기 제거

#### 2-2. 도메인 모델 수정
**`Project.java`**
- `gitProvider` 필드 추가 (`GitProvider` enum: `GITLAB`, `GITHUB`)

**`ProjectJpaEntity.java`**
- `gitProvider` 컬럼 추가 (`@Enumerated(EnumType.STRING)`, DEFAULT `GITLAB`)

#### 2-3. DTO / Command 수정
**`ProjectRegisterRequest.java`**
```java
GitProvider gitProvider  // null이면 GITLAB (하위호환)
```

**`ProjectRegisterCommand.java`** (record)
```java
GitProvider gitProvider  // null 불허 — Mapper에서 null → GITLAB 변환
```

> null → GITLAB 변환 위치: `ProjectAppMapper` (Command 생성 시점)

**`ProjectRegisterRequest` URL 검증 추가**:
- `@Pattern` 또는 커스텀 validator로 GitHub URL 형식 `https://github.com/{owner}/{repo}` 검증
- 백엔드 파싱 실패 시 400 Bad Request 반환

#### 2-4. GitHub OAuth 설정
**`GitHubOAuthProperties.java`** (새 파일)
```java
@ConfigurationProperties("github.oauth")
public record GitHubOAuthProperties(
    String clientId,
    String clientSecret,
    String redirectUri,
    String frontendOrigin
) {}
```

**`application.yml` 추가**
```yaml
github:
  oauth:
    client-id: ${GITHUB_OAUTH_CLIENT_ID}
    client-secret: ${GITHUB_OAUTH_CLIENT_SECRET}
    redirect-uri: ${GITHUB_OAUTH_REDIRECT_URI:http://localhost:8080/api/oauth/github/callback}
    frontend-origin: ${FRONTEND_ORIGIN:http://localhost:3000}
```

#### 2-5. GitHubOAuthController (새 파일)
경로: `adapter/in/web/GitHubOAuthController.java`

| 엔드포인트 | 설명 |
|-----------|------|
| `GET /api/oauth/github/authorize` | state 생성 + GitHub OAuth URL 반환 |
| `GET /api/oauth/github/callback` | state 검증 → 코드 → 토큰 교환 → 팝업 postMessage |

**OAuth CSRF 방지 (state)**:
```java
// authorize: UUID state 생성 → 세션/캐시 저장 → URL에 포함
String state = UUID.randomUUID().toString();
session.setAttribute("github_oauth_state", state);

// callback: state 검증
String savedState = (String) session.getAttribute("github_oauth_state");
if (!savedState.equals(requestState)) {
    // postMessage type: github-oauth-error, message: "invalid state"
}
```

**에러 postMessage 정제 규칙**:
- 내부 스택트레이스, 토큰 값 절대 포함 금지
- 허용 메시지: `"authentication_failed"`, `"invalid_state"`, `"token_exchange_failed"`

**OAuth scope**: `scope=repo` (private/public repo 모두 접근)

postMessage type: `github-oauth` (성공) / `github-oauth-error` (실패)

성공 postMessage payload:
```json
{ "type": "github-oauth", "token": "gho_xxx", "repoUrl": null }
```
> repoUrl은 null — OAuth 완료 후 모달에서 별도 입력받는 방식으로 확정

GitHub OAuth 고정 URL:
- authorize: `https://github.com/login/oauth/authorize`
- token: `https://github.com/login/oauth/access_token`

#### 2-6. GitHubAdapter (새 파일)
경로: `report/adapter/out/github/GitHubAdapter.java`

`GitProviderAdapter` 인터페이스 구현.

GitHub REST API (`https://api.github.com`):
```
GET /repos/{owner}/{repo}
Authorization: Bearer {token}
Accept: application/vnd.github+json
```

**URL 파싱**: `https://github.com/owner/repo` → `owner/repo`
- 형식 불일치 시 `IllegalArgumentException` throw → 상위에서 400 처리

```java
@Override
public GitProvider supports() { return GitProvider.GITHUB; }
```

**accessToken 로깅 금지**: 이 클래스에서 token 값을 log.debug/info에 포함 금지

#### 2-7. ProjectRegisterService 수정
Strategy 패턴 적용:
```java
// 생성자 주입
private final Map<GitProvider, GitProviderAdapter> adapterMap;

@Autowired
public ProjectRegisterService(List<GitProviderAdapter> adapters) {
    this.adapterMap = adapters.stream()
        .collect(Collectors.toMap(GitProviderAdapter::supports, a -> a));
}

// 사용
GitProviderAdapter adapter = adapterMap.get(command.gitProvider());
Long projectId = adapter.resolveProjectId(command.repoUrl(), command.accessToken());
```

---

### 3. Frontend

#### 3-1. Sidebar.tsx 수정
Projects 섹션 헤더에 버튼 2개:

```tsx
<div className={styles.addBtnGroup}>
  <button onClick={() => setIsGitLabModalOpen(true)}>+ GitLab 프로젝트 추가</button>
  <button onClick={() => setIsGitHubModalOpen(true)}>+ GitHub 프로젝트 추가</button>
</div>
```

#### 3-2. AddGitHubProjectModal.tsx (새 파일)
경로: `components/project/AddGitHubProjectModal.tsx`

**PAT 방식**:
- GitHub 저장소 URL 입력 (`https://github.com/owner/repo`)
  - 프론트엔드 validation: 정규식 `^https://github\.com/[\w.-]+/[\w.-]+$`
  - 안내 문구: "필요 권한: `repo` scope (private 저장소 포함)"
- Personal Access Token 입력
- 제출 → POST /api/projects `{ gitlabUrl: repoUrl, authType: 'PAT', accessToken, gitProvider: 'GITHUB' }`

**OAuth 방식**:
- `GET /api/oauth/github/authorize` → 팝업 오픈
- postMessage 수신 시 **반드시 origin 검증**:
  ```ts
  window.addEventListener('message', (event) => {
    if (event.origin !== import.meta.env.VITE_API_BASE_URL) return;
    if (event.data.type === 'github-oauth') { ... }
    if (event.data.type === 'github-oauth-error') { ... }
  });
  ```
- OAuth 완료 후 repoUrl 입력 필드 표시 (token은 state에 보관)
- 안내 문구: "필요 권한: `repo` scope로 앱 승인 필요"

#### 3-3. oauthApi.ts 수정
```ts
getGitHubAuthUrl: async (): Promise<GitHubAuthUrlResponse> => {
  const res = await apiClient.get('/oauth/github/authorize')
  return res.data.data
}
```

#### 3-4. types/report.ts 수정
`CreateProjectRequest`에 `gitProvider?: 'GITLAB' | 'GITHUB'` 추가

---

## 구현 순서

1. DB 마이그레이션 (git_provider 컬럼 + 기존 row UPDATE)
2. BE: `GitProviderAdapter` 인터페이스 신규 생성
3. BE: 도메인 모델 / Entity / DTO `gitProvider` 추가, Mapper null→GITLAB 변환
4. BE: `GitHubOAuthProperties` + yml 설정
5. BE: `GitHubAdapter` 구현 (`GitProviderAdapter` 구현)
6. BE: `GitLabAdapter` `GitProviderAdapter` 인터페이스 구현 추가
7. BE: `GitHubOAuthController` (state CSRF 방지 포함)
8. BE: `ProjectRegisterService` Strategy 패턴 적용
9. FE: `types/report.ts` 수정
10. FE: `oauthApi.ts` GitHub 메서드 추가
11. FE: `AddGitHubProjectModal.tsx` 신규 작성 (postMessage origin 검증 포함)
12. FE: `Sidebar.tsx` 버튼 추가 + 모달 연결

---

## 구현 범위 (1차)

| 기능 | 포함 여부 |
|------|----------|
| GitHub PAT 인증 | ✅ |
| GitHub OAuth (state CSRF 방지 포함) | ✅ |
| 기존 GitLab 기능 유지 | ✅ |
| Commit 조회 (GitHub API) | 별도 태스크 |
| GitHub 프로젝트 커밋 보고서 | 별도 태스크 |

---

## 파일 목록

### 신규 생성
- `backend/.../project/application/port/out/GitProviderAdapter.java`
- `backend/.../common/config/GitHubOAuthProperties.java`
- `backend/.../project/adapter/in/web/GitHubOAuthController.java`
- `backend/.../report/adapter/out/github/GitHubAdapter.java`
- `frontend/src/components/project/AddGitHubProjectModal.tsx`
- `frontend/src/components/project/AddGitHubProjectModal.module.css`
- `backend/src/main/resources/db/migration/V{N}__add_git_provider_to_projects.sql`

### 수정
- `backend/.../project/domain/model/Project.java`
- `backend/.../project/adapter/out/persistence/ProjectJpaEntity.java`
- `backend/.../project/adapter/in/web/ProjectRegisterRequest.java`
- `backend/.../project/application/dto/ProjectRegisterCommand.java`
- `backend/.../project/application/mapper/ProjectAppMapper.java`
- `backend/.../project/application/service/command/ProjectRegisterService.java`
- `backend/.../report/adapter/out/gitlab/GitLabAdapter.java` (GitProviderAdapter 구현 추가)
- `backend/src/main/resources/application.yml`
- `frontend/src/types/report.ts`
- `frontend/src/services/oauthApi.ts`
- `frontend/src/components/layout/Sidebar.tsx`

---

## Review 결과
- 검토일: 2026-04-21
- 검토 항목: 보안 / 리팩토링 / 기능
- 수정 반영: OAuth state CSRF 방지, postMessage origin 검증, GitProviderAdapter 인터페이스, Strategy 패턴, OAuth repoUrl 처리 확정, PAT scope 명시, null→GITLAB 변환 위치(Mapper), URL 형식 검증, 마이그레이션 UPDATE 구문 추가
