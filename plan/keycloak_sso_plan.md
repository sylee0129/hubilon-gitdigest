# Keycloak SSO 연동 구현 Plan

## 환경 정보
- Keycloak Host: `http://192.168.10.30:8080`
- Realm: `hubilon_pd`
- Client ID: `hubilon-gitdigest`

---

## 현재 상태 분석

### 백엔드 (Spring Boot 4.0.5, Java 25)
- 자체 JWT 발급/검증 (`JwtTokenAdapter`) — Keycloak 전환 시 제거 대상
- `/auth/login`, `/auth/logout`, `/auth/refresh` 엔드포인트 존재
- `RefreshTokenJpaEntity` — DB에 refresh token 저장 중

### 프론트엔드 (React 19 + Vite)
- `useAuthStore.ts` (Zustand) — email/password 로그인, accessToken/refreshToken 메모리 보관
- `axios.ts` — Bearer 토큰 인터셉터 + 401 시 refresh 재시도
- `LoginPage.tsx`, `SignupPage.tsx` 존재

---

## Keycloak 구성 현황

### Groups 계층 구조
```
Hubilon (최상위)
├── 서비스개발실
│   └── (팀들...)
├── 솔루션연구소
│   └── (팀들...)
└── 플랫폼개발실
    └── 플랫폼개발팀
```
사용자는 최하위 팀 노드에 소속. JWT에는 full path로 포함.

### Client Roles — 역할 할당 방식

역할은 **사용자 개별 할당이 아닌 Group Role Mapping으로 관리**.
사용자가 그룹에 속하면 해당 그룹에 매핑된 Client role이 JWT에 자동 포함됨.

```
사용자 → 그룹 소속 → 그룹 Role Mapping → resource_access.hubilon-gitdigest.roles (JWT)
예) 플랫폼개발팀 소속 → ROLE_ADMIN 자동 부여
```

| 역할명 | Spring Security Authority | 할당 방식 |
|--------|--------------------------|----------|
| `ROLE_ADMIN` | `ROLE_ADMIN` | 그룹 Role Mapping |
| `ROLE_USER` | `ROLE_USER` | 그룹 Role Mapping |

> JWT 위치: `resource_access.hubilon-gitdigest.roles`
> 모든 역할이 `ROLE_` 접두사를 포함하므로 변환 없이 그대로 사용.

### JWT Claim 출처 정리

| 클레임 | 출처 | 위치 | 예시 값 |
|--------|------|------|---------|
| `preferred_username` | profile scope (Default) | 최상위 | `"suyeon129"` |
| `email` | email scope (Default) | 최상위 | `"..."` |
| `resource_access.hubilon-gitdigest.roles` | roles scope (Default) + Group Role Mapping | nested | `["ROLE_ADMIN"]` |
| `department` | Dedicated Scope Mapper `department-mapper` (Group Membership, Full path ON) | 최상위 배열 | `["/Hubilon/플랫폼개발실/플랫폼개발팀"]` |

> `department-mapper`는 Client Dedicated Scopes에 이미 구성됨 — 별도 Admin 작업 불필요.

### department 클레임 파싱 규칙
```
"/Hubilon/플랫폼개발실/플랫폼개발팀"
  parts[0] ""           → 무시 (leading slash)
  parts[1] "Hubilon"    → 회사명
  parts[2] "플랫폼개발실" → 부서명 (departmentName)
  parts[3] "플랫폼개발팀" → 팀명 (teamName)
```
복수 그룹 소속 시 첫 번째 path만 사용. depth 2단계 사용자(`/Hubilon/부서`)는 teamName = null.

---

## 아키텍처 전환 방향

```
[Before]  Frontend ──email/pw──▶ Spring Boot (자체 JWT 발급)
[After]   Frontend ──OIDC Auth Code──▶ Keycloak ──JWT 발급
                   Spring Boot = Resource Server (Keycloak JWT 검증만)
```

- Spring Boot는 JWT를 발급하지 않고 **검증만** 담당
- 사용자 인증/세션은 Keycloak이 전담
- Frontend는 `keycloak-js`로 Auth Code Flow 처리

---

## 백엔드 구현

