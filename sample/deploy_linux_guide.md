# Work-Log-AI Linux 서버 배포 가이드

> **스택**: Java 17 + Spring Boot 4.x (Backend) / React 19 + Vite 6 (Frontend)  
> **DB**: H2 파일 기반 (프로덕션 전환 시 MySQL/PostgreSQL 권장)

---

## 1. 사전 요구사항

서버에 아래 패키지를 설치한다.

```bash
# Ubuntu/Debian 기준

# Java 17
sudo apt update
sudo apt install -y openjdk-17-jdk

# Node.js LTS (nvm 사용 권장)
curl -fsSL https://fnm.vercel.app/install | bash
source ~/.bashrc
fnm install --lts
fnm use lts-latest

# Nginx
sudo apt install -y nginx

# Git
sudo apt install -y git

# 버전 확인
java -version    # openjdk 17.x.x
node -v          # v22.x.x
npm -v
nginx -v
```

---

## 2. 프로젝트 클론

```bash
# 배포 디렉토리 생성
sudo mkdir -p /opt/work-log-ai
sudo chown $USER:$USER /opt/work-log-ai

cd /opt/work-log-ai

# Git 클론
git clone <YOUR_REPO_URL> .
```

---

## 3. 환경변수 설정

### 3-1. Backend 환경변수

```bash
cd /opt/work-log-ai/backend

# .env 파일 생성
cat > .env << 'EOF'
# JWT (필수 — 최소 32자 이상 강력한 키로 변경)
JWT_SECRET=your-production-secret-key-must-be-32chars-or-more!!

# GitLab OAuth (GitLab 로그인 사용 시)
GITLAB_OAUTH_CLIENT_ID=your-gitlab-client-id
GITLAB_OAUTH_CLIENT_SECRET=your-gitlab-client-secret
GITLAB_OAUTH_REDIRECT_URI=http://your-domain.com/api/oauth/gitlab/callback
FRONTEND_ORIGIN=http://your-domain.com

# AI 요약 기능 (Google Gemini)
GEMINI_API_KEY=your-gemini-api-key

# Confluence 연동 (사용 시)
CONFLUENCE_BASE_URL=https://your-org.atlassian.net
CONFLUENCE_USER_EMAIL=your-email@example.com
CONFLUENCE_API_TOKEN=your-confluence-api-token
CONFLUENCE_SPACE_KEY=YOUR_SPACE_KEY
CONFLUENCE_PARENT_PAGE_TITLE=주간보고
CONFLUENCE_PARENT_PAGE_ID=000000
EOF

# 파일 권한 제한 (소유자만 읽기)
chmod 600 .env
```

### 3-2. Frontend 환경변수

```bash
cd /opt/work-log-ai/frontend

cat > .env.production << 'EOF'
VITE_API_URL=http://your-domain.com/api
EOF
```

---

## 4. Backend 빌드 및 실행

### 4-1. 빌드

```bash
cd /opt/work-log-ai/backend

# gradlew 실행 권한 부여
chmod +x gradlew

# 빌드 (테스트 제외)
./gradlew build -x test

# 빌드 결과물 확인
ls build/libs/*.jar
```

### 4-2. 수동 실행 (테스트용)

```bash
# .env 파일을 소스로 불러와 실행
set -a && source .env && set +a

java -jar build/libs/work-log-ai-*.jar \
  --spring.profiles.active=prod
```

### 4-3. systemd 서비스 등록 (운영용)

```bash
sudo tee /etc/systemd/system/work-log-ai.service << 'EOF'
[Unit]
Description=Work Log AI Backend
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/work-log-ai/backend
EnvironmentFile=/opt/work-log-ai/backend/.env
ExecStart=/usr/bin/java -jar /opt/work-log-ai/backend/build/libs/work-log-ai-*.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=10
StandardOutput=append:/var/log/work-log-ai/backend.log
StandardError=append:/var/log/work-log-ai/backend-error.log

[Install]
WantedBy=multi-user.target
EOF

# 로그 디렉토리 생성
sudo mkdir -p /var/log/work-log-ai
sudo chown ubuntu:ubuntu /var/log/work-log-ai

# 서비스 등록 및 시작
sudo systemctl daemon-reload
sudo systemctl enable work-log-ai
sudo systemctl start work-log-ai

# 상태 확인
sudo systemctl status work-log-ai
```

> **주의**: `User=ubuntu` 부분을 실제 서버 사용자명으로 변경한다.

---

## 5. Frontend 빌드

```bash
cd /opt/work-log-ai/frontend

# 의존성 설치
npm install

# 프로덕션 빌드
npm run build

# 빌드 결과물 확인
ls dist/
```

빌드 완료 후 `dist/` 폴더에 정적 파일(HTML, JS, CSS)이 생성된다.

---

## 6. Nginx 설정

```bash
sudo tee /etc/nginx/sites-available/work-log-ai << 'EOF'
server {
    listen 80;
    server_name your-domain.com;  # 실제 도메인 또는 서버 IP로 변경

    # Frontend 정적 파일 서빙
    root /opt/work-log-ai/frontend/dist;
    index index.html;

    # React Router 지원 (새로고침 시 404 방지)
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Backend API 프록시
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # H2 Console (개발 시만, 프로덕션에서는 제거)
    # location /h2-console/ {
    #     proxy_pass http://localhost:8080/h2-console/;
    # }
}
EOF

# 설정 활성화
sudo ln -s /etc/nginx/sites-available/work-log-ai /etc/nginx/sites-enabled/

# 기본 설정 비활성화
sudo rm -f /etc/nginx/sites-enabled/default

# 설정 검증
sudo nginx -t

# Nginx 재시작
sudo systemctl restart nginx
sudo systemctl enable nginx
```

