# Keycloak SSO 전환 Plan

## 목표
기존 커스텀 JWT 로그인 → Keycloak SSO (common-auth-lib-1.0.0.jar) 전환

---

## 인증 흐름 비교

| 구분 | 기존 (JWT) | 변경 후 (Keycloak SSO) |
|------|-----------|----------------------|
| 인증 방식 | Bearer 토큰 (Authorization 헤더) | HttpOnly 쿠키 (access_token, refresh_token) |
| 로그인 | POST /api/auth/login (이메일/비밀번호) | GET /auth/login → Keycloak 리다이렉트 |
| 토큰 갱신 | POST /api/auth/refresh (body에 refreshToken) | POST /auth/refresh (쿠키 자동 전송) |
| 로그아웃 | POST /api/auth/logout | GET /auth/logout → Keycloak SSO 종료 |
| CSRF | 비활성화 | XSRF-TOKEN 쿠키 → X-XSRF-TOKEN 헤더 |
| 세션 | STATELESS | 서버 세션 (HttpOnly 쿠키 기반) |

---

## Backend 작업

### Step 1. build.gradle.kts 의존성 수정

**추가:**
```kotlin
implementation(files("libs/common-auth-lib-1.0.0.jar"))
implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
```

**제거:**
```kotlin
// 아래 3개 제거 (라이브러리가 대체)
implementation("io.jsonwebtoken:jjwt-api:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
```

> `spring-boot-starter-security`는 이미 존재 → 유지

---

### Step 2. application.yml 수정

**추가 (keycloak 블록):**
```yaml
keycloak:
  server-url: http://192.168.10.30:8080
  realm: hubilon_pd
  client-id: hubilon-gitdigest-local
  client-secret: YUA90njSxvvdXq6crpJj58oiAnQHxe3j
  oauth-state-cookie: my_oauth_state
  session-id-token-key: my_id_token
  redirect-uri: http://localhost:8080/auth/callback
  post-logout-redirect-uri: http://localhost:8080/auth/login
  post-login-redirect-uri: /
  secure-cookie: false
  permit-all-paths:
    - /public/**
    - /health
    - /actuator/**
    - /swagger-ui/**
    - /api-docs/**
    - /swagger-ui.html
  auth-controller:
    enabled: true
  uri:
    login: /auth/login
    callback: /auth/callback
    logout: /auth/logout
    refresh: /auth/refresh
```

**제거 (jwt 블록):**
```yaml
jwt:
  secret: ...
  access-token-expiry: ...
  refresh-token-expiry: ...
```

---

### Step 3. 삭제할 파일 목록

| 파일 경로 | 이유 |
|-----------|------|
| `common/config/JwtAuthFilter.java` | 라이브러리 KeycloakTokenFilter로 대체 |
| `modules/auth/adapter/out/jwt/JwtProperties.java` | JWT 설정 클래스 불필요 |
| `modules/auth/adapter/out/jwt/JwtTokenAdapter.java` | 라이브러리가 토큰 처리 |
| `modules/auth/adapter/out/persistence/RefreshTokenJpaEntity.java` | 리프레시 토큰 DB 관리 불필요 |
| `modules/auth/adapter/out/persistence/RefreshTokenPersistenceAdapter.java` | 동상 |
| `modules/auth/adapter/out/persistence/RefreshTokenRepository.java` | 동상 |
| `modules/auth/application/dto/LoginCommand.java` | 커스텀 로그인 DTO 불필요 |
| `modules/auth/application/dto/LoginResult.java` | 동상 |
| `modules/auth/application/service/LoginService.java` | 라이브러리가 처리 |
| `modules/auth/application/service/LogoutService.java` | 동상 |
| `modules/auth/application/service/TokenRefreshService.java` | 동상 |
| `modules/auth/domain/model/TokenPair.java` | 불필요 |
| `modules/auth/domain/port/in/LoginUseCase.java` | 불필요 |
| `modules/auth/domain/port/in/LogoutUseCase.java` | 불필요 |
| `modules/auth/domain/port/in/TokenRefreshUseCase.java` | 불필요 |
| `modules/auth/domain/port/out/RefreshTokenPort.java` | 불필요 |
| `modules/auth/domain/port/out/TokenPort.java` | 불필요 |
| `modules/auth/adapter/in/web/LoginRequest.java` | 불필요 |
| `modules/auth/adapter/in/web/LogoutRequest.java` | 불필요 |
| `modules/auth/adapter/in/web/RefreshRequest.java` | 불필요 |