### 1. `build.gradle` 의존성 추가
```groovy
implementation 'org.springframework.boot:spring-boot-starter-oauth2-resource-server'
```

### 2. `application.yml` 추가 설정
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_URL:http://192.168.10.30:8080}/realms/${KEYCLOAK_REALM:hubilon_pd}
          jwk-set-uri: ${KEYCLOAK_URL:http://192.168.10.30:8080}/realms/${KEYCLOAK_REALM:hubilon_pd}/protocol/openid-connect/certs

keycloak:
  auth-server-url: ${KEYCLOAK_URL:http://192.168.10.30:8080}
  realm: ${KEYCLOAK_REALM:hubilon_pd}
  client-id: ${KEYCLOAK_CLIENT_ID:hubilon-gitdigest}
```

> 환경변수(`KEYCLOAK_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`) 미설정 시 내부 IP 기본값 사용.

### 3. `JwtClaimExtractor` 유틸 작성
위치: `common/config/security/JwtClaimExtractor.java`

> `resource_access` 파싱 로직을 단일 유틸로 추출하여 `KeycloakJwtConverter`·`UserInfoResponse` 중복 제거.

```java
public final class JwtClaimExtractor {

    private JwtClaimExtractor() {}

    @SuppressWarnings("unchecked")
    public static List<String> extractClientRoles(Jwt jwt, String clientId) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null) return List.of();
        Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(clientId);
        if (clientAccess == null) return List.of();
        List<String> roles = (List<String>) clientAccess.get("roles");
        return roles != null ? roles : List.of();
    }

    public static String extractDepartmentName(Jwt jwt) {
        List<String> departments = jwt.getClaimAsStringList("department");
        if (departments == null || departments.isEmpty()) return null;
        String[] parts = departments.get(0).split("/");
        return parts.length >= 3 ? parts[2] : null;
    }

    public static String extractTeamName(Jwt jwt) {
        List<String> departments = jwt.getClaimAsStringList("department");
        if (departments == null || departments.isEmpty()) return null;
        String[] parts = departments.get(0).split("/");
        return parts.length >= 4 ? parts[3] : null;
    }
}
```

### 4. `KeycloakJwtConverter` 작성
위치: `common/config/security/KeycloakJwtConverter.java`

> **principal name을 `email` claim으로 설정** — 기존 `SecurityUtils.getCurrentUser()`가 `authentication.getName()`으로 `findByEmail()`을 호출하므로 `preferred_username` 대신 `email`을 사용해야 기존 API가 정상 동작한다.

```java
@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Value("${keycloak.client-id}")
    private String clientId;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<String> roles = JwtClaimExtractor.extractClientRoles(jwt, clientId);
        Collection<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        // principal name = email (SecurityUtils.findByEmail() 호환)
        return new JwtAuthenticationToken(jwt, authorities, jwt.getClaimAsString("email"));
    }
}
```

### 5. `SecurityConfig` 재작성
위치: `common/config/SecurityConfig.java`

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final KeycloakJwtConverter keycloakJwtConverter;
    private final KeycloakLogoutHandler keycloakLogoutHandler;

    public SecurityConfig(KeycloakJwtConverter keycloakJwtConverter,
                          KeycloakLogoutHandler keycloakLogoutHandler) {
        this.keycloakJwtConverter = keycloakJwtConverter;
        this.keycloakLogoutHandler = keycloakLogoutHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**", "/swagger-ui/**", "/api-docs/**").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter))
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .addLogoutHandler(keycloakLogoutHandler)
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_OK))
            );
        return http.build();
    }
}
```

### 6. `KeycloakLogoutHandler` 작성
위치: `common/config/security/KeycloakLogoutHandler.java`

> Keycloak 25+는 `id_token_hint` 기반 logout 권장. refresh_token도 fallback으로 지원.

```java
@Component
public class KeycloakLogoutHandler implements LogoutHandler {

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
                       Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String idToken = jwtAuth.getToken().getTokenValue();
            String logoutUrl = authServerUrl + "/realms/" + realm
                    + "/protocol/openid-connect/logout";

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("id_token_hint", idToken);

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body,
                    new HttpHeaders() {{ setContentType(MediaType.APPLICATION_FORM_URLENCODED); }});

            try {
                restTemplate.postForEntity(logoutUrl, entity, Void.class);
            } catch (Exception ignored) {
                // Keycloak logout 실패해도 로컬 세션은 정리됨 (STATELESS)
            }
        }
    }
}
```

