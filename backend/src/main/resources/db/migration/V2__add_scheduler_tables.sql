-- 초기 부서 데이터
INSERT INTO departments (name) VALUES ('플랫폼개발실'), ('서비스개발실'), ('솔루션연구소');

-- 초기 카테고리 데이터
INSERT INTO categories (name, sort_order) VALUES ('개발사업', 0), ('신규추진사업', 1), ('기타', 2);

-- 초기 팀 데이터
INSERT INTO teams (name, dept_id) VALUES
    ('플랫폼개발팀',  (SELECT id FROM departments WHERE name = '플랫폼개발실')),
    ('서비스개발1팀', (SELECT id FROM departments WHERE name = '서비스개발실')),
    ('서비스개발2팀', (SELECT id FROM departments WHERE name = '서비스개발실')),
    ('서비스개발3팀', (SELECT id FROM departments WHERE name = '서비스개발실')),
    ('솔루션개발1팀', (SELECT id FROM departments WHERE name = '솔루션연구소')),
    ('솔루션개발2팀', (SELECT id FROM departments WHERE name = '솔루션연구소'));