---

## 7. 방화벽 설정

```bash
# ufw 활성화
sudo ufw enable

# SSH 허용 (먼저 반드시 허용)
sudo ufw allow ssh

# HTTP/HTTPS 허용
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# Backend 직접 접근은 Nginx를 통하므로 외부 노출 불필요
# (내부 통신만 사용: localhost:8080)

# 상태 확인
sudo ufw status
```

---

## 8. 데이터베이스 설정

현재 H2 파일 기반 DB를 사용 중이다. 개발/소규모 배포에서는 그대로 사용 가능하나, 프로덕션 환경에서는 MySQL 또는 PostgreSQL로 전환을 권장한다.

### H2 그대로 사용 시

```bash
# DB 파일 디렉토리 생성 및 권한 설정
mkdir -p /opt/work-log-ai/backend/data
```

`application.yml`의 `spring.datasource.url`에서 `./data/worklogai`는 `WorkingDirectory` 기준 상대경로이므로, systemd 서비스에서 `WorkingDirectory=/opt/work-log-ai/backend`로 지정하면 자동으로 `/opt/work-log-ai/backend/data/`에 DB 파일이 생성된다.

### MySQL로 전환 시

```bash
# MySQL 설치
sudo apt install -y mysql-server
sudo mysql_secure_installation

# DB 및 사용자 생성
sudo mysql -u root -p << 'SQL'
CREATE DATABASE worklogai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'worklogai'@'localhost' IDENTIFIED BY 'strong-password';
GRANT ALL PRIVILEGES ON worklogai.* TO 'worklogai'@'localhost';
FLUSH PRIVILEGES;
SQL
```

`backend/src/main/resources/application-prod.yml` 생성:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/worklogai?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: worklogai
    password: ${DB_PASSWORD}
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: update
    show-sql: false
  h2:
    console:
      enabled: false
```

`build.gradle.kts`에 MySQL 의존성 추가:

```kotlin
runtimeOnly("com.mysql:mysql-connector-j")
```

---

## 9. HTTPS 설정 (Let's Encrypt)

도메인이 있는 경우 Let's Encrypt로 무료 SSL 인증서를 발급한다.

```bash
# Certbot 설치
sudo apt install -y certbot python3-certbot-nginx

# 인증서 발급 및 Nginx 자동 설정
sudo certbot --nginx -d your-domain.com

# 자동 갱신 확인
sudo certbot renew --dry-run
```

---

## 10. 로그 확인 및 트러블슈팅

```bash
# Backend 로그
tail -f /var/log/work-log-ai/backend.log
tail -f /var/log/work-log-ai/backend-error.log

# systemd 로그
sudo journalctl -u work-log-ai -f

# Nginx 로그
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log

# 서비스 재시작
sudo systemctl restart work-log-ai
sudo systemctl restart nginx

# Backend 포트 확인
ss -tlnp | grep 8080
```

### 자주 발생하는 문제

| 증상 | 원인 | 해결 |
|------|------|------|
| 502 Bad Gateway | Backend가 아직 기동 중 | `ss -tlnp \| grep 8080` 확인 후 대기 |
| CORS 오류 | `FRONTEND_ORIGIN` 환경변수 불일치 | `.env`의 `FRONTEND_ORIGIN`을 실제 도메인으로 변경 |
| JWT 오류 | `JWT_SECRET` 미설정 또는 짧은 키 | 32자 이상 강력한 키로 변경 |
| 빌드 실패 | Java 버전 불일치 | `java -version`으로 17 확인 |
| DB 파일 권한 오류 | data/ 디렉토리 권한 문제 | `chown -R ubuntu:ubuntu /opt/work-log-ai/backend/data` |

---

## 11. 배포 업데이트 절차

코드 변경 후 재배포 시:

```bash
cd /opt/work-log-ai

# 최신 코드 pull
git pull origin master

# Backend 재빌드
cd backend
./gradlew build -x test

# Frontend 재빌드
cd ../frontend
npm install
npm run build

# Backend 서비스 재시작
sudo systemctl restart work-log-ai

# Nginx는 정적 파일만 서빙하므로 재시작 불필요
# (설정 변경 시에는 sudo systemctl reload nginx)
```

---

## 보안 체크리스트

- [ ] `JWT_SECRET` — 운영용 강력한 키로 변경 (32자 이상)
- [ ] `.env` 파일 권한 `chmod 600` 적용
- [ ] `.env` 파일 Git에 커밋되지 않도록 `.gitignore` 확인
- [ ] H2 콘솔 비활성화 (운영 환경에서는 `spring.h2.console.enabled: false`)
- [ ] Swagger UI 비활성화 (운영 환경에서 필요 시)
- [ ] HTTPS 적용 (Let's Encrypt)
- [ ] 방화벽에서 불필요한 포트 차단
- [ ] 외부에서 Backend 포트(8080) 직접 접근 차단 (Nginx 프록시만 허용)