---

### Step 4. SecurityConfig.java 수정

라이브러리가 SecurityFilterChain을 자동 구성하므로 기존 설정 제거.

**제거 내용:**
- `JwtAuthFilter` 주입 및 `addFilterBefore()`
- `csrf(AbstractHttpConfigurer::disable)` — 라이브러리가 CSRF 활성화 상태로 관리
- `SessionCreationPolicy.STATELESS` — 쿠키 세션 방식으로 변경
- 기존 `authorizeHttpRequests` 규칙 전체 — `application.yml` `permit-all-paths`로 이관

**유지 내용:**
- `PasswordEncoder` Bean (User 등록/인증 로직에 필요하면 유지, 없으면 삭제)

**변경 후 예시:**
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    // 라이브러리가 FilterChain 자동 등록
    // 추가 permitAll 규칙이 필요하면 keycloak.permit-all-paths 에 추가

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

> `@RequiredArgsConstructor` 제거 — JwtAuthFilter 의존성 소멸

---

### Step 5. AuthController.java 수정

라이브러리가 `/auth/login`, `/auth/callback`, `/auth/logout`, `/auth/refresh`를 자동 등록하므로 기존 엔드포인트 대부분 제거.

**유지 (수정 필요):**
```java
// GET /api/auth/me — 현재 사용자 정보 반환
@GetMapping("/me")
public Response<LoginResponse.UserInfo> me(@CurrentUser UserInfo keycloakUser) {
    // UserContext.getUserId() 로 Keycloak userId 획득 후 DB 조회
    User user = userQueryUseCase.findByEmail(keycloakUser.email())
            .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    return Response.ok(...);
}
```

**제거:**
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- 관련 UseCase 주입 필드

**LoginResponse.java** — UserInfo 내부 record는 유지. accessToken, refreshToken, expiresIn 필드 제거.

---

### Step 6. SecurityUtils.java 수정

`SecurityContextHolder.getAuthentication().getName()` → 라이브러리의 `UserContext` 사용

```java
// 변경 후
public User getCurrentUser() {
    String email = UserContext.getEmail(); // 또는 UserContext.getUserId()
    return userQueryPort.findByEmail(email)
            .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
}
```

> 라이브러리 UserInfo/UserContext의 실제 필드명은 JAR 내부 확인 후 결정

---

### Step 7. DB 마이그레이션 (Flyway)

- `refresh_tokens` 테이블 drop 스크립트 추가 (Keycloak이 토큰 관리)
- 새 migration 파일: `V{다음번호}__remove_refresh_tokens.sql`

---

## Frontend 작업

### Step 1. services/axios.ts 수정

**제거:**
- 요청 인터셉터의 Bearer 토큰 주입 (`Authorization: Bearer ${token}`)
- `useAuthStore` 의존성

**수정 (401 인터셉터):**
- 기존: refreshToken을 body로 전송
- 변경: body 없이 POST /auth/refresh (refresh_token 쿠키 자동 전송)

```typescript
const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  timeout: 30000,
  withCredentials: true,  // HttpOnly 쿠키 자동 전송 (필수)
  headers: { 'Content-Type': 'application/json' },
  // xsrfCookieName: 'XSRF-TOKEN' (axios 기본값 — 명시 불필요)
  // xsrfHeaderName: 'X-XSRF-TOKEN' (axios 기본값 — 명시 불필요)
})

// 요청 인터셉터: 제거 (Bearer 토큰 불필요)

// 401 인터셉터 수정
let isRefreshing = false
let refreshQueue: Array<() => void> = []

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/auth/refresh')
    ) {
      originalRequest._retry = true

      if (isRefreshing) {
        return new Promise((resolve) => {
          refreshQueue.push(() => resolve(apiClient(originalRequest)))
        })
      }

      isRefreshing = true
      try {
        await apiClient.post('/auth/refresh')  // body 없음, 쿠키 자동 전송
        refreshQueue.forEach((cb) => cb())
        refreshQueue = []
        return apiClient(originalRequest)
      } catch {
        window.location.href = '/auth/login'   // Keycloak 로그인 페이지
        return Promise.reject(error)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)
```

---

### Step 2. stores/useAuthStore.ts 수정

토큰 상태 제거. 사용자 정보만 관리.

