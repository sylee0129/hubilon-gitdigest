-- ShedLock: 분산 스케줄러 락 테이블
CREATE TABLE shedlock (
    name       VARCHAR(64)  NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by  VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- 스케줄러 잡 실행 로그
CREATE TABLE scheduler_job_logs (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    executed_at        TIMESTAMP    NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    total_folder_count INT          NOT NULL DEFAULT 0,
    success_count      INT          NOT NULL DEFAULT 0,
    fail_count         INT          NOT NULL DEFAULT 0,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- 스케줄러 폴더별 처리 결과
CREATE TABLE scheduler_folder_results (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    job_log_id          BIGINT        NOT NULL,
    folder_id           BIGINT        NOT NULL,
    folder_name         VARCHAR(255)  NOT NULL,
    success             BOOLEAN       NOT NULL,
    error_message       LONGTEXT,
    confluence_page_url VARCHAR(500),
    PRIMARY KEY (id),
    CONSTRAINT fk_sfr_job_log FOREIGN KEY (job_log_id)
        REFERENCES scheduler_job_logs (id) ON DELETE CASCADE
);
