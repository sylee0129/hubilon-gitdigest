-- reports 테이블의 (project_id, start_date, end_date) 중복 레코드 제거 후 UNIQUE constraint 추가

-- 1. 중복 레코드 중 최신(id 최대) 1건만 남기고 나머지 삭제
--    commit_infos / file_changes는 FK CASCADE가 없으므로 먼저 삭제
DELETE FROM file_changes
WHERE commit_info_id IN (
    SELECT ci.id FROM commit_infos ci
    WHERE ci.report_id IN (
        SELECT id FROM reports r
        WHERE r.id NOT IN (
            SELECT MAX(r2.id)
            FROM reports r2
            GROUP BY r2.project_id, r2.start_date, r2.end_date
        )
    )
);

DELETE FROM commit_infos
WHERE report_id IN (
    SELECT id FROM reports r
    WHERE r.id NOT IN (
        SELECT MAX(r2.id)
        FROM reports r2
        GROUP BY r2.project_id, r2.start_date, r2.end_date
    )
);

DELETE FROM reports
WHERE id NOT IN (
    SELECT MAX(r.id)
    FROM reports r
    GROUP BY r.project_id, r.start_date, r.end_date
);

-- 2. UNIQUE constraint 추가
ALTER TABLE reports
    ADD CONSTRAINT uk_reports_project_date UNIQUE (project_id, start_date, end_date);
