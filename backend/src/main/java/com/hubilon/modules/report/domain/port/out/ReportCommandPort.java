package com.hubilon.modules.report.domain.port.out;

import com.hubilon.modules.report.domain.model.Report;

public interface ReportCommandPort {

    Report save(Report report);

    void deleteByProjectId(Long projectId);
}
