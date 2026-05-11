#!/bin/bash
set -e

SERVER=root@192.168.10.30
DEPLOY_DIR=/home/hubilon-gitdigest
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "[1/4] 백엔드 빌드..."
cd "$ROOT_DIR/backend"
./gradlew bootJar

echo "[2/4] 프론트엔드 빌드..."
cd "$ROOT_DIR/frontend"
npm run build

echo "[3/4] 서버 업로드..."
cd "$ROOT_DIR"
scp backend/build/libs/work-log-ai-*.jar $SERVER:$DEPLOY_DIR/backend/app.jar
scp -r frontend/dist/. $SERVER:$DEPLOY_DIR/frontend/
scp deploy/docker-compose.yml deploy/nginx.conf $SERVER:$DEPLOY_DIR/

echo "[4/4] 컨테이너 재시작..."
ssh $SERVER "cd $DEPLOY_DIR && docker compose up -d && docker compose ps"

echo "완료!"
