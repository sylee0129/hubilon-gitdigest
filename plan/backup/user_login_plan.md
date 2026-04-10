# 담당자 로그인 기능 계획

**작성일:** 2026-04-09  
**대상 모듈:** Backend (`modules/auth`, `modules/user` 확장) + Frontend

---

## Overview

`project_folder_management_plan.md`에서 설계한 `users` 테이블(담당자 풀)을 기반으로,  
시스템 담당자가 **이메일 + 비밀번호**로 로그인하고, **JWT**로 API 접근을 인증하는 기능을 추가한다.

기존 GitLab OAuth는 GitLab 프로젝트 연동용이며, 시스템 로그인과 분리된다.

```
[담당자] → 이메일 + 비밀번호 입력 → JWT 발급 → API 요청 시 Bearer 토큰 포함
```

---

## 현재 상태 (Before)

| 항목 | 상태 |
|------|------|
| Spring Security | ❌ 미적용 (build.gradle에 미포함) |
| JWT 라이브러리 | ❌ 없음 |
| User 엔티티 | ❌ 없음 (folder plan에서 신규 설계) |
| 비밀번호 필드 | ❌ 없음 |
| Frontend 로그인 페이지 | ❌ 없음 |
| Axios 토큰 처리 | ❌ 없음 |

---

## 인증 방식 결정

| 항목 | 선택 | 이유 |
|------|------|------|
| 인증 방식 | 이메일 + 비밀번호 | 내부 시스템, 간단한 관리 |
| 토큰 방식 | JWT (Access + Refresh) | Stateless, Spring Security 표준 |
| Access Token 만료 | 1시간 | 보안/편의성 균형 |
| Refresh Token 만료 | 7일 | 자동 갱신 지원 |
| Refresh Token 저장 | DB (`refresh_tokens` 테이블) | 무효화(로그아웃) 지원 |
| 비밀번호 해싱 | BCrypt | Spring Security 기본 지원 |

---

## Features

### 인증
- [ ] 로그인 (이메일 + 비밀번호 → Access/Refresh Token 발급)
- [ ] 토큰 갱신 (Refresh Token → 새 Access Token 발급)
- [ ] 로그아웃 (Refresh Token 무효화)
- [ ] 내 정보 조회 (`GET /api/auth/me`)

### 사용자 관리 (기존 users 테이블 확장)
- [ ] 회원가입 — 최초 관리자 등록 또는 관리자가 사용자 생성
- [ ] 비밀번호 변경
- [ ] 사용자 역할: `ADMIN` / `USER` (추후 권한 분기용)

### Spring Security 적용
- [ ] 공개 엔드포인트: `POST /api/auth/login`, `POST /api/auth/refresh`
- [ ] 인증 필요: 나머지 모든 `/api/**`
- [ ] JWT 필터: `Authorization: Bearer <token>` 검증

---

## Database Schema

### `users` 테이블 확장 (folder plan 설계 기반)

