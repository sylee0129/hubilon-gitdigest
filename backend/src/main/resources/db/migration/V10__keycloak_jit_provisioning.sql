-- JIT Provisioning: password nullable, keycloak_username 컬럼 추가
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS keycloak_username VARCHAR(255);
