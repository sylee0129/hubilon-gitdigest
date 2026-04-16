# Hubilon GitDigest

GitLab 커밋 이력을 기반으로 AI 주간보고를 자동 생성하고 Confluence에 업로드하는 팀 업무 관리 도구입니다.

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| Backend | Java 25, Spring Boot 4.0, Spring Security, JPA |
| Database | MariaDB, Flyway |
| Frontend | React 19, Vite, TypeScript, Zustand, React Query |
| AI | Google Gemini 2.5 Flash |
| 외부 연동 | GitLab OAuth, Confluence Cloud REST API |
| 스케줄러 | Spring Scheduler + ShedLock |

---

## 핵심 기능

### GitLab 커밋 기반 보고서

GitLab 프로젝트의 커밋 이력을 조회하여 주차별·프로젝트별로 시각화합니다.

- GitLab OAuth 로그인으로 인증
- 프로젝트 등록 후 주차 선택 → 해당 기간의 커밋 목록 자동 조회
- 폴더(사업 단위)로 프로젝트를 묶어 사업 단위 현황 파악

### AI 요약 (Gemini API)

커밋 메시지를 Gemini 2.5 Flash 모델에 전달하여 사람이 읽기 좋은 업무 요약문을 자동 생성합니다.

- 커밋 목록 → Gemini API → 자연어 요약
- 생성된 요약은 수동 편집 가능
- `ai.summary.enabled` 설정으로 기능 on/off 제어
- API 키: `GEMINI_API_KEY` 환경변수

### Confluence 업로드

생성된 주간보고를 Atlassian Confluence 페이지로 직접 업로드합니다.

- 사업 폴더 단위로 주간보고 페이지 자동 생성/갱신
- Confluence Cloud REST API v2 사용
- 업로드 대상 스페이스·상위 페이지를 환경변수로 설정
- 헤더의 **Confluence 업로드** 버튼으로 수동 실행

### 주간보고 스케줄러

매주 목요일 오후 7시에 전체 진행 중인 사업 폴더의 주간보고를 자동으로 Confluence에 업로드합니다.

- 실행 주기: `0 0 19 * * THU` (Asia/Seoul)
- 분산 환경 중복 실행 방지: ShedLock 적용 (lockAtMost 2시간)
- 실행 이력(시작 시각, 상태, 폴더별 성공/실패)을 DB에 저장
- **수동 실행** 버튼으로 즉시 트리거 가능
- `scheduler.weekly-report.enabled` 설정으로 자동 실행 on/off 제어

---

## 환경변수

### Backend

```env
# DB
DB_URL=jdbc:mariadb://localhost:3306/worklog
DB_USERNAME=root
DB_PASSWORD=

# GitLab OAuth
GITLAB_OAUTH_CLIENT_ID=
GITLAB_OAUTH_CLIENT_SECRET=
GITLAB_OAUTH_REDIRECT_URI=http://localhost:8080/api/oauth/gitlab/callback
FRONTEND_ORIGIN=http://localhost:3000

# JWT
JWT_SECRET=                          # 32자 이상

# Gemini AI
GEMINI_API_KEY=

# Confluence
CONFLUENCE_BASE_URL=https://<your-domain>.atlassian.net
CONFLUENCE_USER_EMAIL=
CONFLUENCE_API_TOKEN=
CONFLUENCE_SPACE_KEY=
CONFLUENCE_PARENT_PAGE_ID=
```

### Frontend

```env
VITE_API_URL=http://localhost:8080/api
```

---

## 로컬 실행

```bash
# Backend
cd backend
./gradlew bootRun

# Frontend
cd frontend
npm install
npm run dev
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

---

## 프로젝트 구조

```
work-log-ai/
├── backend/
│   └── src/main/java/com/hubilon/
│       ├── modules/
│       │   ├── auth/          # JWT 인증
│       │   ├── project/       # GitLab 프로젝트 관리
│       │   ├── folder/        # 사업 폴더 관리
│       │   ├── report/        # 커밋 조회 + AI 요약
│       │   ├── confluence/    # Confluence 업로드
│       │   └── scheduler/     # 주간보고 자동화
│       └── common/            # 공통 설정, 예외 처리
└── frontend/
    └── src/
        ├── components/        # 공통·도메인별 컴포넌트
        ├── pages/             # 라우트 페이지
        ├── hooks/             # 커스텀 훅
        ├── services/          # API 호출
        └── stores/            # Zustand 전역 상태
```