```sql
CREATE TABLE users (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(50)  NOT NULL,
  email        VARCHAR(100) NOT NULL UNIQUE,
  password     VARCHAR(255) NOT NULL,             -- BCrypt 해시
  department   VARCHAR(100),
  role         VARCHAR(20)  NOT NULL DEFAULT 'USER',  -- ENUM: ADMIN | USER
  created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### `refresh_tokens` — Refresh Token 저장 (로그아웃/무효화용)

```sql
CREATE TABLE refresh_tokens (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  token       VARCHAR(512) NOT NULL UNIQUE,
  expires_at  TIMESTAMP NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### ERD 추가
```
users (1) ──── (N) refresh_tokens
users (1) ──── (N:M) folders  [via folder_members]
```

---

## API Design

### Auth API — `/api/auth`

| Method | URL | 인증 | 설명 |
|--------|-----|------|------|
| POST | `/api/auth/login` | 불필요 | 로그인 → Access/Refresh Token 발급 |
| POST | `/api/auth/refresh` | 불필요 | Access Token 갱신 |
| POST | `/api/auth/logout` | 필요 | Refresh Token 무효화 |
| GET | `/api/auth/me` | 필요 | 현재 로그인 사용자 정보 |

**POST /api/auth/login 요청**
```json
{
  "email": "hong@example.com",
  "password": "plaintext"
}
```

**POST /api/auth/login 응답 (200)**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "expiresIn": 3600,
  "user": {
    "id": 1,
    "name": "홍길동",
    "email": "hong@example.com",
    "department": "개발팀",
    "role": "USER"
  }
}
```

**POST /api/auth/refresh 요청**
```json
{ "refreshToken": "eyJhbGci..." }
```

**POST /api/auth/refresh 응답 (200)**
```json
{
  "accessToken": "eyJhbGci...",
  "expiresIn": 3600
}
```

**POST /api/auth/logout 요청**
```json
{ "refreshToken": "eyJhbGci..." }
```

**GET /api/auth/me 응답 (200)**
```json
{
  "id": 1,
  "name": "홍길동",
  "email": "hong@example.com",
  "department": "개발팀",
  "role": "USER"
}
```

### 오류 응답

| 상황 | HTTP | 코드 |
|------|------|------|
| 이메일 없음 / 비밀번호 불일치 | 401 | `INVALID_CREDENTIALS` |
| Access Token 만료 | 401 | `TOKEN_EXPIRED` |
| Access Token 변조 | 401 | `INVALID_TOKEN` |
| Refresh Token 만료/무효 | 401 | `REFRESH_TOKEN_INVALID` |

---

## Backend 구현 — Hexagonal 구조

### 의존성 추가 (`build.gradle.kts`)

```kotlin
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("io.jsonwebtoken:jjwt-api:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
```

### 모듈 구조 (`modules/auth/`)

```
modules/
├── user/                          # 사용자 관리 (folder plan에서 분리)
│   ├── domain/model/User.java     # id, name, email, password, department, role
│   ├── domain/port/in/UserRegisterUseCase, UserSearchUseCase, UserPasswordChangeUseCase
│   ├── domain/port/out/UserCommandPort, UserQueryPort
│   ├── application/service/...
│   └── adapter/
│       ├── in/web/UserController  # GET /api/users, POST /api/users, DELETE /api/users/{id}
│       └── out/persistence/UserJpaEntity, UserJpaRepository, UserPersistenceAdapter
│
└── auth/                          # 인증 (신규)
    ├── domain/model/TokenPair.java       # accessToken, refreshToken, expiresIn
    ├── domain/port/in/LoginUseCase, LogoutUseCase, TokenRefreshUseCase
    ├── domain/port/out/TokenPort, RefreshTokenPort
    ├── application/service/LoginService, TokenRefreshService, LogoutService
    └── adapter/
        ├── in/web/AuthController          # POST /api/auth/login, refresh, logout, GET /api/auth/me
        ├── out/jwt/JwtTokenAdapter        # JWT 생성/검증
        └── out/persistence/RefreshTokenJpaEntity, RefreshTokenPersistenceAdapter
```

### Spring Security 설정 (`common/config/SecurityConfig.java`)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(POST, "/api/auth/login").permitAll()
                .requestMatchers(POST, "/api/auth/refresh").permitAll()
                .requestMatchers("/api/**").authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### JWT 설정 (`application.yml` 추가)

```yaml
jwt:
  secret: ${JWT_SECRET:local-dev-secret-key-min-32-chars}
  access-token-expiry: 3600      # 1시간 (초)
  refresh-token-expiry: 604800   # 7일 (초)
```

---

## Frontend 구현

### 신규 파일

```
src/
├── pages/LoginPage.tsx              # 로그인 폼 (이메일, 비밀번호)
├── services/authApi.ts              # login, refresh, logout, me API
├── stores/useAuthStore.ts           # Zustand: user, accessToken, 로그인/로그아웃 액션
├── hooks/useAuth.ts                 # 로그인 상태 체크, 리디렉션
└── components/common/ProtectedRoute.tsx  # 인증 필요 라우트 가드
```

### 토큰 관리 전략

| 토큰 | 저장 위치 | 이유 |
|------|-----------|------|
| Access Token | Zustand (메모리) | XSS 방지 (localStorage 저장 금지) |
| Refresh Token | `httpOnly` Cookie | XSS 방지, 자동 전송 |

> Refresh Token은 서버에서 `Set-Cookie: refreshToken=...; HttpOnly; Secure; SameSite=Strict` 응답

### Axios 인터셉터 (`services/axios.ts` 수정)

```typescript
// 요청 인터셉터: Access Token 자동 주입
axiosInstance.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 응답 인터셉터: 401 시 Refresh Token으로 재발급 후 재시도
axiosInstance.interceptors.response.use(null, async (error) => {
  if (error.response?.status === 401 && !error.config._retry) {
    error.config._retry = true;
    const newToken = await authApi.refresh();     // cookie의 refreshToken 자동 포함
    useAuthStore.getState().setAccessToken(newToken);
    error.config.headers.Authorization = `Bearer ${newToken}`;
    return axiosInstance(error.config);
  }
  return Promise.reject(error);
});
```

### 라우팅 구조 (`App.tsx`)

```tsx
<Routes>
  <Route path="/login" element={<LoginPage />} />
  <Route element={<ProtectedRoute />}>
    <Route path="/" element={<ReportDashboard />} />
    {/* 기타 인증 필요 라우트 */}
  </Route>
</Routes>
```

### Zustand Auth Store (`stores/useAuthStore.ts`)

```typescript
interface AuthStore {
  user: User | null;
  accessToken: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  setAccessToken: (token: string) => void;
}
```

---

## UI/UX Plan

### 로그인 페이지

```
┌─────────────────────────────────────┐
│          work-log-ai                │
│                                     │
│  이메일  [____________________]     │
│  비밀번호 [____________________]    │
│                                     │
│         [  로그인  ]                │
│                                     │
│  ─────────── 또는 ──────────        │
│  [ GitLab으로 계속 ]                │
└─────────────────────────────────────┘
```

- 로그인 실패 시 "이메일 또는 비밀번호가 올바르지 않습니다." 표시
- 로딩 중 버튼 비활성화

### 헤더 사용자 표시 (Header.tsx 수정)

```
현재: "사용자" (하드코딩)
변경: "[홍길동]  [로그아웃]"
```

---

## Implementation Order

### Phase 1 — Backend
1. `build.gradle.kts`에 Spring Security + JJWT 의존성 추가
2. `users` 테이블에 `password`, `role`, `updated_at` 컬럼 추가
3. `refresh_tokens` 테이블 신규 생성
4. `modules/user/` 도메인 모듈 생성 (UserJpaEntity, CRUD)
5. `modules/auth/` 도메인 모듈 생성
   - `JwtTokenAdapter` — Access/Refresh Token 생성/검증
   - `LoginService`, `LogoutService`, `TokenRefreshService`
   - `AuthController` — login, refresh, logout, me 엔드포인트
6. `common/config/SecurityConfig.java` — JWT 필터, 공개/인증 라우트 설정

### Phase 2 — Frontend
1. `react-router-dom` 설치 (미설치 시)
2. `stores/useAuthStore.ts` — 인증 상태 관리
3. `services/authApi.ts` — login, refresh, logout, me
4. `services/axios.ts` 수정 — 토큰 주입 + 자동 갱신 인터셉터
5. `pages/LoginPage.tsx` — 로그인 폼
6. `components/common/ProtectedRoute.tsx` — 인증 가드
7. `App.tsx` 수정 — 라우팅 구성
8. `Header.tsx` 수정 — 사용자명 + 로그아웃 버튼

### Phase 3 — 연동 테스트
1. 로그인 → 토큰 발급 → API 요청 흐름 확인
2. Access Token 만료 → 자동 갱신 → API 재시도 확인
3. 로그아웃 → Refresh Token 무효화 → 재사용 차단 확인
4. 미인증 상태에서 보호 라우트 접근 시 `/login` 리디렉션 확인

---

## 초기 데이터 (Seed)

최초 관리자 계정은 애플리케이션 기동 시 `data.sql` 또는 `DataInitializer`로 자동 삽입한다.  
초기 비밀번호 `hubilon1!` 는 BCrypt 해시로 저장한다.

```sql
INSERT INTO users (name, email, password, role) VALUES
  ('이경희', 'khlee@hubilon.com',       '$2a$10$<bcrypt(hubilon1!)>', 'ADMIN'),
  ('이건',   'geonlee@hubilon.com',     '$2a$10$<bcrypt(hubilon1!)>', 'ADMIN'),
  ('유민기', 'mouseyk@hubilon.com',     '$2a$10$<bcrypt(hubilon1!)>', 'ADMIN'),
  ('최보경', 'bokyeong0113@hubilon.com','$2a$10$<bcrypt(hubilon1!)>', 'ADMIN'),
  ('최기성', 'gsuchoi@hubilon.com',     '$2a$10$<bcrypt(hubilon1!)>', 'ADMIN'),
  ('이수연', 'suyeon129@hubilon.com',   '$2a$10$<bcrypt(hubilon1!)>', 'ADMIN'),
  ('김미진', 'mijinkim@hubilon.com',    '$2a$10$<bcrypt(hubilon1!)>', 'ADMIN');
```

> 실제 BCrypt 해시는 `DataInitializer.java`에서 `passwordEncoder.encode("hubilon1!")` 로 생성하여 삽입한다.  
> SQL에 평문 또는 하드코딩 해시를 넣지 않고, 코드에서 런타임 해싱 후 `existsByEmail` 체크를 통해 중복 삽입을 방지한다.

**DataInitializer 구현 방식 (추천)**
```java
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserCommandPort userCommandPort;
    private final UserQueryPort   userQueryPort;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PASSWORD = "hubilon1!";
    private static final List<String[]> INIT_ADMINS = List.of(
        new String[]{"이경희", "khlee@hubilon.com"},
        new String[]{"이건",   "geonlee@hubilon.com"},
        new String[]{"유민기", "mouseyk@hubilon.com"},
        new String[]{"최보경", "bokyeong0113@hubilon.com"},
        new String[]{"최기성", "gsuchoi@hubilon.com"},
        new String[]{"이수연", "suyeon129@hubilon.com"},
        new String[]{"김미진", "mijinkim@hubilon.com"}
    );

    @Override
    public void run(ApplicationArguments args) {
        INIT_ADMINS.forEach(admin -> {
            if (!userQueryPort.existsByEmail(admin[1])) {
                userCommandPort.save(User.of(
                    admin[0], admin[1],
                    passwordEncoder.encode(DEFAULT_PASSWORD),
                    Role.ADMIN
                ));
            }
        });
    }
}
```

## 미결 사항

- **최초 관리자 계정 생성 방법**: ✅ 확정 — `DataInitializer`로 기동 시 자동 삽입, 이메일 중복 체크로 멱등 보장
- **비밀번호 초기화**: 이메일 발송 없이 관리자가 임시 비밀번호 재설정하는 방식으로 단순화 고려
- **GitLab OAuth와 통합**: GitLab 계정으로 로그인한 사용자를 `users` 테이블과 연동할지 추후 결정
- **CORS 설정**: Refresh Token Cookie의 `SameSite=Strict`이 프론트-백 도메인 분리 환경에서 동작하는지 확인 필요
