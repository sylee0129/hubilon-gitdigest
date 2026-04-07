# My Profile 기능 구현 계획

## 개요

우측 상단 Navbar에서 사용자 성(이니셜 아바타) 클릭 시 드롭다운이 열리고,
"My Profile" 클릭 시 프로필 모달을 표시한다.

---

## 현재 상태

- `Navbar.tsx`: 드롭다운 UI 존재, "My Profile" 버튼 미연결
- `GET /api/v1/auth/me`: 현재 사용자 정보 조회 API 존재
- `POST /api/v1/auth/change-password`: 비밀번호 변경 API 존재

---

## 작업 범위

### Backend (확인 및 보완)
- `GET /api/v1/auth/me` 응답에 프로필 표시에 필요한 필드 확인
  - 필요 필드: `id`, `name`, `email`, `username`, `department`, `position`, `phoneNumber`
  - 누락 필드 있으면 응답 DTO에 추가
- `POST /api/v1/auth/change-password` API가 RSA+AES 암호화된 비밀번호를 수신하는지 확인
  - 로그인과 동일한 암호화 방식 적용 (RSA 공개키로 AES키 암호화 → AES로 비밀번호 암호화)
  - 미적용 시 암호화 처리 추가

### Frontend (주 작업)

1. **`authService.ts`** 수정
   - `getMyProfile()` 함수 추가 → `GET /api/v1/auth/me` 호출
   - `changePassword()` 함수 추가 → 로그인과 동일한 RSA+AES 암호화 적용 후 `POST /api/v1/auth/change-password` 호출

2. **`MyProfileModal.tsx`** 신규 생성
   - 사용자 정보 표시: 이름, 이메일, 아이디, 부서, 직급, 연락처
   - `Navbar`에 이미 사용자 정보가 있으면 재사용, 없으면 `getMyProfile()` 호출
   - 비밀번호 변경 섹션 포함
   - 공통 `Modal.tsx` 컴포넌트 활용

3. **`Navbar.tsx`** 수정
   - "My Profile" 버튼 클릭 시 `MyProfileModal` 표시 연결

---

## 화면 구성 (My Profile 모달)

```
┌─────────────────────────────┐
│  My Profile            [X]  │
├─────────────────────────────┤
│  [아바타/이니셜]             │
│  홍길동                      │
│  hong@hubilon.com           │
├─────────────────────────────┤
│  아이디    hong              │
│  부서      개발팀             │
│  직급      대리              │
│  연락처    010-0000-0000     │
├─────────────────────────────┤
│  비밀번호 변경                │
│  현재 비밀번호  [          ]  │
│  새 비밀번호   [          ]  │
│  비밀번호 확인 [          ]  │
│                  [변경하기]  │
└─────────────────────────────┘
```

---

## 비밀번호 변경 정책

- **암호화**: 로그인과 동일한 RSA+AES Hybrid 암호화 적용
  - `GET /api/v1/auth/public-key` → RSA 공개키 발급 (일회성)
  - AES 키 생성 → RSA로 AES 키 암호화 → AES로 비밀번호 암호화
- **유효성 검사 (프론트엔드)**:
  - 새 비밀번호와 확인 비밀번호 일치 여부
  - 최소 8자 이상, 영문+숫자+특수문자 조합 (백엔드 정책 확인 후 맞춤)
- **비밀번호 입력 필드 속성**:
  - 현재 비밀번호: `autocomplete="current-password"`
  - 새 비밀번호: `autocomplete="new-password"`
  - 비밀번호 확인: `autocomplete="new-password"`
- **성공 처리**:
  1. 토스트/알림 메시지 표시: `"비밀번호가 변경되었습니다. 잠시 후 로그인 페이지로 이동합니다."`
  2. 2초 후 localStorage 토큰 및 사용자 정보 삭제 후 `/login` 페이지로 이동
- **실패 처리**: 에러 메시지 인라인 표시 (예: "현재 비밀번호가 올바르지 않습니다.")

---

## 작업 순서

1. BE: `GET /api/v1/auth/me` 응답 필드 확인 및 보완, `POST /api/v1/auth/change-password` 암호화 처리 확인
2. FE: `authService.ts`에 `getMyProfile()`, `changePassword()` 추가
3. FE: `MyProfileModal.tsx` 컴포넌트 구현
4. FE: `Navbar.tsx` 연결

---

## 비고

- 프로필 편집 기능은 이번 범위에서 제외 (조회만)

---

## Review 결과
- 검토일: 2026-04-06
- 검토 항목: 보안 / 리팩토링 / 기능
- 주요 이슈: 비밀번호 암호화 정책, 변경 후 로그아웃 처리 → 반영 완료
- 결과: 이상 없음 (구현 진행)
