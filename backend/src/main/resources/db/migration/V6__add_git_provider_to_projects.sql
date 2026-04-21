ALTER TABLE projects ADD COLUMN git_provider VARCHAR(10) NOT NULL DEFAULT 'GITLAB';
UPDATE projects SET git_provider = 'GITLAB' WHERE git_provider IS NULL;
