CREATE TABLE categories (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

INSERT INTO categories (name, sort_order) VALUES ('개발사업', 0), ('신규추진사업', 1), ('기타', 2);

ALTER TABLE folders ADD COLUMN category_id BIGINT;

-- category 컬럼이 존재하는 경우에만 데이터 마이그레이션 & 컬럼 삭제 수행
DROP PROCEDURE IF EXISTS migrate_folder_category;

DELIMITER $$
CREATE PROCEDURE migrate_folder_category()
BEGIN
    DECLARE col_exists INT DEFAULT 0;

    SELECT COUNT(*)
    INTO col_exists
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME   = 'folders'
      AND COLUMN_NAME  = 'category';

    IF col_exists > 0 THEN
        UPDATE folders SET category_id = (SELECT id FROM categories WHERE name = '개발사업')    WHERE category = 'DEVELOPMENT';
        UPDATE folders SET category_id = (SELECT id FROM categories WHERE name = '신규추진사업') WHERE category = 'NEW_BUSINESS';
        UPDATE folders SET category_id = (SELECT id FROM categories WHERE name = '기타')        WHERE category = 'OTHER';
        ALTER TABLE folders DROP COLUMN category;
    END IF;
END$$
DELIMITER ;

CALL migrate_folder_category();
DROP PROCEDURE IF EXISTS migrate_folder_category;

-- category_id 가 NULL 인 행(초기 구동 등)에 기본값 세팅
UPDATE folders SET category_id = (SELECT id FROM categories WHERE name = '개발사업' LIMIT 1) WHERE category_id IS NULL;

ALTER TABLE folders MODIFY COLUMN category_id BIGINT NOT NULL;
ALTER TABLE folders ADD CONSTRAINT fk_folders_category FOREIGN KEY (category_id) REFERENCES categories(id);