```typescript
interface AuthStore {
  user: User | null
  fetchUser: () => Promise<void>
  logout: () => void
  setUser: (user: User) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthStore>((set) => ({
  user: null,
  fetchUser: async () => {
    const data = await authApi.me()
    set({ user: data })
  },
  logout: () => {
    queryClient.clear()
    window.location.href = '/auth/logout'  // Keycloak SSO 로그아웃
  },
  setUser: (user) => set({ user }),
  clearAuth: () => set({ user: null }),
}))
```

---

### Step 3. services/authApi.ts 수정

```typescript
const authApi = {
  me: async () => {
    const res = await apiClient.get<ApiResponse<UserInfo>>('/auth/me')
    return res.data.data
  },
  // login/logout/refresh 제거 — window.location.href 또는 인터셉터 처리
}
```

---

### Step 4. pages/LoginPage.tsx 수정

이메일/비밀번호 폼 → Keycloak 리다이렉트 버튼으로 교체

```typescript
export default function LoginPage() {
  const handleLogin = () => {
    window.location.href = '/auth/login'
  }

  return (
    <div className={styles.container}>
      <div className={styles.card}>
        {/* 헤더 유지 */}
        <button onClick={handleLogin} className={styles.submitBtn}>
          Keycloak으로 로그인
        </button>
      </div>
    </div>
  )
}
```

---

### Step 5. components/common/ProtectedRoute.tsx 수정

accessToken 체크 → user 상태 체크 (없으면 me API 호출)

```typescript
export default function ProtectedRoute() {
  const { user, fetchUser } = useAuthStore()
  const [loading, setLoading] = useState(!user)

  useEffect(() => {
    if (!user) {
      fetchUser()
        .catch(() => { window.location.href = '/auth/login' })
        .finally(() => setLoading(false))
    }
  }, [])

  if (loading) return null
  if (!user) return null
  return <Outlet />
}
```

---

### Step 6. 회원가입 처리

`SignupPage.tsx` — Keycloak에서 사용자 관리 → 제거 또는 사용 안 함 처리
- Keycloak 콘솔 또는 Keycloak 자체 회원가입 페이지로 안내

---

## 삭제 대상 Frontend 파일

| 파일 | 이유 |
|------|------|
| `pages/SignupPage.tsx` | Keycloak에서 사용자 생성 |
| `pages/LoginPage.module.css` | 폼 제거 시 CSS 대부분 불필요 (최소화) |

---

## 작업 순서

```
1. Backend
   1-1. build.gradle.kts 수정
   1-2. application.yml 수정
   1-3. 삭제 대상 파일 제거
   1-4. SecurityConfig.java 수정
   1-5. AuthController.java 수정 (me 엔드포인트 유지)
   1-6. SecurityUtils.java 수정
   1-7. Flyway 마이그레이션 스크립트 추가
   1-8. 빌드 확인

2. Frontend
   2-1. axios.ts 수정 (인터셉터 교체)
   2-2. useAuthStore.ts 수정
   2-3. authApi.ts 수정
   2-4. LoginPage.tsx 수정
   2-5. ProtectedRoute.tsx 수정
   2-6. Header.tsx / Sidebar.tsx 로그아웃 처리 수정
   2-7. SignupPage 처리
```

---

## 주의 사항

1. **`UserContext` API 확인 필요** — `common-auth-lib-1.0.0.jar` 내부의 실제 메서드명 확인 후 `SecurityUtils.java` 수정
2. **User 도메인 연동** — **email 기준 매핑으로 결정.** Keycloak `email` scope가 Default이므로 JWT에 항상 포함. `UserContext.getEmail()` → `userQueryPort.findByEmail(email)`. `UserRepository.findByEmail()` 이미 구현됨. 전제: Keycloak 사용자 email과 로컬 DB email 동일해야 함.
3. **`/api/auth/me` 경로** — 기존 프론트엔드가 `/api/auth/me`를 호출 중. 라이브러리가 `/auth/callback` 이하를 처리하므로 `/api/auth/me`는 그대로 유지 가능
4. **CORS 설정** — `WebConfig.java`의 CORS allowedOrigins에 Keycloak 서버(192.168.10.30:8080) 관련 설정 필요 여부 확인
5. **로컬 개발** — `secure-cookie: false` 확인, Keycloak 서버 접근 가능 여부 확인


---

## Review 결과
- 검토일: 2026-05-07
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: 이슈 발견 후 구현 중 수정 완료

## 구현 완료
- 백엔드: 96개 테스트 통과
- 프론트엔드: 24개 테스트 통과
- Bean 충돌, 테스트 인증 설정 등 구현 중 이슈 해결 완료
