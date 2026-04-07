# 관리자 로그인 페이지 구현 계획

## Context
관리자 전용 로그인 페이지를 `/hub-adm` URI에 구현한다. 일반 사용자 로그인(`/login`)과 별도로 운영하며, ROLE_ADMIN 권한을 가진 사용자만 로그인 가능하다. 기존 일반 로그인 흐름은 변경하지 않는다.

---

## 구현 범위

### 백엔드 (5개 파일)

#### 신규 파일 3개

**1. `AuthAdminLoginUseCase.java`**
- 위치: `modules/auth/domain/port/in/`
- 기존 `AuthLoginCommand` / `AuthLoginResult` DTO 재사용
```java
public interface AuthAdminLoginUseCase {
    AuthLoginResult adminLogin(AuthLoginCommand command);
}
```

**2. `AuthAdminLoginService.java`**
- 위치: `modules/auth/application/service/command/`
- `AuthLoginService` 로직과 동일하되, 비밀번호 검증 후 `ROLE_ADMIN` 체크 추가
- 비관리자일 때 에러 메시지는 계정 존재 여부 노출 방지를 위해 동일 문구 사용:
  ```java
  if (!"ROLE_ADMIN".equals(user.getRole())) {
      throw new UnauthorizedException("아이디 또는 비밀번호가 올바르지 않습니다.");
  }
  ```
- 기존 `AuthLoginService` 의존성 주입 구조 동일하게 따름 (`@Service`, `@RequiredArgsConstructor`, `@Slf4j`, `@Transactional`)

**3. `AuthAdminLoginRequest.java`**
- 위치: `modules/auth/adapter/in/web/`
- 기존 `AuthLoginRequest`와 동일 필드의 별도 record:
  ```java
  public record AuthAdminLoginRequest(
      @NotBlank String username,
      @NotBlank String encryptedPassword,
      @NotBlank String encryptedAesKey,
      @NotBlank String keyId
  ) {}
  ```

#### 수정 파일 2개

**4. `AuthController.java`**
- 위치: `modules/auth/adapter/in/web/`
- `AuthAdminLoginUseCase` 필드 추가 (`@RequiredArgsConstructor`로 자동 주입)
- 아래 엔드포인트 추가 (기존 `login()` 메서드의 쿠키 설정 로직과 동일):
  ```java
  @PostMapping("/admin/login")
  @Operation(summary = "관리자 로그인", description = "ROLE_ADMIN 권한 사용자 전용 로그인")
  public ApiResponse<AuthLoginResponse> adminLogin(
          @Valid @RequestBody AuthAdminLoginRequest request,
          HttpServletResponse response) {
      AuthLoginResult result = authAdminLoginUseCase.adminLogin(new AuthLoginCommand(
              request.username(), request.encryptedPassword(),
              request.encryptedAesKey(), request.keyId()));
      // 기존 login()의 Cookie 설정 로직 동일하게 적용
      ...
      return ApiResponse.ok(AuthLoginResponse.from(result));
  }
  ```

**5. `SecurityConfig.java`**
- 위치: `global/config/`
- permitAll 목록에 `/api/v1/auth/admin/login` 추가

---

### 프론트엔드 (5개 파일)

#### 신규 파일 3개

**1. `src/hooks/useAdminLogin.ts`**
- `useLogin.ts`와 동일한 구조, `authService.adminLogin()` 호출
- 응답의 `role !== 'ROLE_ADMIN'` 이면 에러 처리 (프론트 2차 방어선)

**2. `src/pages/AdminLoginPage.tsx`**
- `LoginPage.tsx`와 동일한 구조
- `useAdminLogin` 훅 사용
- 로그인 성공 후 `/admin/users`로 이동 (`navigate('/admin/users')`)
- 헤더 문구: `"Administrator Portal"`, subtitle: `"Admin access only"`
- 비밀번호 변경 필요 시 기존 LoginPage와 동일하게 처리
- `console.log` 사용 금지 (CLAUDE.md 규칙)
- 로그인 전 기존 localStorage 토큰 클리어:
  ```tsx
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('userInfo')
  ```

**3. `src/pages/AdminLoginPage.module.css`**
- `LoginPage.module.css`와 동일한 구조 (glassmorphism 유지)
- 배경 그라디언트를 블루-슬레이트 계열로 변경해 일반 로그인과 시각적 구분:
  ```css
  /* bgGradient */
  background: radial-gradient(circle at 30% 40%, #dbeafe 0%, #f8faff 60%, #eff6ff 100%);
  ```
- `.adminBadge` 클래스 추가: 작은 배지로 "Administrator" 텍스트 표시

#### 수정 파일 2개

**4. `src/services/authService.ts`**
- `adminLogin` 메서드 추가:
  ```typescript
  adminLogin: async (request: LoginRequest): Promise<LoginResponse> => {
    const response = await api.post<LoginResponse>('/auth/admin/login', request)
    return response.data
  }
  ```

**5. `src/App.tsx`**
- `AdminLoginPage` import 추가
- `AdminProtectedRoute` 컴포넌트 추가 (token + ROLE_ADMIN 검증, 실패 시 `/hub-adm` 리다이렉트):
  ```tsx
  function AdminProtectedRoute({ children }: { children: React.ReactNode }) {
    const token = localStorage.getItem('token')
    if (!token) return <Navigate to="/hub-adm" replace />
    try {
      const userInfo = JSON.parse(localStorage.getItem('userInfo') ?? '{}')
      if (userInfo.role !== 'ROLE_ADMIN') return <Navigate to="/login" replace />
    } catch {
      return <Navigate to="/hub-adm" replace />
    }
    return <>{children}</>
  }
  ```
- `/hub-adm` 라우트 추가 (인증 불필요):
  ```tsx
  <Route path="/hub-adm" element={<AdminLoginPage />} />
  ```
- `/admin/users` 라우트의 `ProtectedRoute` → `AdminProtectedRoute` 교체

---

## 구현 순서

백엔드 → 프론트엔드 순으로 진행 (API 엔드포인트 먼저 확보)

1. `AuthAdminLoginUseCase.java`
2. `AuthAdminLoginService.java`
3. `AuthAdminLoginRequest.java`
4. `AuthController.java` 수정
5. `SecurityConfig.java` 수정
6. `authService.ts` 수정
7. `useAdminLogin.ts`
8. `AdminLoginPage.tsx` + `AdminLoginPage.module.css`
9. `App.tsx` 수정

---

## 검증 방법

1. 백엔드 서버 기동 후 Swagger(`/swagger-ui/index.html`)에서 `POST /api/v1/auth/admin/login` 엔드포인트 확인
2. ROLE_ADMIN 계정으로 `/hub-adm` 로그인 → `/admin/users` 이동 확인
3. ROLE_USER 계정으로 `/hub-adm` 로그인 → 에러 메시지 표시 확인
4. `/hub-adm`에서 로그인 후 기존 일반 세션 토큰이 클리어됨을 localStorage에서 확인
5. 토큰 없이 `/admin/users` 직접 접근 → `/hub-adm`으로 리다이렉트 확인
6. 일반 사용자 토큰으로 `/admin/users` 접근 → `/login`으로 리다이렉트 확인