### 7. `/api/auth/me` 엔드포인트 수정
- DB 조회 제거 → JWT Claims 직접 추출
- `JwtClaimExtractor` 사용으로 중복 파싱 제거

```java
@GetMapping("/me")
public Response<UserInfoResponse> me(Authentication authentication) {
    Jwt jwt = (Jwt) ((JwtAuthenticationToken) authentication).getToken();
    return Response.ok(UserInfoResponse.fromJwt(jwt));
}
```

`UserInfoResponse` record:
```java
public record UserInfoResponse(
    String username,
    String email,
    String role,           // ROLE_ADMIN / ROLE_USER
    String departmentName,
    String teamName
) {
    @Value("${keycloak.client-id}")  // record static factory에서 직접 @Value 불가 — 호출 측에서 clientId 주입
    public static UserInfoResponse fromJwt(Jwt jwt, String clientId) {
        List<String> roles = JwtClaimExtractor.extractClientRoles(jwt, clientId);
        String role = roles.contains("ROLE_ADMIN") ? "ROLE_ADMIN" : "ROLE_USER";

        return new UserInfoResponse(
            jwt.getClaimAsString("preferred_username"),
            jwt.getClaimAsString("email"),
            role,
            JwtClaimExtractor.extractDepartmentName(jwt),
            JwtClaimExtractor.extractTeamName(jwt)
        );
    }
}
```

> `AuthController`에서 `@Value("${keycloak.client-id}") String clientId`를 주입받아 `UserInfoResponse.fromJwt(jwt, clientId)` 호출.

### 8. DB User 동기화 전략 — 첫 로그인 자동 프로비저닝
Keycloak 전환 후 `SecurityUtils.getCurrentUser()`는 `findByEmail()`로 동작하는데, 기존 DB에 없는 신규 사용자는 NotFoundException 발생.

`/api/auth/me` 호출 시 DB에 없으면 JWT claims로 자동 생성:

```java
// AuthController 또는 UserProvisioningService
@GetMapping("/me")
public Response<UserInfoResponse> me(Authentication authentication) {
    Jwt jwt = (Jwt) ((JwtAuthenticationToken) authentication).getToken();
    String email = jwt.getClaimAsString("email");

    // DB에 없으면 자동 프로비저닝
    if (!userRepository.existsByEmail(email)) {
        User newUser = User.builder()
            .email(email)
            .name(jwt.getClaimAsString("preferred_username"))
            .build();
        userRepository.save(newUser);
    }

    return Response.ok(UserInfoResponse.fromJwt(jwt, clientId));
}
```

### 9. 제거 대상 (점진적 삭제)
| 대상 | 이유 |
|------|------|
| `JwtTokenAdapter`, `JwtProperties` | Keycloak JWT 검증으로 대체 |
| `LoginService`, `LogoutService`, `TokenRefreshService` | Keycloak이 담당 |
| `RefreshTokenJpaEntity`, `RefreshTokenRepository` | DB refresh token 불필요 |
| `/auth/login`, `/auth/refresh` 엔드포인트 | Keycloak 직접 처리 |
| `AuthController.logout()` 메서드 | Spring Security logout filter가 처리 |
| `jwt.*` application.yml 설정 | 불필요 |
| `BCryptPasswordEncoder` bean | 자체 인증 제거로 불필요 |
| `UserJpaEntity.password` 컬럼 | Keycloak이 비밀번호 관리 |

> 삭제는 프론트엔드 전환 완료 후 진행

---

## 프론트엔드 구현

### 1. 의존성 설치
```bash
npm install keycloak-js
```

### 2. `src/lib/keycloak.ts` 생성
```ts
import Keycloak from 'keycloak-js'

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL,
  realm: import.meta.env.VITE_KEYCLOAK_REALM,
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
})

export default keycloak
```

