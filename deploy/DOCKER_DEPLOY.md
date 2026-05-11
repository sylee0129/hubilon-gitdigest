# Docker 배포 가이드

## 전제 조건

서버에 Docker 설치 확인:
```bash
docker --version
docker compose version
```

---

## 최초 설정 (1회)

### 1. 기존 프로세스 종료 (서버에서)
```bash
kill $(cat /home/hubilon-gitdigest/backend/gitdigest.pid)
```

### 2. Nginx 심볼릭 링크 제거 후 reload
```bash
rm /etc/nginx/sites-enabled/hubilon-gitdigest
nginx -t
systemctl reload nginx
```

### 3. docker-compose.yml, nginx.conf 업로드 (로컬에서)
```bash
scp deploy/docker-compose.yml deploy/nginx.conf root@192.168.10.30:/home/hubilon-gitdigest/
```

### 4. 컨테이너 시작 (서버에서)
```bash
cd /home/hubilon-gitdigest
docker compose up -d
docker compose ps
```

---

## 배포 (수동)

### 1. 로컬에서 빌드
```bash
# 백엔드
cd backend
./gradlew bootJar

# 프론트엔드
cd frontend
npm run build
```

### 2. 서버에 파일 업로드
```bash
# JAR 업로드
scp backend/build/libs/work-log-ai-*.jar root@192.168.10.30:/home/hubilon-gitdigest/backend/work-log-ai-0.0.1-SNAPSHOT.jar

# 프론트엔드 빌드 결과 업로드
scp -r frontend/dist root@192.168.10.30:/home/hubilon-gitdigest/frontend/
```

### 3. 컨테이너 재시작 (서버에서)
```bash
cd /home/hubilon-gitdigest
docker compose up -d
```

---

## 유용한 명령어 (서버에서)

```bash
# 상태 확인
docker compose ps

# 로그 확인
docker compose logs -f backend
docker compose logs -f frontend

# 재시작
docker compose restart backend
docker compose restart frontend

# 중지
docker compose down
```

---

## 서버 디렉토리 구조

```
/home/hubilon-gitdigest/
├── docker-compose.yml
├── nginx.conf
├── backend/
│   ├── work-log-ai-0.0.1-SNAPSHOT.jar
│   ├── .env
│   └── application-dev.yml
└── frontend/
    └── dist/
        ├── assets/
        └── index.html
```

---

## docker-compose.yml

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
