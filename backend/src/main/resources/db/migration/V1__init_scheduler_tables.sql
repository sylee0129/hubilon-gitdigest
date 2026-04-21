-- =============================================
-- 전체 스키마 초기화 (PostgreSQL)
-- =============================================

-- 1. departments
CREATE TABLE departments (
    id         BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (id)
);

-- 2. categories
CREATE TABLE categories (
    id         BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name       VARCHAR(100) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (id)
);

-- 3. teams (→ departments)
CREATE TABLE teams (
    id         BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name       VARCHAR(255) NOT NULL UNIQUE,
    dept_id    BIGINT       NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_teams_dept FOREIGN KEY (dept_id) REFERENCES departments (id)
);

-- 4. users (→ teams nullable)
CREATE TABLE users (
    id         BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    role       VARCHAR(50)  NOT NULL,
    team_id    BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_users_team FOREIGN KEY (team_id) REFERENCES teams (id)
);

-- 5. refresh_tokens
CREATE TABLE refresh_tokens (
    id         BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    user_id    BIGINT       NOT NULL,
    token      VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP,
    PRIMARY KEY (id)
);

-- 6. folders (→ categories, → teams nullable)
CREATE TABLE folders (
    id          BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name        VARCHAR(255) NOT NULL,
    category_id BIGINT       NOT NULL,
    status      VARCHAR(50)  NOT NULL,
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    team_id     BIGINT,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_folders_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_folders_team     FOREIGN KEY (team_id)     REFERENCES teams (id)
);

-- 7. folder_members (composite PK)
CREATE TABLE folder_members (
    folder_id BIGINT NOT NULL,
    user_id   BIGINT NOT NULL,
    PRIMARY KEY (folder_id, user_id),
    CONSTRAINT fk_fm_folder FOREIGN KEY (folder_id) REFERENCES folders (id)
);

-- 8. work_projects (→ folders)
CREATE TABLE work_projects (
    id         BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    folder_id  BIGINT       NOT NULL,
    name       VARCHAR(255) NOT NULL,
    sort_order INTEGER      NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_wp_folder FOREIGN KEY (folder_id) REFERENCES folders (id)
);

-- 9. projects
CREATE TABLE projects (
    id                BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    name              VARCHAR(255) NOT NULL,
    gitlab_url        VARCHAR(255) NOT NULL,
    access_token      VARCHAR(255) NOT NULL,
    auth_type         VARCHAR(50)  NOT NULL,
    gitlab_project_id BIGINT,
    sort_order        INTEGER      NOT NULL DEFAULT 0,
    folder_id         BIGINT,
    team_id           BIGINT,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    PRIMARY KEY (id)
);

-- 10. reports
CREATE TABLE reports (
    id              BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    project_id      BIGINT       NOT NULL,
    project_name    VARCHAR(255) NOT NULL,
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    ai_summary      TEXT,
    manual_summary  TEXT,
    manually_edited BOOLEAN      NOT NULL,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    PRIMARY KEY (id)
);

-- 11. commit_infos (→ reports)
CREATE TABLE commit_infos (
    id           BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    report_id    BIGINT       NOT NULL,
    sha          VARCHAR(255) NOT NULL,
    author_name  VARCHAR(255),
    author_email VARCHAR(255),
    committed_at TIMESTAMP,
    message      TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_ci_report FOREIGN KEY (report_id) REFERENCES reports (id)
);

-- 12. file_changes (→ commit_infos)
CREATE TABLE file_changes (
    id             BIGINT  NOT NULL GENERATED ALWAYS AS IDENTITY,
    commit_info_id BIGINT  NOT NULL,
    old_path       VARCHAR(255),
    new_path       VARCHAR(255),
    new_file       BOOLEAN NOT NULL DEFAULT FALSE,
    renamed_file   BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_file   BOOLEAN NOT NULL DEFAULT FALSE,
    added_lines    INTEGER NOT NULL DEFAULT 0,
    removed_lines  INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_fc_commit FOREIGN KEY (commit_info_id) REFERENCES commit_infos (id)
);

-- 13. folder_summary
CREATE TABLE folder_summary (
    id                       BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    folder_id                BIGINT       NOT NULL,
    folder_name              VARCHAR(255) NOT NULL,
    start_date               DATE         NOT NULL,
    end_date                 DATE         NOT NULL,
    total_commit_count       INTEGER      NOT NULL,
    unique_contributor_count INTEGER      NOT NULL,
    summary                  TEXT,
    manually_edited          BOOLEAN      NOT NULL,
    ai_summary_failed        BOOLEAN      NOT NULL,
    progress_summary         TEXT,
    plan_summary             TEXT,
    created_at               TIMESTAMP,
    updated_at               TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_folder_summary_folder_date UNIQUE (folder_id, start_date, end_date)
);

-- 14. scheduler_job_logs
CREATE TABLE scheduler_job_logs (
    id                 BIGINT      NOT NULL GENERATED ALWAYS AS IDENTITY,
    executed_at        TIMESTAMP   NOT NULL,
    status             VARCHAR(20) NOT NULL,
    total_folder_count INTEGER     NOT NULL DEFAULT 0,
    success_count      INTEGER     NOT NULL DEFAULT 0,
    fail_count         INTEGER     NOT NULL DEFAULT 0,
    created_at         TIMESTAMP,
    PRIMARY KEY (id)
);

-- 15. scheduler_folder_results (→ scheduler_job_logs)
CREATE TABLE scheduler_folder_results (
    id                  BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    job_log_id          BIGINT       NOT NULL,
    folder_id           BIGINT       NOT NULL,
    folder_name         VARCHAR(255) NOT NULL,
    success             BOOLEAN      NOT NULL,
    error_message       TEXT,
    confluence_page_url VARCHAR(500),
    PRIMARY KEY (id),
    CONSTRAINT fk_sfr_job_log FOREIGN KEY (job_log_id) REFERENCES scheduler_job_logs (id) ON DELETE CASCADE
);

-- 16. shedlock
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- 17. confluence_space_configs
CREATE TABLE confluence_space_configs (
    id         BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY,
    dept_id    BIGINT        NOT NULL UNIQUE,
    user_email VARCHAR(255)  NOT NULL,
    api_token  VARCHAR(1000) NOT NULL,
    space_key  VARCHAR(100)  NOT NULL,
    base_url   VARCHAR(500)  NOT NULL,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    PRIMARY KEY (id)
);

-- 18. confluence_team_configs
CREATE TABLE confluence_team_configs (
    id             BIGINT       NOT NULL GENERATED ALWAYS AS IDENTITY,
    team_id        BIGINT       NOT NULL UNIQUE,
    parent_page_id VARCHAR(100) NOT NULL,
    created_by     VARCHAR(100),
    updated_by     VARCHAR(100),
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    PRIMARY KEY (id)
);