### 3. `src/types/keycloak.d.ts` 생성
`KeycloakTokenParsed`에 커스텀 클레임 타입 확장:

```ts
declare module 'keycloak-js' {
  interface KeycloakTokenParsed {
    department?: string[]
    resource_access?: {
      [clientId: string]: {
        roles: string[]
      }
    }
  }
}
```

### 4. `main.tsx` — Keycloak 초기화 후 React 렌더
> `onTokenExpired`는 `init()` **직전**에 등록해야 한다 — init이 완료되기 전에 만료 이벤트가 발생할 수 있음.

```tsx
import keycloak from './lib/keycloak'

// init() 전에 이벤트 핸들러 등록
keycloak.onTokenExpired = () => {
  keycloak.updateToken(70).catch(() => keycloak.logout())
}

keycloak.init({
  onLoad: 'login-required',
  checkLoginIframe: false, // Single Sign-Out 비활성화 (내부망 환경 허용)
  pkceMethod: 'S256',
}).then((authenticated) => {
  if (authenticated) {
    useAuthStore.getState().initFromKeycloak(keycloak)
    ReactDOM.createRoot(document.getElementById('root')!).render(<App />)
  }
})
```

### 5. `useAuthStore.ts` 재작성
```ts
interface User {
  id: string
  name: string
  email: string
  role: 'ROLE_ADMIN' | 'ROLE_USER'
  departmentName: string | null
  teamName: string | null
}

interface AuthStore {
  user: User | null
  getToken: () => string | undefined
  initFromKeycloak: (kc: Keycloak) => void
  logout: () => void
}

function parseDepartment(paths: string[] | undefined) {
  if (!paths?.length) return { departmentName: null, teamName: null }
  const parts = paths[0].split('/')
  return {
    departmentName: parts[2] ?? null,
    teamName: parts[3] ?? null,  // depth 2단계 사용자는 null
  }
}

function resolveRole(tokenParsed: KeycloakTokenParsed | undefined): User['role'] {
  const roles = tokenParsed?.resource_access?.[import.meta.env.VITE_KEYCLOAK_CLIENT_ID]?.roles ?? []
  return roles.includes('ROLE_ADMIN') ? 'ROLE_ADMIN' : 'ROLE_USER'
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  getToken: () => keycloak.token,
  initFromKeycloak: (kc) => {
    const p = kc.tokenParsed
    const { departmentName, teamName } = parseDepartment(p?.department)
    set({
      user: {
        id:   p?.sub ?? '',
        name: p?.preferred_username ?? '',
        email: p?.email ?? '',
        role: resolveRole(p),
        departmentName,
        teamName,
      }
    })
  },
  logout: () => keycloak.logout({ redirectUri: window.location.origin }),
}))
```

### 6. `axios.ts` 인터셉터 수정
```ts
apiClient.interceptors.request.use(async (config) => {
  // 만료 30초 이내일 때만 갱신 (불필요한 Keycloak 트래픽 방지)
  if (keycloak.isTokenExpired(30)) {
    await keycloak.updateToken(30)
  }
  if (keycloak.token) {
    config.headers.Authorization = `Bearer ${keycloak.token}`
  }
  return config
})

// 기존 401 refresh 재시도 로직 제거 — updateToken이 담당
```

### 7. `LoginPage.tsx`, `SignupPage.tsx`, `ProtectedRoute`
- `onLoad: 'login-required'`로 미인증 시 자동 리다이렉트 → `LoginPage.tsx` 내부 라우트에서 제거 (로딩 스피너로 대체)
- `SignupPage.tsx` — Keycloak 사용자 관리로 대체 (삭제)
- `ProtectedRoute` — `accessToken` 기반 검사 → `keycloak.authenticated` 기반으로 교체 (또는 삭제)

```tsx
// ProtectedRoute 수정
const ProtectedRoute = () => {
  if (!keycloak.authenticated) {
    keycloak.login()
    return null
  }
  return <Outlet />
}
```

### 8. 환경변수 추가
`.env.development`:
```
VITE_KEYCLOAK_URL=http://192.168.10.30:8080
VITE_KEYCLOAK_REALM=hubilon_pd
VITE_KEYCLOAK_CLIENT_ID=hubilon-gitdigest
```
`.env.example` 동일 항목 추가 (값 비움)

