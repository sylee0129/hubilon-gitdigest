CREATE TABLE departments (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

INSERT INTO departments (name) VALUES ('플랫폼개발실'), ('서비스개발실'), ('솔루션연구소');

ALTER TABLE teams ADD COLUMN dept_id BIGINT;

UPDATE teams SET dept_id = (SELECT id FROM departments WHERE name = '플랫폼개발실') WHERE name = '플랫폼개발팀';
UPDATE teams SET dept_id = (SELECT id FROM departments WHERE name = '서비스개발실') WHERE name IN ('서비스개발1팀','서비스개발2팀','서비스개발3팀');
UPDATE teams SET dept_id = (SELECT id FROM departments WHERE name = '솔루션연구소') WHERE name IN ('솔루션개발1팀','솔루션개발2팀');

-- 매핑되지 않은 팀에 기본값 설정 (폴백)
UPDATE teams SET dept_id = (SELECT id FROM departments WHERE name = '플랫폼개발실' LIMIT 1) WHERE dept_id IS NULL;

ALTER TABLE teams MODIFY COLUMN dept_id BIGINT NOT NULL;
ALTER TABLE teams ADD CONSTRAINT fk_teams_dept FOREIGN KEY (dept_id) REFERENCES departments(id);
