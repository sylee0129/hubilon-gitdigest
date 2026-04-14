# 회원가입 추가 + 팀 정보 + 초기 데이터 마이그레이션

## Context
로그인 화면에 회원가입 기능을 추가하고, 회원가입 시 팀(department) 정보를 입력받는다.
기존 사용자들의 department를 '플랫폼개발팀'으로 일괄 업데이트한다.

---

## 현황 분석

### 기존 구조
- `POST /api/users` 회원가입 API 이미 존재 (UserRegisterRequest: name, email, password, department)
- 하지만 SecurityConfig에서 해당 URL이 **인증 필요**로 막혀 있음 → 수정 필요
- User 엔티티에 `department` 필드 존재 (nullable) → 팀 정보로 활용
- 프론트엔드에 회원가입 페이지 없음, authApi에 register 함수 없음

---

## 구현 계획

### Backend (2개 변경)

#### 1. SecurityConfig — `POST /api/users` 허용
**파일**: `backend/src/main/java/com/hubilon/common/config/SecurityConfig.java`
- `.requestMatchers(HttpMethod.POST, "/api/users").permitAll()` 추가

#### 2. 초기 데이터 마이그레이션 Bean
**새 파일**: `backend/src/main/java/com/hubilon/common/config/DataInitializer.java`
- `ApplicationRunner` 구현
- 시작 시 `UPDATE users SET department = '플랫폼개발팀' WHERE department IS NULL` 실행
- `JdbcTemplate` 사용

---

### Frontend (4개 변경)

#### 1. authApi.ts — register 함수 추가
**파일**: `frontend/src/services/authApi.ts`
```ts
interface RegisterRequest {
  name: string
  email: string
  password: string
  department: string
}
register: async (data: RegisterRequest): Promise<void>
// POST /api/users
```

#### 2. SignupPage.tsx — 새 페이지 생성
**새 파일**: `frontend/src/pages/SignupPage.tsx`
- 입력 필드: 이름, 이메일, 비밀번호, 팀(department)
- 팀은 free-text input (필수 입력)
- 성공 시 `/login`으로 이동, 성공 메시지 표시
- 에러 처리 (이메일 중복 등)
- LoginPage와 동일한 카드 레이아웃 스타일 재사용 (`LoginPage.module.css`)

#### 3. App.tsx — `/signup` 라우트 추가
**파일**: `frontend/src/App.tsx`
- `<Route path="/signup" element={<SignupPage />} />` 추가 (인증 불필요)

#### 4. LoginPage.tsx — 회원가입 링크 추가
**파일**: `frontend/src/pages/LoginPage.tsx`
- 로그인 버튼 하단에 "계정이 없으신가요? **회원가입**" 링크 추가
- `<Link to="/signup">` 사용

---

## 핵심 파일 경로

| 파일 | 변경 유형 |
|------|---------|
| `backend/.../common/config/SecurityConfig.java` | 수정 |
| `backend/.../common/config/DataInitializer.java` | 신규 |
| `frontend/src/services/authApi.ts` | 수정 |
| `frontend/src/pages/SignupPage.tsx` | 신규 |
| `frontend/src/App.tsx` | 수정 |
| `frontend/src/pages/LoginPage.tsx` | 수정 |

---

## Review 결과
- 검토일: 2026-04-14
- 검토 항목: 보안 / 리팩토링 / 기능
- 결과: 이상 없음 (기존 코드에서 대부분 처리됨, department 필수 입력으로 결정)

---

## 검증

1. 백엔드 기동 → 기존 사용자 department 확인 (H2 콘솔)
2. `/signup` 접근 → 폼 렌더링 확인
3. 회원가입 완료 → `/login` 리다이렉트
4. 신규 계정으로 로그인 성공
5. 로그인 화면에서 "회원가입" 링크 클릭 동작 확인