---

## Keycloak Admin 설정 체크리스트

### `hubilon-gitdigest` Client 기본 설정

| 항목 | 값 |
|------|----|
| Access Type | public |
| Valid Redirect URIs | `http://localhost:3000/*`, 운영 도메인 |
| Web Origins | `http://localhost:3000`, 운영 도메인 |
| Standard Flow | Enabled |
| Direct Access Grants | Disabled |
| Backchannel Logout | Enabled |

### Client Scopes 확인

| Scope | Assigned type | 확인 내용 |
|-------|--------------|----------|
| `roles` | Default | Client roles가 JWT에 포함되는지 확인 |
| `email` | Default | `email` 클레임 포함 |
| `profile` | Default | `preferred_username` 포함 |
| `department-mapper` | Dedicated scope | 이미 구성됨 — 추가 작업 불필요 |

---

## 구현 순서

```
1.  [BE] build.gradle 의존성 추가
2.  [BE] application.yml Keycloak Resource Server 설정 (환경변수 바인딩 포함)
3.  [BE] JwtClaimExtractor 유틸 구현
4.  [BE] KeycloakJwtConverter 구현 (principal=email)
5.  [BE] SecurityConfig 재작성 (DI 방식)
6.  [BE] KeycloakLogoutHandler 구현 (id_token_hint)
7.  [BE] /api/auth/me — UserInfoResponse + 첫 로그인 자동 프로비저닝
8.  [FE] keycloak-js 설치 + .env 추가
9.  [FE] src/lib/keycloak.ts + src/types/keycloak.d.ts 생성
10. [FE] main.tsx keycloak.init() 추가 (onTokenExpired 선등록)
11. [FE] useAuthStore.ts 재작성
12. [FE] axios.ts 인터셉터 수정 (isTokenExpired 선확인)
13. [FE] LoginPage, SignupPage 정리 + ProtectedRoute 수정
14. [BE] 기존 auth 모듈(자체 JWT) 제거
```

> 백엔드 1~7 완료 후 프론트엔드 8~13 진행

---

## 주의사항

- Keycloak 서버가 프론트엔드·백엔드 양쪽에서 네트워크 접근 가능해야 함
- `issuer-uri` 내부 IP면 로컬 개발 환경에서도 동일 IP 접근 가능한지 확인
- CORS: Keycloak Admin Web Origins 설정 필수
- `department` 경로 depth가 3단계(`/회사/부서/팀`) 고정임을 전제 — depth 가변 시 파싱 로직 수정 필요
- Client roles는 `resource_access.<clientId>.roles` — `realm_access.roles`(Realm roles)와 다름
- `KeycloakTokenParsed`에 `department` 없음 — `src/types/keycloak.d.ts`로 타입 확장 필수
- `checkLoginIframe: false` — 내부망 환경 허용, Single Sign-Out 비활성화 trade-off 인지

---

## Review 결과
- 검토일: 2026-05-06
- 검토 항목: 보안 / 리팩토링 / 기능
- 반영된 수정 사항:
  - [치명] KeycloakJwtConverter principal name을 `email`로 변경 → SecurityUtils 호환
  - [치명] DB User 동기화 전략 추가 — /api/auth/me 첫 접근 시 자동 프로비저닝
  - [높음] Keycloak 설정 환경변수 바인딩 (`${KEYCLOAK_URL}` 등)
  - [중간] JwtClaimExtractor 유틸 추출 — resource_access 파싱 중복 제거
  - [중간] CLIENT_ID `@Value` 주입으로 하드코딩 제거
  - [중간] KeycloakJwtConverter `@Bean` DI 방식으로 SecurityConfig에서 주입
  - [중간] KeycloakLogoutHandler id_token_hint 방식으로 수정
  - [중간] main.tsx onTokenExpired를 init() 직전에 등록
  - [낮음] ProtectedRoute keycloak.authenticated 기반으로 수정
  - [낮음] axios.ts isTokenExpired(30) 선확인 추가
  - [낮음] AuthController.logout() 및 BCryptPasswordEncoder 제거 대상 목록 추가
