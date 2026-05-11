# Keycloak SSO 연동 및 배포 설정 변경 내역

**작성일**: 2026-05-11  
**작업 브랜치**: master  
**관련 커밋**: `cc21a11` → `5206b9b` → `647a65a` → `c97bc0f`

---

## 1. 변경 배경

기존 커스텀 JWT 인증(자체 로그인/회원가입) 방식을 사내 Keycloak SSO 인증으로 전환.  
사내 계정 체계 통합 및 중앙 인증 관리 목적.

---

## 2. 인증 방식 변경 요약

| 구분 | Before | After |
|------|--------|-------|
| 인증 방식 | 자체 JWT (Access + Refresh Token) | Keycloak SSO (쿠키 기반 세션) |
| 로그인 화면 | 자체 LoginPage / SignupPage | Keycloak 리다이렉트 |
| 토큰 저장 | localStorage | HttpOnly 쿠키 |
| 사용자 생성 | 수동 가입 | JIT Provisioning (첫 로그인 자동 생성) |
| Spring Boot | 4.0.5 | 3.4.5 (라이브러리 호환성 다운그레이드) |

---

## 3. 배포 설정 파일 변경

### 3-1. `deploy/backend/application-dev.yml` — Keycloak 설정 추가

```yaml
keycloak:
  server-url: ${KEYCLOAK_SERVER_URL:http://192.168.10.30:8080}
  realm: ${KEYCLOAK_REALM:hubilon_pd}
  client-id: ${KEYCLOAK_CLIENT_ID:hubilon-gitdigest}
  client-secret: ${KEYCLOAK_CLIENT_SECRET:VAFlVOoN5vPz6gpqeARhAuvtN81TuYuK}
  redirect-uri: ${KEYCLOAK_REDIRECT_URI:http://192.168.10.30:3300/auth/callback}
  post-logout-redirect-uri: ${KEYCLOAK_POST_LOGOUT_REDIRECT_URI:http://192.168.10.30:3300/auth/login}
  post-login-redirect-uri: ${KEYCLOAK_POST_LOGIN_REDIRECT_URI:http://192.168.10.30:3300}
  secure-cookie: ${KEYCLOAK_SECURE_COOKIE:false}
```

- 기본값은 개발서버(`192.168.10.30`) 기준으로 설정
- 운영 환경은 `.env`에서 환경변수로 오버라이드

**포트 선택 기준**

| 설정 키 | 포트 | 이유 |
|---------|------|------|
| `gitlab.oauth.redirect-uri` | 8090 | OAuth 콜백을 백엔드가 직접 처리 — Nginx 불필요 |
| `github.oauth.redirect-uri` | 8090 | 동일 |
| `keycloak.redirect-uri` | 3300 | Keycloak 인증 완료 후 **브라우저**가 이동하는 주소 → Nginx(3300)를 거쳐 백엔드로 프록시 |
| `keycloak.post-logout-redirect-uri` | 3300 | 로그아웃 후 **브라우저**가 이동할 프론트엔드 페이지 |
| `keycloak.post-login-redirect-uri` | 3300 | 로그인 성공 후 프론트엔드 루트 |

> Keycloak redirect-uri에 3300을 쓰는 이유:
> 1. Keycloak Admin의 `Valid Redirect URIs`에 등록된 값과 정확히 일치해야 하며, 현재 `3300/auth/callback`으로 등록되어 있음
> 2. 운영 환경에서 8090을 닫고 Nginx(3300)만 외부에 노출하는 구성을 고려 — 처음부터 3300으로 통일하면 이후 변경 불필요
> 3. `secure-cookie`는 HTTP 개발환경에서는 `false`, HTTPS 운영환경에서는 `true`로 변경 필요

### 3-2. `deploy/backend/.env` — Keycloak 환경변수 (현재 주석 처리)

```bash
# Keycloak dev (application-dev.yml 기본값 사용 중 → 필요 시 주석 해제)
# KEYCLOAK_SERVER_URL=http://192.168.10.30:8080
# KEYCLOAK_REALM=hubilon_pd
# KEYCLOAK_CLIENT_ID=hubilon-gitdigest
# KEYCLOAK_CLIENT_SECRET=VAFlVOoN5vPz6gpqeARhAuvtN81TuYuK
# KEYCLOAK_REDIRECT_URI=http://192.168.10.30:3300/auth/callback
# KEYCLOAK_POST_LOGOUT_REDIRECT_URI=http://192.168.10.30:3300/auth/login
# KEYCLOAK_POST_LOGIN_REDIRECT_URI=http://192.168.10.30:3300
# KEYCLOAK_SECURE_COOKIE=false
```

> 현재 개발환경은 `application-dev.yml` 기본값을 사용.  
> 값 변경이 필요한 경우 `.env`에서 주석 해제 후 값 수정.

### 3-3. `deploy/nginx.conf` — `/auth/` 경로 프록시 추가

```nginx
# Keycloak 인증 콜백 경로 프록시
location /auth/ {
    proxy_pass http://backend:8090/auth/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

- Keycloak 로그인 콜백(`/auth/callback`), 로그아웃(`/auth/logout`) 경로를 백엔드로 프록시
- 기존 `/api/` 프록시와 동일한 구조

### 3-4. `deploy/docker-compose.yml` — Docker 기반 배포 환경 신규 추가

```yaml
services:
  backend:
    image: eclipse-temurin:17-jre-alpine
    volumes:
      - ./backend:/app
    command:
      - java
      - -Dspring.profiles.active=dev
      - -jar
      - /app/work-log-ai-0.0.1-SNAPSHOT.jar
      - --spring.config.additional-location=file:/app/
    env_file: ./backend/.env
    ports:
      - "8090:8090"
    restart: unless-stopped

  frontend:
    image: nginx:alpine
    volumes:
      - ./frontend/dist:/usr/share/nginx/html:ro
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
    ports:
      - "3300:80"
    restart: unless-stopped
```

- 기존 프로세스 직접 실행 방식 → Docker Compose 방식으로 전환
- `env_file`로 `.env` 주입, 설정 파일은 볼륨으로 마운트

---

## 4. 서버 디렉토리 구조 (배포 후)

```
/home/hubilon-gitdigest/
├── docker-compose.yml
├── nginx.conf
├── backend/
│   ├── work-log-ai-0.0.1-SNAPSHOT.jar
│   ├── .env                  ← 민감 환경변수 (Keycloak 포함)
│   └── application-dev.yml   ← Keycloak 기본 설정
└── frontend/
    └── dist/
```

---

## 5. JIT Provisioning 로직 (`UserProvisioningService`)

Keycloak 첫 로그인 시 JWT claims를 파싱하여 DB에 사용자 자동 생성.

| JWT Claim | 매핑 |
|-----------|------|
| `preferred_username` | `keycloak_username` (신규 컬럼) |
| `given_name` + `family_name` | `name` |
| `realm_access.roles` / `resource_access.<client>.roles` | `role` |
| `department` (3depth 이상 조직 경로) | `team` |

- 재로그인 시 role/team/name 변경 감지 → DB 자동 동기화
- `users.password` 컬럼 nullable 처리 (V10 migration)

---

## 6. Keycloak 서버 정보 (개발환경)

| 항목 | 값 |
|------|----|
| Server URL | `http://192.168.10.30:8080` |
| Realm | `hubilon_pd` |
| Client ID | `hubilon-gitdigest` |
| Redirect URI | `http://192.168.10.30:3300/auth/callback` |
| Logout Redirect | `http://192.168.10.30:3300/auth/login` |
