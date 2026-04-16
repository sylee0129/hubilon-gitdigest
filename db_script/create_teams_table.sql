-- teams 테이블 생성
CREATE TABLE teams (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(255) NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_teams_name (name)
);

-- 기본 팀 데이터 삽입
INSERT INTO teams (name, created_at, updated_at)
VALUES ('플랫폼개발팀', NOW(), NOW());

-- 기존 users에 team_id 컬럼 추가 (없는 경우)
ALTER TABLE users ADD COLUMN team_id BIGINT;
ALTER TABLE users ADD CONSTRAINT fk_users_team FOREIGN KEY (team_id) REFERENCES teams (id);

-- 기존 users team_id 업데이트
UPDATE users SET team_id = (SELECT id FROM teams WHERE name = '플랫폼개발팀') WHERE team_id IS NULL;
