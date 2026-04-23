ALTER TABLE scheduler_job_logs
    ADD COLUMN IF NOT EXISTS team_id   BIGINT,
    ADD COLUMN IF NOT EXISTS team_name VARCHAR(100);

CREATE TABLE IF NOT EXISTS scheduler_team_configs (
    id         BIGSERIAL PRIMARY KEY,
    team_id    BIGINT      NOT NULL UNIQUE,
    team_name  VARCHAR(100) NOT NULL,
    enabled    BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_scheduler_team_configs_team
        FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE
);
